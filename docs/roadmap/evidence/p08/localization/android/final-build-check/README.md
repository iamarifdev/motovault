# P08-05 final Android build spot check

The full Android language-switch matrix under ../ was captured on probe build
a0ee2636fadf57fef0b24bc9d88ec1582d9c121c059ee5e940ba393bb44c1581
(built after the P08-05 resource/format migration and copy fixes).

After that capture the tree gained only native error-path localization
(shared capture_*, pick_*, passport_* resources and their Android/iOS display
sites) plus the P08-05 iOS localization test. The final probe APK is
b903f93338d1120826a15ee6264846a2934e36397d62b1fd18fdfd6c06e51756
(test APK unchanged 7f7c82fb...); neither change touches the captured
tab/sheet/profile copy.

This directory verifies the final APK still renders the switched language and
the fixed copy: Garage/Profile in English, quick-add in English (fixed
'Add by typing or pasting' copy), Garage in Bangla. The installed APK SHA-256
was re-read from the device and matches b903f933...

Captured 2026-11-12 23:24 Asia/Dhaka with:
  adb -s emulator-5554 shell am start -n com.motovault.app.privacyprobe/com.motovault.app.FuelEditFixtureActivity --es language <en|bn> --es digits <en|bn>
  adb -s emulator-5554 exec-out screencap -p > <file>.png

SHA-256:
68209b5ea6fe47971acfd57445040447c3d352f82238691878d00c399e5a49d2  final-build-garage-bn.png
c1a7c590e10b633655a67928cf18200f880df4d7e2bfd4831a6b36f81b374827  final-build-profile-garage-en.png
897576b150f58166581a1921400373d5dd556e85314b9f6097e9981edef78207  final-build-quickadd-en.png
