import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const rawNearbyBypass: MatcherPlugin = {
  slug: "raw-nearby-bypass",
  description:
    "Raw Bluetooth, Wi-Fi Direct, or Wi-Fi Aware APIs that bypass the v1 Nearby Connections boundary",
  noiseTier: "precise",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("raw-nearby-bypass", content, [
      {
        regex:
          /\b(BluetoothAdapter|BluetoothManager|BluetoothSocket|BluetoothServerSocket|WifiP2pManager|WifiAwareManager)\b/,
        label: "Raw Android connectivity API outside Nearby Connections",
      },
    ]);
  },
};
