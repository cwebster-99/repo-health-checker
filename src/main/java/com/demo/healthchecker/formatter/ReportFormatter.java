package com.demo.healthchecker.formatter;

import com.demo.healthchecker.model.AiReadinessReport;
import com.demo.healthchecker.model.RepoHealthReport;

import java.util.List;

/**
 * Formats {@link RepoHealthReport} and {@link AiReadinessReport} into
 * a Markdown report document.
 */
public class ReportFormatter {

    private static final int RATING_EXCELLENT_THRESHOLD = 80;
    private static final int RATING_GOOD_THRESHOLD = 60;
    private static final int RATING_FAIR_THRESHOLD = 40;

    private final RecommendationGenerator recommendationGenerator = new RecommendationGenerator();

    /**
     * Formats the given reports as a Markdown document.
     *
     * @param health the repository health report
     * @param ai     the AI-readiness report
     * @return the formatted Markdown report string
     */
    public String format(RepoHealthReport health, AiReadinessReport ai) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Repository Health Report\n\n");
        sb.append(String.format("> **Health Score: %d / 100 (%s)** | **AI Readiness: %d / %d**%n%n",
                health.healthScore(), rating(health.healthScore()), ai.score(), ai.maxScore()));

        // --- Repository Health section ---
        sb.append("## Repository Health\n\n");
        sb.append("| Indicator | Status |\n");
        sb.append("|---|---|\n");
        sb.append(String.format("| README | %s |%n", badge(health.hasReadme())));
        sb.append(String.format("| License | %s |%n",
                health.hasLicense()
                        ? "✅ " + (health.licenseType() != null ? health.licenseType() : "Yes")
                        : "❌ None"));
        sb.append(String.format("| CI | %s |%n",
                health.hasCi()
                        ? "✅ " + (health.ciType() != null ? health.ciType() : "Yes")
                        : "❌ None"));
        sb.append(String.format("| Description | %s |%n", badge(health.hasDescription())));
        sb.append(String.format("| Topics | %s |%n", badge(health.hasTopics())));
        sb.append(String.format("| CODEOWNERS | %s |%n", badge(health.hasCodeowners())));
        sb.append(String.format("| Security Policy | %s |%n", badge(health.hasSecurityPolicy())));
        sb.append(String.format("| Stars | %s |%n",
                health.hasStars() ? "⭐ " + health.starCount() : "0"));
        sb.append(String.format("| Open Issues | %d / %d |%n", health.openIssues(), health.totalIssues()));
        sb.append(String.format("| Last Commit | %d days ago |%n", health.lastCommitDaysAgo()));
        sb.append(String.format("| **Health Score** | **%d / 100** |%n", health.healthScore()));
        sb.append(String.format("| **Rating** | **%s** |%n", rating(health.healthScore())));

        // --- AI Readiness section ---
        sb.append("\n## AI Readiness for GitHub Copilot\n\n");
        sb.append("| Indicator | Status |\n");
        sb.append("|---|---|\n");
        sb.append(String.format("| Copilot Instructions | %s |%n", badge(ai.hasCopilotInstructions())));
        sb.append(String.format("| Custom Agents | %s |%n", badge(ai.hasCustomAgents())));
        sb.append(String.format("| Custom Skills | %s |%n", badge(ai.hasCustomSkills())));
        sb.append(String.format("| Prompt Files | %s |%n", badge(ai.hasPromptFiles())));
        sb.append(String.format("| .gitignore | %s |%n", badge(ai.hasGitignore())));
        sb.append(String.format("| Folder Instructions | %s |%n", badge(ai.hasFolderInstructions())));
        sb.append(String.format("| **AI Readiness Score** | **%d / %d** |%n", ai.score(), ai.maxScore()));

        // --- Recommendations section ---
        List<String> recommendations = recommendationGenerator.generate(health, ai);
        sb.append("\n## Recommendations\n\n");
        if (recommendations.isEmpty()) {
            sb.append("✅ All checks passed — no recommendations.\n");
        } else {
            for (String rec : recommendations) {
                sb.append(String.format("- %s%n", rec));
            }
        }

        return sb.toString();
    }

    /**
     * Returns a human-readable rating label for the given health score.
     *
     * @param score the numeric health score
     * @return one of "Excellent", "Good", "Fair", or "Needs Work"
     */
    public static String rating(int score) {
        if (score >= RATING_EXCELLENT_THRESHOLD) {
            return "Excellent";
        }
        if (score >= RATING_GOOD_THRESHOLD) {
            return "Good";
        }
        if (score >= RATING_FAIR_THRESHOLD) {
            return "Fair";
        }
        return "Needs Work";
    }

    private static String badge(boolean value) {
        return value ? "✅" : "❌";
    }
}
