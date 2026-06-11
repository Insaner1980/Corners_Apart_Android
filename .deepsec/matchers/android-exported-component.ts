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
    const componentRegex =
      /<(activity|activity-alias|service|receiver|provider)\b[\s\S]*?android:exported\s*=\s*"true"[\s\S]*?(?:<\/\1>|\/>)/g;

    for (const match of content.matchAll(componentRegex)) {
      const block = match[0];
      const componentType = match[1];
      const isLauncherActivity =
        componentType === "activity" &&
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
