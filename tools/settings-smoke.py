"""Isolated HTTP/SQLite settings test. Pass path to PHP and its extension directory."""
import base64, hashlib, hmac, os, pathlib, socket, subprocess, sys, tempfile, time
import requests

php, extensions = sys.argv[1:3]
root = pathlib.Path(__file__).resolve().parent.parent
with tempfile.TemporaryDirectory(prefix='lchat-settings-') as directory:
    temp = pathlib.Path(directory)
    (temp/'config.php').write_text("<?php return ['database'=>['dsn'=>'sqlite:" + (temp/'test.sqlite').as_posix() + "'], 'install_key'=>'" + 'x'*40 + "', 'site_name'=>'From config', 'initial_admin_username'=>'customowner', 'registration_open'=>true, 'max_call_participants'=>8, 'turn'=>['urls'=>'','secret'=>'','ttl'=>3600]];", encoding='utf-8')
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0)); port = sock.getsockname()[1]
    env = dict(os.environ, LCHAT_CONFIG=str(temp/'config.php'))
    server = subprocess.Popen([php, '-d', 'extension_dir='+extensions, '-d', 'extension=mbstring', '-d', 'extension=pdo_sqlite', '-S', '127.0.0.1:'+str(port), '-t', str(root)], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    base = 'http://127.0.0.1:'+str(port)+'/'
    try:
        for _ in range(50):
            try:
                if requests.get(base, timeout=1).status_code == 200: break
            except requests.RequestException: time.sleep(.1)
        else: raise RuntimeError('PHP server did not start')
        assert 'From config' in requests.get(base).text
        assert 'Таблицы созданы' in requests.post(base+'install.php', data={'key':'x'*40}).text
        owner = requests.Session(); stranger = requests.Session()
        def register(session, username):
            csrf = session.get(base+'api.php?action=me').json()['csrf']
            r = session.post(base+'api.php?action=register', headers={'X-CSRF-Token':csrf}, json={'name':'Test User', 'username':username, 'email':username+'@example.test', 'password':'strong-password-123'})
            assert r.ok, r.text
        register(owner, 'customowner'); register(stranger, 'outsider')
        me = owner.get(base+'admin.php?action=me').json(); csrf = me['csrf']
        assert stranger.get(base+'admin.php?action=site.settings').status_code == 403
        assert owner.post(base+'admin.php?action=site.save', json={}).status_code == 403
        values = {'site_name':'New <name>', 'registration_open':False, 'max_call_participants':5, 'call_transport':'relay', 'turn_urls':'turn:turn.example.test:3478?transport=udp', 'turn_ttl':3600, 'turn_secret':'temporary-secret-for-test'}
        r = owner.post(base+'admin.php?action=site.save', headers={'X-CSRF-Token':csrf}, json=values); assert r.ok, r.text
        settings = owner.get(base+'admin.php?action=site.settings').json()
        assert settings['site_name'] == values['site_name'] and settings['turn_secret_set'] and 'turn_secret' not in settings
        assert 'temporary-secret-for-test' not in str(settings)
        assert 'New &lt;name&gt;' in owner.get(base).text
        assert owner.get(base+'api.php?action=me').json()['registration_open'] is False
        create = owner.post(base+'api.php?action=room.create', headers={'X-CSRF-Token':csrf}, json={'title':'Test call'}); assert create.ok, create.text
        token = create.json()['token']
        assert owner.get(base+'api.php?action=room.info', params={'token':token}).json()['limit'] == 5
        join = owner.post(base+'api.php?action=room.join', headers={'X-CSRF-Token':csrf}, json={'token':token}); assert join.ok, join.text
        call = join.json(); assert call['ice_transport_policy'] == 'relay'
        turn = next(server for server in call['ice_servers'] if str(server['urls']).startswith('turn:'))
        assert turn['credential'] == base64.b64encode(hmac.new(values['turn_secret'].encode(), turn['username'].encode(), hashlib.sha1).digest()).decode()
        assert values['turn_secret'] not in join.text
        values['max_call_participants'] = 999
        assert owner.post(base+'admin.php?action=site.save', headers={'X-CSRF-Token':csrf}, json=values).status_code == 409
        assert owner.get(base+'admin.php?action=site.settings').json()['max_call_participants'] == 5
        assert owner.post(base+'admin.php?action=site.reset', headers={'X-CSRF-Token':csrf}, json={}).ok
        restored = owner.get(base+'admin.php?action=site.settings').json()
        assert restored['site_name'] == 'From config' and restored['registration_open'] and not restored['turn_secret_set']
        print('PASS: config fallback, configured owner, CSRF, save, secret redaction, HTML escaping, registration, call limit and TURN credentials, validation and reset')
    finally:
        server.terminate(); server.wait(timeout=10)
