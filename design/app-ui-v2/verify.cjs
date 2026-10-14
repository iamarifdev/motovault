const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const elements = new Map();
const element = id => {
  if (!elements.has(id)) elements.set(id, { value: '', hidden: true, textContent: '', addEventListener() {}, setAttribute() {} });
  return elements.get(id);
};
const context = vm.createContext({ console, Intl, Date, Number, String, Math, JSON, setTimeout, clearTimeout, URL, URLSearchParams, Blob,
  localStorage: { getItem: () => null, setItem() {} }, location: { hash: '#garage' },
  structuredClone,
  document: { getElementById: element, querySelectorAll: () => [], addEventListener() {},createElement: () => ({...element('new'),classList:{toggle() {}},after() {}}),querySelector: () => ({after() {}}) },
  window: { addEventListener() {} }
});
const source = fs.readFileSync(__dirname + '/app.js', 'utf8').replace(/render\(\);\s*$/, '');
vm.runInContext(source, context);
vm.runInContext(fs.readFileSync(__dirname+'/catalogue.js','utf8'),context);
const v2=fs.readFileSync(__dirname+'/ui-v2.js','utf8').replace(/render\(\);\s*$/,'').replace(/fontStyle\(state\.font\|\|V2_DEFAULT_FONT\);/,'');
vm.runInContext(v2,context);
for (const [input, expected] of [['১২৫০০',12500],['৫',5],['৬৫০',650],['5.5',5.5],['১২,৪৫০',12450]]) {
  element('test-number').value=input;
  assert.equal(vm.runInContext("parseNumber(document.getElementById('test-number').value)", context),expected,input);
  assert.equal(vm.runInContext("validNumber('test-number')", context),expected,input);
}
assert.equal(vm.runInContext("Object.keys(screens).length", context),35);
assert.equal(vm.runInContext("NATIVE_CATALOGUE.models.length",context),78);
assert.equal(vm.runInContext("Object.keys(NATIVE_CATALOGUE.logos).length",context),9);
assert.equal(vm.runInContext("SERVICE_ITEMS.length",context),30);
assert.equal(vm.runInContext("billTotal(state.serviceBills[0]).total",context),1650);
assert.equal(vm.runInContext("billTotal({items:[{quantity:1,price:null}],labour:0,misc:0}).complete",context),false);
const parsed=vm.runInContext("parseSmartText('অয়েল ৯৫০, ফিল্টার ২৫০, মজুরি ৩০০, ১২৬০০ কিমি')",context);
assert.equal(parsed.items.length,2);
assert.equal(parsed.labour,300);
assert.equal(parsed.odo,12600);
assert.equal(parsed.items[0].unit,'total');
assert.equal(vm.runInContext("parseSmartText('oil filter 250').items[0].id",context),'oil-filter');
assert.equal(vm.runInContext("parseSmartText('অয়েল ৯৫০ ফিল্টার ২৫০').unmatched.length",context),1);
assert.equal(vm.runInContext("parseSmartText('অয়েল ১,২০০').items[0].price",context),1200);
assert.equal(vm.runInContext("careCompleted({id:'chain-adjust',action:'adjust'},'chain')",context),false);
assert.equal(vm.runInContext("careCompleted({id:'chain-clean',action:'inspect'},'chain')",context),false);
assert.equal(vm.runInContext("careCompleted({id:'chain-clean',action:'clean'},'chain')",context),true);
assert.equal(vm.runInContext("billTotal({items:[{quantity:1,price:950,usedAmount:.9,unit:'bottle'}],labour:0,misc:0}).total",context),950);
for (const path of vm.runInContext("[...NATIVE_CATALOGUE.models.map(m=>m.photo),...Object.values(NATIVE_CATALOGUE.logos)]",context)) assert.ok(fs.existsSync(__dirname+'/'+path),path);
for (const input of ['1,5', '-3', '2e4', 'foo', '']) assert.ok(Number.isNaN(vm.runInContext(`parseNumber(${JSON.stringify(input)})`,context)),input+' rejected');
assert.equal(vm.runInContext("deriveBaselines(state.logs,['oil'])[0].lastOdo",context),12300);
assert.equal(vm.runInContext("deriveBaselines([],['oil'])[0].known",context),false);
for (const route of vm.runInContext('Object.keys(screens)', context)) {
  const html=vm.runInContext(`views[${JSON.stringify(route)}]()`,context);
  assert.ok(html.includes('<h1>'),route+' heading');
  assert.ok(!html.includes('undefined'),route+' undefined output');
}
console.log('PASS: numeral input, 78 models / 9 logos, 30 service items, itemized totals, smart draft, 35 screens.');
