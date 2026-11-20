'use strict';
const ORIGIN = 'https://chat.lbs.su';
function trusted(value) {
  try { const u = new URL(value); return u.origin === ORIGIN && !u.username && !u.password; } catch { return false; }
}
function external(value) {
  try { const u = new URL(value); return ['https:', 'http:', 'mailto:'].includes(u.protocol) && !u.username && !u.password; } catch { return false; }
}
function permissionAllowed(value, permission, details = {}) {
  if (!trusted(value)) return false;
  if (permission === 'notifications' || permission === 'fullscreen') return true;
  return permission === 'media' && Array.isArray(details.mediaTypes) && details.mediaTypes.length > 0 && details.mediaTypes.every(x => ['audio', 'video'].includes(x));
}
module.exports = { ORIGIN, trusted, external, permissionAllowed };
