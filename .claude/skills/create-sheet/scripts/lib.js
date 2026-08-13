// Shared helpers for the `sheets` collection admin scripts.
const path = require('path');

const KEY_PATH = path.join(__dirname, '..', '..', '..', '..', '.secrets', 'sa.json');

function db() {
  const { initializeApp, cert, getApps } = require('firebase-admin/app');
  const { getFirestore } = require('firebase-admin/firestore');
  let sa;
  try {
    sa = require(KEY_PATH);
  } catch {
    throw new Error(
      `service account key not found at ${KEY_PATH}\n` +
      `  Firebase Console -> Project Settings -> Service Accounts -> Generate new private key,\n` +
      `  then save it to that path (chmod 600).`
    );
  }
  if (!getApps().length) initializeApp({ credential: cert(sa) });
  return getFirestore();
}

// Mirrors directImageURL() in SheetMusicView.swift: pull the Drive file ID and
// build the raw-bytes URL the app will actually request.
function directImageURL(urlString) {
  if (!urlString.includes('drive.google.com')) return urlString;
  let id = null;
  const m = urlString.match(/(?<=\/d\/)[^/?]+/);
  if (m) id = m[0];
  else {
    try { id = new URL(urlString).searchParams.get('id'); } catch { /* not a URL */ }
  }
  return id ? `https://lh3.googleusercontent.com/d/${id}` : urlString;
}

// A restricted Drive file returns an HTML login page with HTTP 200, which renders
// as a blank sheet in the app with no error. Check the content type, not the status.
async function checkImage(urlString) {
  const direct = directImageURL(urlString);
  try {
    const res = await fetch(direct, { redirect: 'follow', signal: AbortSignal.timeout(30000) });
    const type = res.headers.get('content-type') || '';
    const buf = await res.arrayBuffer();
    return {
      ok: res.ok && type.startsWith('image/'),
      status: res.status,
      type: type.split(';')[0] || '(none)',
      bytes: buf.byteLength,
      direct,
    };
  } catch (e) {
    return { ok: false, status: 'ERR', type: e.message, bytes: 0, direct };
  }
}

async function survey(database) {
  const snap = await database.collection('sheets').get();
  const orders = snap.docs
    .map(d => d.data().order)
    .filter(n => typeof n === 'number');
  const maxIdNum = Math.max(0, ...snap.docs.map(d => {
    const m = d.id.match(/^sheet_(\d+)$/);
    return m ? parseInt(m[1], 10) : 0;
  }));
  return {
    size: snap.size,
    ids: new Set(snap.docs.map(d => d.id)),
    orders,
    minOrder: orders.length ? Math.min(...orders) : 1,
    maxIdNum,
  };
}

const sheetId = n => `sheet_${String(n).padStart(3, '0')}`;

module.exports = { db, directImageURL, checkImage, survey, sheetId, KEY_PATH };
