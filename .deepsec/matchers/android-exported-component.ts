import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { candidate } from "./utils.js";

export const androidExportedComponent: MatcherPlugin = {
  slug: "android-exported-component",
  description:
    "Exported Android components other than the launcher activity that need explicit trust-boundary review",
  noiseTier: "normal",
  filePatterns: ["app/src/main/AndroidManifest.xml"],
  match(content): CandidateMatch[] {
    const matches: CandidateMatch[] = [];
    const componentRegex = /<(activity|activity-alias|service|receiver|provider)\b[^>]*>/g;

    for (const match of content.matchAll(componentRegex)) {
      const startTag = match[0];
      const componentType = match[1];
      if (!/android:exported\s*=\s*"true"/.test(startTag)) continue;

      const isSelfClosing = /\/\s*>$/.test(startTag);
      const closeTag = `</${componentType}>`;
      const closeIndex = isSelfClosing ? -1 : content.indexOf(closeTag, (match.index ?? 0) + startTag.length);
      const block =
        closeIndex >= 0
          ? content.slice(match.index ?? 0, closeIndex + closeTag.length)
          : startTag;
      const isLauncherActivity =
        (componentType === "activity" || componentType === "activity-alias") &&
        block.includes("android.intent.action.MAIN") &&
        block.includes("android.intent.category.LAUNCHER");

      if (!isLauncherActivity) {
        matches.push(
          candidate(
            "android-exported-component",
            content,
            match.index ?? 0,
            'Android component with android:exported="true"',
          ),
        );
      }
    }

    return matches;
  },
};
