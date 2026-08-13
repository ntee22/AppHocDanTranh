// Read-only survey of the `sheets` collection: current order values, doc ids,
// and a field-type census to catch any doc that drifted from the schema.
//
//   node inspect.js
//
const { db } = require('./lib');

(async () => {
  const snap = await db().collection('sheets').orderBy('order').get();
  console.log(`total docs: ${snap.size}\n`);

  console.table(snap.docs.map(doc => {
    const d = doc.data();
    return {
      id: doc.id,
      order: d.order,
      oType: Number.isInteger(d.order) ? 'int' : typeof d.order,
      title: (d.title || '').slice(0, 34),
      pages: Array.isArray(d.pages) ? d.pages.length : `NOT-ARRAY(${typeof d.pages})`,
      isFree: d.isFree,
      isVisible: d.isVisible,
    };
  }));

  const fields = {};
  snap.forEach(doc => Object.entries(doc.data()).forEach(([k, v]) => {
    const t = Array.isArray(v) ? 'array' : typeof v;
    fields[k] = fields[k] || {};
    fields[k][t] = (fields[k][t] || 0) + 1;
  }));
  console.log('\nfield -> type -> count (all should equal total docs):');
  console.log(JSON.stringify(fields, null, 2));

  // Docs the app will silently skip.
  const hidden = snap.docs.filter(d => d.data().isVisible !== true);
  const badOrder = snap.docs.filter(d => !Number.isInteger(d.data().order));
  if (hidden.length) console.log(`\n⚠️  isVisible !== true (hidden in app): ${hidden.map(d => d.id).join(', ')}`);
  if (badOrder.length) console.log(`⚠️  non-integer order (skipped by app): ${badOrder.map(d => d.id).join(', ')}`);
  if (!hidden.length && !badOrder.length) console.log('\nno docs are being skipped by the app.');

  process.exit(0);
})().catch(e => { console.error('ERROR: ' + e.message); process.exit(1); });
