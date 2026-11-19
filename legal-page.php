<?php
declare(strict_types=1);
if (!defined('LCHAT_LEGAL_PAGE')) { http_response_code(404); exit; }
$legal = require __DIR__ . '/legal.php';
$operator = trim((string)($legal['operator'] ?? ''));
$contact = trim((string)($legal['contact'] ?? ''));
function legal_escape(string $s): string { return htmlspecialchars($s, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8'); }
header('X-Content-Type-Options: nosniff');
?><!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title><?= legal_escape($title) ?> · LChat</title></head><body><main class="page"><nav><a href="index.php">← LChat</a><a href="privacy.php">Конфиденциальность</a><a href="terms.php">Условия</a><a href="rules.php">Правила и возврат</a><a href="premium.php">Premium</a></nav><article><p class="eyebrow">LCHAT</p><h1><?= legal_escape($title) ?></h1><?php if (!$operator || !$contact): ?><p class="notice">Документ подготовлен для ознакомления. Данные оператора и контакт для обращений уточняются; до их заполнения документ не является окончательной редакцией.</p><?php endif; ?><?= $content ?><p class="details">Оператор: <?= $operator ? legal_escape($operator) : 'сведения уточняются' ?>. Контакт: <?= $contact ? legal_escape($contact) : 'уточняется' ?>.</p><?= $legalFooter ?? '' ?></article></main></body></html>
