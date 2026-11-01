"""Check that supported shared resource tables stay translatable."""

from collections import Counter
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2] / "composeApp/src/commonMain/composeResources"
PROJECT = Path(__file__).resolve().parents[2]
FORMAT_ARGUMENT = re.compile(r"%(\d+\$)?[-+#0,( ]*(?:\d+)?(?:\.\d+)?([a-zA-Z])")


def load_table(folder):
    entries = {}
    for file in sorted(folder.glob("*.xml")):
        for element in ET.parse(file).getroot():
            name = element.get("name")
            if not name:
                continue
            key = (element.tag, name)
            if key in entries:
                raise ValueError(f"Duplicate resource: {key}")
            entries[key] = element
    return entries


def placeholders(element):
    text = "".join(element.itertext())
    return placeholder_signature(text)


def placeholder_signature(text):
    """Compare argument position and type, including unnumbered arguments."""
    result = Counter()
    implicit = 0
    for position, kind in FORMAT_ARGUMENT.findall(text.replace("%%", "")):
        if position:
            index = int(position[:-1])
        else:
            implicit += 1
            index = implicit
        result[(index, kind)] += 1
    return result


def compare_tables(english, bangla):
    errors = []
    for key in sorted(english.keys() - bangla.keys()):
        errors.append(f"Missing translation: {key}")
    for key in sorted(bangla.keys() - english.keys()):
        errors.append(f"Unexpected translation: {key}")
    for key in sorted(english.keys() & bangla.keys()):
        if key[0] != "plurals":
            if placeholders(english[key]) != placeholders(bangla[key]):
                errors.append(f"Placeholder mismatch: {key}")
            continue
        variants = []
        for locale, entry in (("en", english[key]), ("bn", bangla[key])):
            items = list(entry)
            quantities = [item.get("quantity") for item in items]
            if "other" not in quantities or len(set(quantities)) != len(quantities) or None in quantities:
                errors.append(f"Invalid plural variants: {locale} {key}")
            variants.append({item.get("quantity"): item for item in items})
        base, translated = variants
        if "other" in base:
            for locale, table in (("en", base), ("bn", translated)):
                for quantity, item in table.items():
                    if placeholders(base["other"]) != placeholders(item):
                        errors.append(f"Plural placeholder mismatch: {locale} {key} {quantity}")
    return errors


class LocalizationResourcesTest(unittest.TestCase):
    def test_english_and_bangla_keys_and_placeholders_match(self):
        english = load_table(ROOT / "values")
        bangla = load_table(ROOT / "values-bn")
        self.assertEqual(compare_tables(english, bangla), [])

    def test_negative_translation_and_placeholder_fixtures(self):
        def table(xml):
            return {(item.tag, item.get("name")): item for item in ET.fromstring(xml)}
        base = table('<resources><string name="message">%1$s %2$d</string></resources>')
        self.assertEqual(compare_tables(base, base), [])
        self.assertTrue(compare_tables(base, {}))
        self.assertTrue(compare_tables(base, table('<resources><string name="message">%1$d %2$s</string></resources>')))
        self.assertNotEqual(placeholder_signature('%s %d'), placeholder_signature('%d %s'))
        self.assertEqual(placeholder_signature('%1$s %2$d'), placeholder_signature('%2$d %1$s'))

    def test_negative_plural_fixtures(self):
        def plural(xml):
            item = ET.fromstring(xml)
            return {("plurals", "count"): item}
        base = plural('<plurals name="count"><item quantity="other">%1$s</item></plurals>')
        self.assertTrue(compare_tables(base, plural('<plurals name="count"><item quantity="one">%1$s</item></plurals>')))
        self.assertTrue(compare_tables(base, plural('<plurals name="count"><item quantity="other">%1$d</item></plurals>')))
        self.assertTrue(compare_tables(base, plural('<plurals name="count"><item quantity="other">%1$s</item><item quantity="other">%1$s</item></plurals>')))

    def test_fuel_count_supports_singular_and_plural(self):
        english = load_table(ROOT / "values")[("plurals", "fuel_entry_count")]
        self.assertEqual({item.get("quantity") for item in english}, {"one", "other"})
        self.assertEqual(placeholders(english[0]), placeholders(english[1]))

    def test_native_reminder_tables_match_across_platforms_and_locales(self):
        tables = {}
        for locale, android_folder in (("en", "values"), ("bn", "values-bn")):
            xml = PROJECT / "composeApp/src/androidMain/res" / android_folder / "reminders.xml"
            android = {item.get("name"): item.text for item in ET.parse(xml).getroot()}
            ios_path = PROJECT / "iosApp/MotoVault" / f"{locale}.lproj/Localizable.strings"
            ios = dict(re.findall(r'^"([^"]+)"\s*=\s*"([^"]*)";', ios_path.read_text(), re.MULTILINE))
            self.assertTrue(android)
            shared_android = {key: value for key, value in android.items() if not key.startswith("reminder_channel_")}
            self.assertEqual(shared_android, ios)
            tables[locale] = android
        self.assertEqual(tables["en"].keys(), tables["bn"].keys())
        for key in tables["en"]:
            with self.subTest(resource=key):
                self.assertEqual(placeholder_signature(tables["en"][key]),
                                 placeholder_signature(tables["bn"][key]))


if __name__ == "__main__":
    unittest.main()
