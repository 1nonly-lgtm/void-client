// Logowanie kontem Microsoft (msmc). Token odświeżania szyfrowany przez Windows (safeStorage).
const { safeStorage } = require('electron');
const settings = require('./settings');

let AuthClass = null;
function manager() {
  if (!AuthClass) AuthClass = require('msmc').Auth; // ładowane dopiero przy logowaniu – szybszy start
  return new AuthClass('select_account');
}

function encrypt(text) {
  if (safeStorage.isEncryptionAvailable()) return 'enc:' + safeStorage.encryptString(text).toString('base64');
  return 'raw:' + Buffer.from(text).toString('base64');
}

function decrypt(stored) {
  const [kind, payload] = [stored.slice(0, 4), stored.slice(4)];
  const buf = Buffer.from(payload, 'base64');
  return kind === 'enc:' ? safeStorage.decryptString(buf) : buf.toString();
}

function remember(xbox, mc) {
  const account = { name: mc.profile.name, uuid: mc.profile.id, refresh: encrypt(xbox.save()) };
  settings.setAccount(account);
  return { name: account.name, uuid: account.uuid };
}

function friendly(err) {
  const msg = String(err && (err.message || err.ts || err) || '');
  if (/error\.gui\.closed|closed/i.test(msg)) return new Error('Logowanie anulowane');
  if (/NotOwned|own|profile/i.test(msg)) return new Error('To konto nie ma kupionego Minecrafta (Java Edition)');
  return new Error('Logowanie nie powiodło się: ' + msg);
}

async function login() {
  try {
    const xbox = await manager().launch('electron', { width: 520, height: 680, resizable: false, title: 'Void Client – logowanie Microsoft' });
    const mc = await xbox.getMinecraft();
    return remember(xbox, mc);
  } catch (e) {
    throw friendly(e);
  }
}

function logout() {
  settings.setAccount(null);
}

/** Zwraca dane logowania dla minecraft-launcher-core, odświeżając token. */
async function getLaunchAuth() {
  const acc = settings.get().account;
  if (!acc || !acc.refresh) throw new Error('Najpierw zaloguj się kontem Microsoft (prawy górny róg)');
  try {
    const xbox = await manager().refresh(decrypt(acc.refresh));
    const mc = await xbox.getMinecraft();
    remember(xbox, mc);
    return mc.mclc();
  } catch (e) {
    throw new Error('Sesja wygasła – zaloguj się ponownie. (' + (e.message || e) + ')');
  }
}

/** Aktualny token Minecrafta (np. do zmiany skina). */
async function getAccessToken() {
  const auth = await getLaunchAuth();
  return auth.access_token;
}

module.exports = { login, logout, getLaunchAuth, getAccessToken };
