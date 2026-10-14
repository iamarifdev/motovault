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
  document: { getElementById: element, querySelectorAll: () => [], addEventListener() {} },
  window: { addEventListener() {} }
});
const source = fs.readFileSync(__dirname + '/app.js', 'utf8').replace(/render\(\);\s*$/, '');
vm.runInContext(source, context);
for (const [input, expected] of [['১২৫০০',12500],['৫',5],['৬৫০',650],['5.5',5.5],['১২,৪৫০',12450]]) {
  element('test-number').value=input;
  assert.equal(vm.runInContext("parseNumber(document.getElementById('test-number').value)", context),expected,input);
  assert.equal(vm.runInContext("validNumber('test-number')", context),expected,input);
}
assert.equal(vm.runInContext("Object.keys(screens).length", context),25);
for (const input of ['1,5', '-3', '2e4', 'foo', '']) assert.ok(Number.isNaN(vm.runInContext(`parseNumber(${JSON.stringify(input)})`,context)),input+' rejected');
assert.equal(vm.runInContext("deriveBaselines(state.logs,['oil'])[0].lastOdo",context),12300);
assert.equal(vm.runInContext("deriveBaselines([],['oil'])[0].known",context),false);
for (const route of vm.runInContext('Object.keys(screens)', context)) {
  const html=vm.runInContext(`views[${JSON.stringify(route)}]()`,context);
  assert.ok(html.includes('<h1>'),route+' heading');
  assert.ok(!html.includes('undefined'),route+' undefined output');
}
console.log('PASS: both digit scripts, invalid numeric formats, correction baselines, 25 screen renderers and headings.');
