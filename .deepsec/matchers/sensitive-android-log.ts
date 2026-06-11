import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

const sensitiveWords =
  "(?:nearby|endpoint|payload|profile|player|saved|settings|board|move|token|cornersapart|corners)";

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose game, profile, Nearby endpoint, payload, save, or board state",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("sensitive-android-log", content, [
      {
        regex: new RegExp(
          String.raw`\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e)\s*\([^;\n]*${sensitiveWords}[^;\n]*\)`,
          "i",
        ),
        label: "Sensitive game or Nearby term in Android log call",
      },
    ]);
  },
};
