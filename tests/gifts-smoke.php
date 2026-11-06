<?php
declare(strict_types=1);
define('LCHAT_INTERNAL',true);
if (!in_array('sqlite', PDO::getAvailableDrivers(), true)) { fwrite(STDERR,"pdo_sqlite is required\n"); exit(1); }
function db(): PDO { static $db;if(!$db){$db=new PDO('sqlite::memory:');$db->setAttribute(PDO::ATTR_ERRMODE,PDO::ERRMODE_EXCEPTION);$db->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE,PDO::FETCH_ASSOC);$db->exec("CREATE TABLE users (id INTEGER PRIMARY KEY,username TEXT,name TEXT); INSERT INTO users VALUES (1,'alice','Alice'),(2,'bob','Bob')");}return $db; }
function query(string $sql,array $p=[]): PDOStatement {$s=db()->prepare($sql);$s->execute($p);return $s;}
function one(string $sql,array $p=[]): ?array {return query($sql,$p)->fetch()?:null;}
function transaction(callable $fn): mixed {db()->beginTransaction();try{$v=$fn();db()->commit();return $v;}catch(Throwable $e){db()->rollBack();throw $e;}}
function stamp(): string {return gmdate('Y-m-d H:i:s');}
function check(bool $ok,string $message): void {if(!$ok)throw new RuntimeException($message);}
require __DIR__.'/../apis/rewards.php';
rewards_install();$catalog=rewards_catalog();check(count($catalog)===170,'catalog count');
check($catalog['heart'][2]===20 && $catalog['star'][2]===10,'existing prices changed');
foreach($catalog as $id=>$item){check(strlen($id)<=24 && preg_match('/^[a-z0-9_]+$/D',$id)===1,'invalid id');check(is_int($item[2])&&$item[2]>0,'invalid price');}
$alice=one('SELECT * FROM users WHERE id=?',[1]);query('INSERT INTO lchat_wallets (user_id,tickets,verified) VALUES (1,100,0)');
$first=str_repeat('a',48);$second=str_repeat('b',48);
rewards_purchase($alice,'heart','bob',$first);rewards_purchase($alice,'heart','bob',$first);
check((int)rewards_wallet(1)['tickets']===80,'replay debited twice');
rewards_purchase($alice,'heart','bob',$second);check((int)rewards_wallet(1)['tickets']===60,'second debit');
check((int)one('SELECT COUNT(*) AS n FROM lchat_gift_orders')['n']===2,'unique instances');
check((int)one('SELECT COUNT(*) AS n FROM lchat_gift_recipients WHERE recipient_id=?',[2])['n']===2,'recipient');
try{rewards_purchase($alice,'rocket','bob',str_repeat('c',48));throw new RuntimeException('insufficient funds accepted');}catch(DomainException $e){}
try{rewards_purchase($alice,'heart','missing',str_repeat('d',48));throw new RuntimeException('unknown recipient accepted');}catch(DomainException $e){}
check((int)rewards_wallet(1)['tickets']===60,'balance changed on rejection');
echo "PASS catalog, prices, unique gifts, recipient, replay and insufficient funds\n";
