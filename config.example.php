<?php
declare(strict_types=1);
if (!defined('LCHAT_INTERNAL')) { http_response_code(404); exit; }

return [
    // Сервер запросов: host — хостинг; pc — HTTPS-сервер на вашем ПК.
    'backend' => ['mode' => 'host', 'url' => '', 'proxy_secret' => ''],
    'database' => [
        'dsn' => 'mysql:host=localhost;port=3306;dbname=YOUR_DATABASE;charset=utf8mb4',
        'username' => 'YOUR_DATABASE_USER',
        'password' => 'YOUR_DATABASE_PASSWORD',
    ],
    'install_key' => 'REPLACE_WITH_A_LONG_RANDOM_SECRET',
    // Shared secret for authenticated gateway requests to this backend.
    'proxy_secret' => '',
    // Название в веб-интерфейсе; по умолчанию LChat.
    'site_name' => 'LChat',
    // Логин пользователя, который станет владельцем при новой установке.
    'initial_admin_username' => 'leosmleol',
    'registration_open' => true,
    'session_name' => 'lchat_session',
    'session_domain' => '',
    // WebRTC соединяет участников друг с другом; используйте небольшой лимит.
    'max_call_participants' => 8,
    'ice_servers' => [
        ['urls' => 'stun:stun.l.google.com:19302'],
    ],
    'turn' => [
        'urls' => 'turn:209.25.141.16:45209?transport=tcp',
        // Set this privately in config.php or the LCHAT_TURN_SECRET environment variable.
        'secret' => getenv('LCHAT_TURN_SECRET') ?: '',
        'ttl' => 3600,
    ],
    'call_transport' => 'all',
    'public_url' => 'https://your-domain.example',
];
