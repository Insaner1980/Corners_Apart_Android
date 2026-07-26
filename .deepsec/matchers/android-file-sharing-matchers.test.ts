import assert from "node:assert/strict";
import { test } from "node:test";
import { androidUriShareWithoutClipData } from "./android-uri-share-without-clipdata.js";
import { fileproviderBroadPath } from "./fileprovider-broad-path.js";
import { sensitiveAndroidLog } from "./sensitive-android-log.js";

test("FileProvider matcher covers single-quoted broad paths", () => {
  const matches = fileproviderBroadPath.match(
    "<paths><cache-path name='cache' path='.'/></paths>",
    "app/src/main/res/xml/file_paths.xml",
  );

  assert.equal(matches.length, 1);
});

test("sensitive log matcher covers multiline calls", () => {
  const matches = sensitiveAndroidLog.match(
    `Log.w(
      TAG,
      "Nearby endpoint payload: $payload",
    )`,
    "app/src/main/java/com/finnvek/cornersapart/Logger.kt",
  );

  assert.equal(matches.length, 1);
});

test("sensitive log matcher does not consume a later Kotlin statement", () => {
  const matches = sensitiveAndroidLog.match(
    `Log.i(TAG, "sync complete")
sendPayload(endpointId, payload)`,
    "app/src/main/java/com/finnvek/cornersapart/Logger.kt",
  );

  assert.deepEqual(matches, []);
});

test("URI share matcher evaluates each share construction independently", () => {
  const content = `
fun safe(uri: Uri): Intent =
  Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_STREAM, uri)
    clipData = ClipData.newUri(resolver, "safe", uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }

fun unsafe(uri: Uri): Intent =
  Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_STREAM, uri)
  }
`;

  const matches = androidUriShareWithoutClipData.match(
    content,
    "app/src/main/java/com/finnvek/cornersapart/ShareFactory.kt",
  );

  assert.equal(matches.length, 1);
  assert.equal(
    matches[0]?.matchedPattern,
    "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
  );
});
