import assert from "node:assert/strict";
import { test } from "node:test";
import { androidExportedComponent } from "./android-exported-component.js";

test("ignores the exported launcher activity", () => {
  const manifest = `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application>
    <activity
      android:name=".MainActivity"
      android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>`;

  const matches = androidExportedComponent.match(manifest, "app/src/main/AndroidManifest.xml");

  assert.deepEqual(matches, []);
});

test("flags exported components that are not launcher activities", () => {
  const manifest = `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application>
    <service
      android:name=".SyncService"
      android:exported="true" />
  </application>
</manifest>`;

  const matches = androidExportedComponent.match(manifest, "app/src/main/AndroidManifest.xml");

  assert.equal(matches.length, 1);
  assert.equal(matches[0]?.vulnSlug, "android-exported-component");
});
