'use strict';
// Znajomi z Void Client i ikonka Void przy nicku – serwer Supabase.
// Klucz publishable jest jawny z założenia (taki sam siedzi w modzie); dostęp do danych pilnują funkcje
// w bazie, które sprawdzają token Minecraft u Mojanga (backend/schema.sql).

const URL = 'https://ogapyiwjrplfdztjmbbq.supabase.co/rest/v1/rpc/';
const KEY = 'sb_publishable_auXkcWYA2zmqOGEOw7APrw_LJApnFl6';

let token = null;
let tokenAt = 0;

async function tok() {
  if (!token || Date.now() - tokenAt > 60 * 60 * 1000) {
    token = await require('./auth').getAccessToken();
    tokenAt = Date.now();
  }
  return token;
}

async function rpc(fn, body) {
  const r = await fetch(URL + fn, {
    method: 'POST',
    headers: { apikey: KEY, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(15000)
  });
  const text = await r.text();
  if (!r.ok) {
    let msg = '';
    try { msg = JSON.parse(text).message || ''; } catch { /* nie JSON */ }
    if (/logowanie/i.test(msg)) token = null; // token wygasł – następnym razem weźmiemy nowy
    throw new Error(msg || 'Serwer znajomych nie odpowiada (' + r.status + ')');
  }
  return text ? JSON.parse(text) : null;
}

const heartbeat = async playing => rpc('void_heartbeat', { p_token: await tok(), p_playing: !!playing });
const list = async () => (await rpc('void_friends', { p_token: await tok() })) || [];
const add = async name => rpc('void_friend_add', { p_token: await tok(), p_name: String(name || '').trim() });
const remove = async uuid => rpc('void_friend_remove', { p_token: await tok(), p_uuid: uuid });
const reset = () => { token = null; };

module.exports = { heartbeat, list, add, remove, reset };
