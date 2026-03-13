package com.demo.healthchecker.formatter;

import com.demo.healthchecker.model.AiReadinessReport;
import com.demo.healthchecker.model.RepoHealthReport;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates actionable recommendations based on failed repository health and AI-readiness checks.
 *
 * <p>Each failing check produces one recommendation that explains the problem and gives a
 * concrete step the developer can take to address it.
 */
public class RecommendationGenerator {

    private static final double ISSUE_RATIO_EXCELLENT = 0.25;
    private static final double ISSUE_RATIO_GOOD = 0.50;
    private static final double ISSUE_RATIO_FAIR = 0.75;
    private static final long COMMIT_DAYS_RECENT = 7;
    private static final long COMMIT_DAYS_MODERATE = 30;
    private static final long COMMIT_DAYS_STALE = 90;

    /**
     * Returns an ordered list of actionable recommendations for all failed checks.
     * Returns an empty list when every check passes.
     *
     * @param health the repository health report
     * @param ai     the AI-readiness report
     * @return recommendations for failed checks, in check order
     */
    public List<String> generate(RepoHealthReport health, AiReadinessReport ai) {
        List<String> recommendations = new ArrayList<>();
        addHealthRecommendations(health, recommendations);
        addAiRecommendations(ai, recommendations);
        return recommendations;
    }

    private void addHealthRecommendations(RepoHealthReport h, List<String> recs) {
        if (!h.hasReadme()) {
            recs.add("Add a README.md to the repository root describing the project purpose, "
                    + "setup steps, and usage. Create the file directly or run `gh repo edit`.");
        }
        if (!h.hasLicense()) {
            recs.add("Add a LICENSE file to clarify how others may use your code. "
                    + "On GitHub, go to Insights → Community → License, "
                    + "or run `gh repo edit --license MIT`.");
        }
        if (h.licenseType() == null) {
            recs.add("Use a recognized SPDX license identifier (e.g., MIT, Apache-2.0) "
                    + "so GitHub can detect your license automatically.");
        }
        if (!h.hasCi()) {
            recs.add("Add a CI workflow under `.github/workflows/`. Browse starter templates "
                    + "at github.com/<owner>/<repo>/actions/new or copy one from a similar project.");
        }
        if (!h.hasDescription()) {
            recs.add("Add a repository description so visitors understand the project at a glance. "
                    + "Run `gh repo edit --description 'Your description here'`.");
        }
        if (!h.hasTopics()) {
            recs.add("Add topics to improve discoverability. "
                    + "Run `gh repo edit --add-topic <topic>` "
                    + "or set them via the repository homepage under About.");
        }
        if (!h.hasCodeowners()) {
            recs.add("Create a CODEOWNERS file at `.github/CODEOWNERS` to define code ownership "
                    + "and automatically assign reviewers to pull requests.");
        }
        if (!h.hasSecurityPolicy()) {
            recs.add("Create a SECURITY.md file at `.github/SECURITY.md` describing "
                    + "how to responsibly report vulnerabilities in your project.");
        }
        if (!h.hasStars()) {
            recs.add("Promote your repository through documentation, blog posts, "
                    + "or community engagement to gain stars and increase visibility.");
        }
        addIssueRecommendations(h, recs);
        addCommitRecommendations(h, recs);
    }

    private void addIssueRecommendations(RepoHealthReport h, List<String> recs) {
        if (h.totalIssues() == 0) {
            return;
        }
        double ratio = (double) h.openIssues() / h.totalIssues();
        if (ratio > ISSUE_RATIO_FAIR) {
            recs.add(String.format(
                    "Triage open issues — %d of %d issues are open (%.0f%%). "
                    + "Close resolved issues or label them as 'wontfix' to reduce "
                    + "your open-to-total ratio below 75%%.",
                    h.openIssues(), h.totalIssues(), ratio * 100));
        } else if (ratio >= ISSUE_RATIO_GOOD) {
            recs.add(String.format(
                    "Reduce open issues — %d of %d issues are open (%.0f%%). "
                    + "Aim for below 50%% open to improve your issue management score.",
                    h.openIssues(), h.totalIssues(), ratio * 100));
        } else if (ratio >= ISSUE_RATIO_EXCELLENT) {
            recs.add(String.format(
                    "Your issue ratio is good, but reducing open issues below 25%% of total "
                    + "(%d of %d open) will earn maximum issue management points.",
                    h.openIssues(), h.totalIssues()));
        }
    }

    private void addCommitRecommendations(RepoHealthReport h, List<String> recs) {
        long days = h.lastCommitDaysAgo();
        if (days == Long.MAX_VALUE) {
            recs.add("No commits were found in this repository. "
                    + "Push an initial commit to start tracking activity.");
        } else if (days > COMMIT_DAYS_STALE) {
            recs.add(String.format(
                    "The last commit was %d days ago. Make regular commits to keep the repository "
                    + "active — activity within the last 90 days is required to score commit recency points.",
                    days));
        } else if (days > COMMIT_DAYS_MODERATE) {
            recs.add(String.format(
                    "The last commit was %d days ago. Committing within the last 30 days earns more "
                    + "points — consider merging pending pull requests or updating documentation.",
                    days));
        } else if (days > COMMIT_DAYS_RECENT) {
            recs.add(String.format(
                    "The last commit was %d days ago. "
                    + "Committing within the last 7 days earns maximum commit recency points.",
                    days));
        }
    }

    private void addAiRecommendations(AiReadinessReport ai, List<String> recs) {
        if (!ai.hasCopilotInstructions()) {
            recs.add("Create `.github/copilot-instructions.md` with your project's conventions, "
                    + "tech stack, and coding standards to guide Copilot suggestions.");
        }
        if (!ai.hasCustomAgents()) {
            recs.add("Create custom agent definition files (`.agent.md`) under `.github/copilot/` "
                    + "to define specialized Copilot assistants tailored to your workflows.");
        }
        if (!ai.hasCustomSkills()) {
            recs.add("Add custom skill definitions under `.github/copilot/skills/` "
                    + "to extend Copilot with project-specific capabilities.");
        }
        if (!ai.hasPromptFiles()) {
            recs.add("Add `.prompt.md` files under `.github/` "
                    + "to save and reuse prompt templates for common development tasks.");
        }
        if (!ai.hasGitignore()) {
            recs.add("Create a `.gitignore` file to prevent build artifacts, secrets, "
                    + "and local config from being committed. Visit gitignore.io for templates.");
        }
        if (!ai.hasFolderInstructions()) {
            recs.add("Add `.instructions.md` files inside `src/` subdirectories "
                    + "to give Copilot context about each module's purpose and conventions.");
        }
    }
}
