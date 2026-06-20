import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { type DeepsecPlugin, defineConfig } from "deepsec/config";
import { androidExportedComponent } from "./matchers/android-exported-component.js";
import { androidSecurityBoundarySurface } from "./matchers/android-security-boundary-surface.js";
import { androidUriShareWithoutClipData } from "./matchers/android-uri-share-without-clipdata.js";
import { fileproviderBroadPath } from "./matchers/fileprovider-broad-path.js";
import { rawNearbyBypass } from "./matchers/raw-nearby-bypass.js";
import { sensitiveAndroidLog } from "./matchers/sensitive-android-log.js";

const here = path.dirname(fileURLToPath(import.meta.url));

function cornersApartPlugin(): DeepsecPlugin {
  return {
    name: "corners-apart-android",
    matchers: [
      androidExportedComponent,
      androidSecurityBoundarySurface,
      fileproviderBroadPath,
      androidUriShareWithoutClipData,
      rawNearbyBypass,
      sensitiveAndroidLog,
    ],
  };
}

export default defineConfig({
  projects: [
    {
      id: "corners_apart_android",
      root: "..",
      infoMarkdown: fs.readFileSync(path.join(here, "data", "corners_apart_android", "INFO.md"), "utf-8"),
      promptAppend:
        "Prioritize Android exported components, FileProvider exports, URI grants, Nearby Connections trust boundaries, local profile/save privacy, and raw Bluetooth or Wi-Fi Direct bypasses.",
      priorityPaths: [
        "app/src/main/AndroidManifest.xml",
        "app/src/main/java/com/finnvek/cornersapart/multiplayer/",
        "app/src/main/java/com/finnvek/cornersapart/data/",
        "app/src/main/java/com/finnvek/cornersapart/model/",
      ],
    },
  ],
  plugins: [cornersApartPlugin()],
});
