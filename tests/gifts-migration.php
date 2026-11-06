<?php
declare(strict_types=1);
define('LCHAT_INTERNAL', true);
if (!in_array('sqlite', PDO::getAvailableDrivers(), true)) {
    fwrite(STDERR, "pdo_sqlite is required\n");
    exit(1);
}
function db(): PDO {
    static $db;
    if (!$db) {
        $db = new PDO('sqlite::memory:');
        $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $db->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        $db->exec("CREATE TABLE settings (name TEXT PRIMARY KEY, value TEXT); INSERT INTO settings VALUES ('schema_version', '17')");
    }
    return $db;
}
function query(string $sql, array $params = []): PDOStatement {
    $stmt = db()->prepare($sql);
    $stmt->execute($params);
    return $stmt;
}
function one(string $sql, array $params = []): ?array { return query($sql, $params)->fetch() ?: null; }
function bot_creator_install(): void {}
require __DIR__ . '/../apis/features.php';
require __DIR__ . '/../apis/rewards.php';
ensure_features();
if ((int)one('SELECT value FROM settings WHERE name = ?', ['schema_version'])['value'] !== 21) {
    throw new RuntimeException('Migration did not update schema version');
}
foreach (['lchat_wallets', 'lchat_gift_orders', 'lchat_ticket_grants', 'lchat_gift_recipients'] as $table) {
    if (!one("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", [$table])) {
        throw new RuntimeException('Missing table: ' . $table);
    }
}
rewards_install();
echo "PASS migration from version 17 and repeat installation\n";
