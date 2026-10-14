"""Copy the native catalogue and convert its existing vector logos for the design reference.
No new catalogue claims or product prices are created.
"""
from pathlib import Path
import json
import re
import shutil
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
DRAWABLES = ROOT / 'composeApp/src/commonMain/composeResources/drawable'
SOURCE = ROOT / 'composeApp/src/commonMain/kotlin/com/motovault/app/ui/BikeCatalogue.kt'
ANDROID = '{http://schemas.android.com/apk/res/android}'

def convert(node):
    attrs = node.attrib
    if node.tag == 'path':
        return f'<path fill="#181c22" d="{attrs.get(ANDROID + "pathData", "")}"/>'
    if node.tag == 'group':
        transform = []
        tx, ty = attrs.get(ANDROID+'translateX','0'), attrs.get(ANDROID+'translateY','0')
        sx, sy = attrs.get(ANDROID+'scaleX','1'), attrs.get(ANDROID+'scaleY','1')
        if tx!='0' or ty!='0': transform.append(f'translate({tx} {ty})')
        if sx!='1' or sy!='1': transform.append(f'scale({sx} {sy})')
        return '<g transform="' + ' '.join(transform) + '">' + ''.join(convert(child) for child in node) + '</g>'
    return ''.join(convert(child) for child in node)

logos = {}
for path in DRAWABLES.glob('logo_*'):
    brand = path.stem.removeprefix('logo_')
    if path.suffix == '.xml':
        root = ET.parse(path).getroot()
        width, height = root.attrib[ANDROID+'viewportWidth'], root.attrib[ANDROID+'viewportHeight']
        output = HERE/'assets'/f'logo-{brand}.svg'
        output.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}">' + convert(root) + '</svg>')
    else:
        output = HERE/'assets'/f'logo-{brand}{path.suffix}'
        shutil.copyfile(path, output)
    logos[brand] = 'assets/' + output.name

models = []
pattern = r'BikeModel\("([^"]+)", "([^"]+)", "([^"]+)", Res\.drawable\.([\w]+), "([^"]+)", "([^"]+)"\)'
for ident, brand, name, resource, note, source in re.findall(pattern, SOURCE.read_text()):
    candidates = list(DRAWABLES.glob(resource + '.*'))
    if len(candidates)!=1: raise ValueError(f'Missing or ambiguous resource: {resource}')
    photo = candidates[0]
    shutil.copyfile(photo, HERE/'assets'/photo.name)
    models.append(dict(id=ident,brand=brand,name=name,photo='assets/'+photo.name,note=note,source=source))
assert len(models) >= 70, 'Catalogue extraction unexpectedly small'
(HERE/'catalogue.js').write_text('const NATIVE_CATALOGUE = ' + json.dumps(dict(models=models,logos=logos),ensure_ascii=False) + ';\n')
print(f'Prepared {len(models)} models and {len(logos)} original brand logos.')
