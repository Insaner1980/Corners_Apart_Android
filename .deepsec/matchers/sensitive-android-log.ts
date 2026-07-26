import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { candidate, isTestFile } from "./utils.js";

const sensitiveWords =
  "(?:nearby|endpoint|payload|profile|player|saved|settings|board|move|token|cornersapart|corners)";
const sensitiveWordPattern = new RegExp(sensitiveWords, "i");
const logCallStart = /\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e)\s*\(/g;

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose game, profile, Nearby endpoint, payload, save, or board state",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return [...content.matchAll(logCallStart)].flatMap((match) => {
      const start = match.index ?? 0;
      const openParenthesis = start + match[0].lastIndexOf("(");
      const end = findCallEnd(content, openParenthesis);
      if (end === null || !sensitiveWordPattern.test(content.slice(openParenthesis + 1, end))) return [];

      return candidate(
        "sensitive-android-log",
        content,
        start,
        "Sensitive game or Nearby term in Android log call",
      );
    });
  },
};

function findCallEnd(content: string, openParenthesis: number): number | null {
  let depth = 0;
  let quote: "'" | '"' | null = null;
  let escaped = false;

  for (let cursor = openParenthesis; cursor < content.length; cursor += 1) {
    const character = content[cursor];
    if (quote !== null) {
      if (escaped) {
        escaped = false;
      } else if (character === "\\") {
        escaped = true;
      } else if (character === quote) {
        quote = null;
      }
      continue;
    }

    if (character === "'" || character === '"') {
      quote = character;
    } else if (character === "(") {
      depth += 1;
    } else if (character === ")") {
      depth -= 1;
      if (depth === 0) return cursor;
    }
  }
  return null;
}
