// Add sheets to the top of the Firestore `sheets` collection.
//
//   node add-sheets.js [input.json]            # dry run — writes nothing
//   node add-sheets.js [input.json] --commit   # actually write
//
// Input JSON: array of { title, pages: [url, ...], isFree?, order? }
// Listed FIRST ends up TOPMOST in the app.
//
// New sheets get order values below every existing one (0, -1, -2, ...), so
// existing docs are never renumbered. Pass an explicit integer `order` to override.
//
const path = require('path');
const { db, checkImage, survey, sheetId } = require('./lib');

const args = process.argv.slice(2);
const COMMIT = args.includes('--commit');
const inputArg = args.find(a => !a.startsWith('--')) || 'new-sheets.json';
const inputPath = path.resolve(inputArg);

function validate(s, i) {
  const errs = [];
  if (typeof s.title !== 'string' || !s.title.trim()) errs.push('title must be a non-empty string');
  if (!Array.isArray(s.pages) || s.pages.length === 0) errs.push('pages must be a non-empty array');
  else s.pages.forEach((p, j) => {
    if (typeof p !== 'string' || !/^https?:\/\//.test(p)) errs.push(`pages[${j}] is not an http(s) URL`);
  });
  if (s.isFree !== undefined && typeof s.isFree !== 'boolean') errs.push('isFree must be a boolean');
  // A fractional order makes the doc invisible: SheetMusicView parses it as `as? Int`.
  if (s.order !== undefined && !Number.isInteger(s.order)) errs.push('order must be a whole number');
  if (errs.length) throw new Error(`input[${i}] ("${s.title || '?'}"):\n    - ${errs.join('\n    - ')}`);
}

(async () => {
  const input = require(inputPath);
  if (!Array.isArray(input) || input.length === 0) throw new Error(`${inputPath} must be a non-empty array`);
  input.forEach(validate);

  const database = db();
  const state = await survey(database);
  console.log(`existing: ${state.size} docs · min order=${state.minOrder} · highest id=${sheetId(state.maxIdNum)}\n`);

  const n = input.length;
  const planned = input.map((s, i) => {
    const id = sheetId(state.maxIdNum + 1 + i);
    if (state.ids.has(id)) throw new Error(`id collision: ${id} already exists — aborting`);
    return {
      id,
      data: {
        title: s.title.trim(),
        pages: s.pages,
        isFree: s.isFree !== undefined ? s.isFree : true,
        isVisible: true,                                    // required, or the app skips the doc
        order: s.order !== undefined ? s.order : state.minOrder - (n - i),
      },
    };
  });

  // Verify every image actually serves bytes before writing anything.
  console.log('=== IMAGE CHECK ===');
  let badImages = 0;
  for (const p of planned) {
    for (let j = 0; j < p.data.pages.length; j++) {
      const r = await checkImage(p.data.pages[j]);
      if (!r.ok) badImages++;
      console.log(
        `  ${r.ok ? 'OK  ' : 'FAIL'} ${p.id} page ${j + 1}: ` +
        `http=${r.status} type=${r.type} bytes=${r.bytes}`
      );
      if (!r.ok) console.log(`       -> ${r.direct}`);
    }
  }
  if (badImages) {
    console.log(
      `\n${badImages} image(s) did not return image bytes.\n` +
      `Most likely the Drive file is not shared "Anyone with the link" — a restricted\n` +
      `file returns an HTML login page and renders as a blank sheet in the app.`
    );
  }

  console.log('\n=== PLANNED WRITES (top of list = top of app) ===');
  planned.forEach(p => {
    console.log(`\n${p.id}  order=${p.data.order}  isFree=${p.data.isFree}`);
    console.log(`  title: ${p.data.title}`);
    p.data.pages.forEach((u, j) => console.log(`  page ${j + 1}: ${u}`));
  });

  const resulting = [...planned.map(p => p.data.order), ...state.orders].sort((a, b) => a - b);
  console.log(`\nresulting order sequence: ${resulting.join(', ')}`);

  if (!COMMIT) {
    console.log('\n*** DRY RUN — nothing written. Re-run with --commit to apply. ***');
    process.exit(0);
  }
  if (badImages) {
    console.error('\nREFUSING to commit: fix the failing image links first, or drop those pages.');
    process.exit(1);
  }

  // .create() throws if the doc exists — never silently overwrites a sheet.
  const batch = database.batch();
  planned.forEach(p => batch.create(database.collection('sheets').doc(p.id), p.data));
  await batch.commit();
  console.log(`\n✅ wrote ${planned.length} doc(s).`);

  const after = await database.collection('sheets').orderBy('order').limit(n + 3).get();
  console.log('\nverified — first docs by order now:');
  after.forEach(d => console.log(`  ${d.id}  order=${d.data().order}  ${d.data().title}`));
  process.exit(0);
})().catch(e => { console.error('\nERROR: ' + e.message); process.exit(1); });
