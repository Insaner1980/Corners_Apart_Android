import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { candidate, isTestFile } from "./utils.js";

const sendAction = /\bIntent\.ACTION_SEND(?:_MULTIPLE)?\b/g;

export const androidUriShareWithoutClipData: MatcherPlugin = {
  slug: "android-uri-share-without-clipdata",
  description:
    "ACTION_SEND content URI shares that should pair EXTRA_STREAM with read grants and ClipData",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!content.includes("Intent.ACTION_SEND") && !content.includes("Intent.ACTION_SEND_MULTIPLE")) return [];
    if (!content.includes("Intent.EXTRA_STREAM")) return [];

    return shareScopes(content).flatMap(({ start, text }) => {
      if (!text.includes("Intent.EXTRA_STREAM")) return [];

      const hasReadGrant = text.includes("FLAG_GRANT_READ_URI_PERMISSION");
      const hasClipData = /\bclipData\b|ClipData\./.test(text);
      if (hasReadGrant && hasClipData) return [];

      return candidate(
        "android-uri-share-without-clipdata",
        content,
        start,
        hasReadGrant
          ? "EXTRA_STREAM content URI share without ClipData"
          : "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
      );
    });
  },
};

function shareScopes(content: string): Array<{ start: number; text: string }> {
  const starts = [...content.matchAll(sendAction)].map((match) => match.index ?? 0);
  return starts.map((start, index) => {
    const nextShareStart = starts[index + 1] ?? content.length;
    const enclosingBlockEnd = findEnclosingBlockEnd(content, start);
    const end = Math.min(nextShareStart, enclosingBlockEnd);
    return { start, text: content.slice(start, end) };
  });
}

function findEnclosingBlockEnd(content: string, index: number): number {
  const openBraces: number[] = [];
  for (let cursor = 0; cursor < index; cursor += 1) {
    if (content[cursor] === "{") {
      openBraces.push(cursor);
    } else if (content[cursor] === "}") {
      openBraces.pop();
    }
  }

  if (openBraces.length === 0) return content.length;

  let depth = 1;
  for (let cursor = index; cursor < content.length; cursor += 1) {
    if (content[cursor] === "{") {
      depth += 1;
    } else if (content[cursor] === "}") {
      depth -= 1;
      if (depth === 0) return cursor;
    }
  }
  return content.length;
}
