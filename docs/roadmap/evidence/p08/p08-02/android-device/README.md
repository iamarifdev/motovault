# P08-02 Android physical-device pass — provenance

Device: motorola edge 50 fusion (motorola), Android 16 / API 36, arm64-v8a, 1080x2400 @ 400dpi
Build fingerprint: motorola/cuscoi_g/cuscoi:16/W1UUIS36H.110-42-4/83f47c-9d83a:user/release-keys
Serial recorded internally, omitted from evidence.

Probe APK installed (initial pass): e60d3bf49011cea4b48aef711b849e35eb47710b954d04f4c207860968bd5564
Test APK installed (initial pass): 7f7c82fbae147c414896f928f57a9629282ed007f418f3c9de98c076701c7713
Probe APK installed (post-HistoryScreen fix & P09–P15 pass): 8bd3b17cb42704f4254b448a63a05b40ca9d340966b70d31d86f8d4bc6395b90
Test APK installed (post-HistoryScreen fix & P09–P15 pass): 0e8aa4f41f29e3fc915b66a2b5f0bd63c356b70c5eba70d70a9ff1edbd267a00
Source: P08 initial pass (`e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4` + working tree) followed by `HistoryScreen.kt` / `ProbeFuelChecks.kt` + P09–P16 working tree.

Device settings before: font_scale=1.15, night mode=no, screen_off_timeout=60000 ms, brightness=110, stay_on_while_plugged_in=0
Camera permission before: granted=true (previous synthetic probe session); revoked only to capture the prompt and restored to granted=true afterwards.
POST_NOTIFICATIONS before: granted=false. The system prompt was captured (10-*); the allow button was not successfully confirmed in the dump (the dialog was still present right after the tap), so no grant is claimed and the permission was left at its original false value.
stay_on_while_plugged_in temporarily set to 3 and restored to 0; no OS locale, night-mode, font-scale or timeout change was made.

Native probe: adb shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
Result: PASS 37 native storage checks (see native-probe-device-unlocked.txt).

Performance measurements on this device:
  cold start (am start -W, fixture activity): 943-1018 ms across launches
  TOTAL PSS: ~82 MB at launch, ~133 MB after permission flow, ~137 MB after list scrolling
  frames: 431 rendered, 6 janky (1.39%), 50th 12 ms, 90th 14 ms, 95th 15 ms
  battery: 89% -> 88% while USB charging over ~6 minutes (not a battery-drain measurement)

Files are sanitized screenshots/XML dumps from the isolated synthetic probe only; no owner data.


Files and SHA-256:

Files and SHA-256:

Files and SHA-256:

Files and SHA-256:
94ae59373efc0ca9827e06ae24c749c92b06148996f6b8fc817d8e95e30d3dfa  01-launch-records-bn.png
4fd3c7446d97ba20aeb3ac512f430b051689698dc360a9d02f3cd3812f8fac89  01-launch-records-bn.xml
53cc2184fe5b83a040083649b516c420054a85c4c72b9168955de7b43b543b26  02-profile-bn.png
3865bd71d67aba8d9a3d560f8180c075a39d9fc42a742b4444095034337c56b1  02-profile-bn.xml
aba581527f05f126cec69d862a47864f7c97e4ca71b952f05d9be47a03c6ac99  03-profile-en.png
e14de9790e01dea641932eb869cba88b4a461d7be2bbdf656c54e285cfa02919  03-profile-en.xml
c37b1c54589d4d2739bfe9c711c674e3e213bae778baaf37ee52ca157fd343d8  04-records-en.png
7c9ddf88a98205c328b82f5fb47f34706c627d8de869db2ad40f7e098da48773  05-garage-en.png
3865bd71d67aba8d9a3d560f8180c075a39d9fc42a742b4444095034337c56b1  06-profile-bn-again.xml
b9d9478323db5af68d8f2b9792a1e8721ce8b4f7cb1aee9c697001e0246467f0  07-profile-1.3-bn.png
9856f8b2ff376fda0ff45ac83781b9a40b3aac14dbc3bb0a811235a700d42c7f  07b-profile-1.3.xml
00c9853f22d0fc328f48596bea4f88916ac36f3980880a24824bfb627aa157ab  08-records-1.3-bn.png
5cb6c1ab24bdfd5149b6a3ef33837c1dd14c7fa00e87e52d894050900bd5ecd4  08-records-1.3-bn.xml
5cb6c1ab24bdfd5149b6a3ef33837c1dd14c7fa00e87e52d894050900bd5ecd4  08b-records.xml
9856f8b2ff376fda0ff45ac83781b9a40b3aac14dbc3bb0a811235a700d42c7f  09-profile-for-notification.xml
92cd0e376b7ee60bd5f9b2aafc31117d440a19471486f70af48e39c865822d3d  10-notification-permission-dialog.png
d0a50c625fa4cdf090e65768efeeede3c343324edce6ccfa176957956517c6f3  10-notification-permission-dialog.xml
92cd0e376b7ee60bd5f9b2aafc31117d440a19471486f70af48e39c865822d3d  11-notification-dialog-after-tap.png
d0a50c625fa4cdf090e65768efeeede3c343324edce6ccfa176957956517c6f3  11b-notification-dialog-after-tap.xml
3d97c61ec567ed5148404781794af46f0337bcb6147676beae4802f85630a273  13-odometer-sheet.xml
5fa6bb658f091d4cccf91697d3f7503f0b80cae3d89748cae2363bf5d47dfe6a  14-camera-permission-dialog.png
03618293d9cd527b1e9eb4bb323120ae501f6896c1c7f5d7ff48bcd6c8bfed50  14-camera-permission-dialog.xml
5fa6bb658f091d4cccf91697d3f7503f0b80cae3d89748cae2363bf5d47dfe6a  15-camera-denied-fallback.png
03618293d9cd527b1e9eb4bb323120ae501f6896c1c7f5d7ff48bcd6c8bfed50  15-camera-denied-fallback.xml
4fd3c7446d97ba20aeb3ac512f430b051689698dc360a9d02f3cd3812f8fac89  c01-garage.xml
69915083a74fdd412d16eb0a9711306505b0729ea1f77545f200c4224464cd36  c01b-garage-active.xml
4bb4439052b23965f64341ac01b3a7daf5571f9ebc9acfb3bbcea073090427cd  c02-odometer-sheet.xml
aea9d36243df162b3feb85211b3148f9df4582e2f1616637f46224c147c76808  c03-camera-permission-dialog.png
b9e7f0f3cd99c4bc54a45376f63e74e9f565f57833bdf128b6c9126e98a50229  c03-camera-permission-dialog.xml
9455b21075408618df655768e8da89e5a74e0a403b139a2998b0d8a1e7781b6e  c04-camera-denied-fallback.png
40c1d6db32c6ae7c33b337075279d529ebb1be2595a9c141764d51c397c5f350  c04-camera-denied-fallback.xml
f06ba2c864c6dccdadb7ec6072e29e8eddd75122108252c43a581b172303a0d6  extra-picker-01-launch.xml
4cc44f4275746a50ff764a4344a3d860a807559422bbb7ff57049a925f93cce1  extra-picker-02-documents.xml
d7984cf103cb7ec94bbdaae586fb2e2398ba88f4af80d9ff7d7f77219bb28bdc  extra-picker-03-type-sheet.xml
2af0437fa65295611eb7d4237963dd7a09eccca33a971678a732b8ab4d937c98  extra-picker-04-scan.xml
8c05c296b56db60589184f7f361323980c119e158ba0443fb3061fee377de866  extra-share-rejected-mime.xml
0ddefc01c3059138b915b006523773039710432ffdd2571afe77cab096493f0b  native-probe-device.txt
f06ba2c864c6dccdadb7ec6072e29e8eddd75122108252c43a581b172303a0d6  notif-01-launch.xml
8da66d2fddbc20900c63a331eb4d614afc1ac9c54f9f7e572ea65db8923c8361  notif-02-profile.xml
d3e5caf88caea5085681275920c6125d5ee3fe6b4e668484304805bdb6d640eb  notif-03-reminder-card-granted.png
8da66d2fddbc20900c63a331eb4d614afc1ac9c54f9f7e572ea65db8923c8361  notif-03-reminder-card-granted.xml
f06ba2c864c6dccdadb7ec6072e29e8eddd75122108252c43a581b172303a0d6  p2-01-launch.xml
e65e13ba3b42fcfef6eb37a59497ab55fe8d78a3ea8752e757a1616f84ce9757  p2-02-profile.xml
3c28874ae3715b87faecd69cb6e9346fb4db9629ccda79a0617f8c87e4155f5e  p2-03-restore-sheet.xml
f06ba2c864c6dccdadb7ec6072e29e8eddd75122108252c43a581b172303a0d6  p2-cam-01-launch.xml
3da551dfef22f7fe850321cc30bab659a9b2e9ebd57fceaae8e8ceae46418a93  p2-cam-02-garage.xml
4bb4439052b23965f64341ac01b3a7daf5571f9ebc9acfb3bbcea073090427cd  p2-cam-03-sheet.xml
1c6fe59b20853a5d6f2d92885a5d1a066d9d198a30b7b66695a21b5a01604fa8  p2-cam-04-after-cancel.png
4bb4439052b23965f64341ac01b3a7daf5571f9ebc9acfb3bbcea073090427cd  p2-cam-04-after-cancel.xml
3656784b3f070446f88ad924eef465b2f908163694b09b6ae39ae5fc45bf9752  p2-largevault-perf.txt
a361f97d509ce26e510c9c9b5a7b1741a3a3756449050f948ebff1d542553a2d  p2-largevault-records.png
f97e77c34ab1875c79d25f2dc38ac703481ea2f93f8b8ee79e9d9deecca5f4c9  p2-largevault-records.xml
abf2611d19950c29dec86f69bd3dd9b0ccb211ca81f7c8ce7c1eaaa8fcf41b15  p2-notification-delivered-filtered.txt
16b2763faae5b95ff7147035cda2e751d92360ca1a97a525094a7aaa0f40e05a  p2-reminder-alarms-filtered.txt
8cb2143607f9e95e67dc0f51e55989b054a489067e2b99dd6ff6137acc25d60e  p2-sched-01-main.xml
f06ba2c864c6dccdadb7ec6072e29e8eddd75122108252c43a581b172303a0d6  picker-01-launch.xml
e65e13ba3b42fcfef6eb37a59497ab55fe8d78a3ea8752e757a1616f84ce9757  picker-02-profile.xml
3c28874ae3715b87faecd69cb6e9346fb4db9629ccda79a0617f8c87e4155f5e  picker-03-restore-sheet.xml
d31231c891e6b09739eb0f5cf464ce15cb3bcbb18fc8d962bf44f4fd86f5d7f9  picker-04-after-cancel.png
e65e13ba3b42fcfef6eb37a59497ab55fe8d78a3ea8752e757a1616f84ce9757  picker-04-after-cancel.xml
b54b3c39fc817875d0297bd6a3e9e74824b069fbca8d0ceadab59a4dc5c932ad  picker-files-02-after-cancel.png
3c28874ae3715b87faecd69cb6e9346fb4db9629ccda79a0617f8c87e4155f5e  picker-files-02-after-cancel.xml
539d9acc99eba47f959c1f0cb74ea0c5e1f50944ddcf7bbf00af40a9f72aaa41  share-cold-review.png
8e6b670b2544cc3824b235f7ef33645aae95ce26207eb222fd116bbaf472330a  share-cold-review.xml
12d8cde31054bfc0f1e9fb307d5fdf315d0f07be9ed6796c94c86b7aef7c92e3  share-mime-rejection.png
8c05c296b56db60589184f7f361323980c119e158ba0443fb3061fee377de866  share-mime-rejection.xml
b268773ce361c25720a5f9489ea3a2ce7d7ff0230ae3cc05b0ee45dd80465b5a  share-warm-review.png
8cb2143607f9e95e67dc0f51e55989b054a489067e2b99dd6ff6137acc25d60e  share-warm-review.xml
2e33f345bcf6f7927859d8aad9d444f4800f65e96e7d1459f7ecec744ddda62e  large-vault-lazycolumn-records.png
124c6b067dd3fbef9cd275bb69706dd5451d987a6e3684d2c6379dfd04047bf9  profile-swatches-wrapped-device.png
6b9a5c91793d0ad04102bc686796ab49d86f122384c2ed5ffc704b0adfca4ddd  profile-swatches-wrapped-device.xml
