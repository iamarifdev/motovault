#!/bin/sh
# Xcode 27 simulator builds leave the embedded share extension with an ad-hoc
# signature and no entitlements, so its app-group container is unavailable.
# Re-sign the extension with the app group read from its own Info.plist, so the
# entitlement matches the group the extension uses at runtime. Simulator only.
set -eu

[ "${EFFECTIVE_PLATFORM_NAME:-}" = "-iphonesimulator" ] || exit 0
APPEX="${TARGET_BUILD_DIR}/${WRAPPER_NAME}/PlugIns/CaptureShare.appex"
[ -d "$APPEX" ] || exit 0

GROUP=$(/usr/libexec/PlistBuddy -c "Print :CaptureAppGroup" "$APPEX/Info.plist")
ENTITLEMENTS="${DERIVED_FILE_DIR}/CaptureShare.expanded.entitlements"
cat > "$ENTITLEMENTS" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>com.apple.security.application-groups</key>
	<array>
		<string>${GROUP}</string>
	</array>
</dict>
</plist>
PLIST
/usr/bin/codesign --force --sign - --entitlements "$ENTITLEMENTS" "$APPEX"
