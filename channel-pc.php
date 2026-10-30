<?php
if (!defined('LCHAT_INTERNAL')) { http_response_code(404); exit; }
define('LCHAT_LOCAL_PAGE', true);
$slug = (string)($_GET['slug'] ?? '');
if (!preg_match('/^[\p{L}][\p{L}\p{N}_]{2,31}$/uD', $slug)) { http_response_code(404); exit('Канал не найден.'); }
$path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
if (str_ends_with($path, '/')) { header('Location: ' . rtrim($path, '/'), true, 308); exit; }
ob_start(); require __DIR__ . '/index.php'; $html = ob_get_clean();
echo str_replace('<head>', '<head><script>window.LCHAT_CHANNEL=' . json_encode($slug, JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT) . ';</script>', $html);
