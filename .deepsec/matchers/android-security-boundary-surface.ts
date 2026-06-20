import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

const MULTIPLAYER_PATH =
  /(?:^|[\\/])app[\\/]src[\\/]main[\\/]java[\\/]com[\\/]finnvek[\\/]cornersapart[\\/]multiplayer[\\/]/;
const DATA_PATH = /(?:^|[\\/])app[\\/]src[\\/]main[\\/]java[\\/]com[\\/]finnvek[\\/]cornersapart[\\/]data[\\/]/;

export const androidSecurityBoundarySurface: MatcherPlugin = {
  slug: "android-security-boundary-surface",
  description:
    "Android Nearby and JSON DataStore trust-boundary surfaces that should receive Kotlin security review",
  noiseTier: "normal",
  filePatterns: [
    "app/src/main/java/com/finnvek/cornersapart/multiplayer/**/*.kt",
    "app/src/main/java/com/finnvek/cornersapart/data/**/*.kt",
  ],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    if (MULTIPLAYER_PATH.test(filePath)) {
      return regexCandidates("android-security-boundary-surface", content, [
        {
          regex: /\bGameProtocol\.decode\s*\(/,
          label: "Nearby inbound protocol decode boundary",
        },
        {
          regex: /\bonBytesPayload\s*\(/,
          label: "Nearby inbound bytes callback boundary",
        },
        {
          regex: /\b(?:facade|client)\.sendPayload\s*\(/,
          label: "Nearby outbound payload send boundary",
        },
        {
          regex: /\b(?:startAdvertising|startDiscovery|requestConnection|acceptConnection)\s*\(/,
          label: "Nearby connection lifecycle boundary",
        },
      ]);
    }

    if (DATA_PATH.test(filePath)) {
      return regexCandidates("android-security-boundary-surface", content, [
        {
          regex: /\bdataStore\s*\(/,
          label: "Android DataStore file declaration boundary",
        },
        {
          regex: /\bjson\.decodeFromString\s*\(/,
          label: "JSON DataStore read boundary",
        },
        {
          regex: /\bjson\.encodeToString\s*\(/,
          label: "JSON DataStore write boundary",
        },
      ]);
    }

    return [];
  },
};
