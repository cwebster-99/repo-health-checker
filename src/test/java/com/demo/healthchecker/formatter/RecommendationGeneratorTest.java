package com.demo.healthchecker.formatter;

import com.demo.healthchecker.model.AiReadinessReport;
import com.demo.healthchecker.model.RepoHealthReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationGeneratorTest {

    private RecommendationGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RecommendationGenerator();
    }

    // ===================================================================
    // No recommendations when all checks pass
    // ===================================================================

    @Test
    void generate_allChecksPassing_returnsEmptyList() {
        List<String> recs = generator.generate(allPassingHealth(), allPassingAi());

        assertThat(recs).isEmpty();
    }

    // ===================================================================
    // Health check — boolean failures
    // ===================================================================

    @Test
    void generate_missingReadme_includesReadmeRecommendation() {
        RepoHealthReport health = health(false, true, "MIT", true, true, true, true, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("README.md");
    }

    @Test
    void generate_missingLicenseFile_includesLicenseRecommendation() {
        RepoHealthReport health = health(true, false, "MIT", true, true, true, true, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("LICENSE");
    }

    @Test
    void generate_missingLicenseAndType_includesBothRecommendations() {
        RepoHealthReport health = health(true, false, null, true, true, true, true, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(2);
        assertThat(recs.get(0)).containsIgnoringCase("LICENSE");
        assertThat(recs.get(1)).containsIgnoringCase("SPDX");
    }

    @Test
    void generate_nullLicenseTypeOnly_includesSpdxRecommendation() {
        RepoHealthReport health = health(true, true, null, true, true, true, true, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("SPDX");
    }

    @Test
    void generate_missingCi_includesCiRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", false, true, true, true, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase(".github/workflows");
    }

    @Test
    void generate_missingDescription_includesDescriptionRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, false, true, true, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("description");
    }

    @Test
    void generate_missingTopics_includesTopicsRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, false, true, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("topic");
    }

    @Test
    void generate_missingCodeowners_includesCodeownersRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, false, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("CODEOWNERS");
    }

    @Test
    void generate_missingSecurityPolicy_includesSecurityRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, false, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("SECURITY.md");
    }

    @Test
    void generate_noStars_includesStarsRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 2, 20, 3, false, 0);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("stars");
    }

    // ===================================================================
    // Issue ratio recommendations
    // ===================================================================

    @Test
    void generate_poorIssueRatio_includesTriageRecommendation() {
        // 80% open → poor (> 75%)
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 80, 100, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("triage");
        assertThat(recs.get(0)).contains("80 of 100");
    }

    @Test
    void generate_fairIssueRatio_includesReduceOpenIssues() {
        // 60% open → fair (50–75%)
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 60, 100, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("reduce");
        assertThat(recs.get(0)).contains("60 of 100");
    }

    @Test
    void generate_goodIssueRatio_includesReduceBelow25Percent() {
        // 30% open → good (25–50%)
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 30, 100, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).contains("25%");
    }

    @Test
    void generate_excellentIssueRatio_noIssueRecommendation() {
        // 10% open → excellent (< 25%)
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 10, 100, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).isEmpty();
    }

    @Test
    void generate_zeroTotalIssues_noIssueRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 0, 0, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).isEmpty();
    }

    // ===================================================================
    // Commit recency recommendations
    // ===================================================================

    @Test
    void generate_noCommitsFound_includesPushCommitRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 2, 20, Long.MAX_VALUE, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("no commits");
    }

    @Test
    void generate_commitOver90Days_includesOldCommitRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 2, 20, 120, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).contains("120 days ago");
        assertThat(recs.get(0)).containsIgnoringCase("90 days");
    }

    @Test
    void generate_commitBetween31And90Days_includesModerateCommitRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 2, 20, 45, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).contains("45 days ago");
        assertThat(recs.get(0)).containsIgnoringCase("30 days");
    }

    @Test
    void generate_commitBetween8And30Days_includesRecentCommitRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 2, 20, 15, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).contains("15 days ago");
        assertThat(recs.get(0)).containsIgnoringCase("7 days");
    }

    @Test
    void generate_commitWithin7Days_noCommitRecommendation() {
        RepoHealthReport health = health(true, true, "MIT", true, true, true, true, true, 2, 20, 3, true, 10);

        List<String> recs = generator.generate(health, allPassingAi());

        assertThat(recs).isEmpty();
    }

    // ===================================================================
    // AI readiness recommendations
    // ===================================================================

    @Test
    void generate_missingCopilotInstructions_includesInstructionsRecommendation() {
        AiReadinessReport ai = ai(false, true, true, true, true, true);

        List<String> recs = generator.generate(allPassingHealth(), ai);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("copilot-instructions.md");
    }

    @Test
    void generate_missingCustomAgents_includesAgentsRecommendation() {
        AiReadinessReport ai = ai(true, false, true, true, true, true);

        List<String> recs = generator.generate(allPassingHealth(), ai);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase(".agent.md");
    }

    @Test
    void generate_missingCustomSkills_includesSkillsRecommendation() {
        AiReadinessReport ai = ai(true, true, false, true, true, true);

        List<String> recs = generator.generate(allPassingHealth(), ai);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase("copilot/skills");
    }

    @Test
    void generate_missingPromptFiles_includesPromptFilesRecommendation() {
        AiReadinessReport ai = ai(true, true, true, false, true, true);

        List<String> recs = generator.generate(allPassingHealth(), ai);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase(".prompt.md");
    }

    @Test
    void generate_missingGitignore_includesGitignoreRecommendation() {
        AiReadinessReport ai = ai(true, true, true, true, false, true);

        List<String> recs = generator.generate(allPassingHealth(), ai);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase(".gitignore");
    }

    @Test
    void generate_missingFolderInstructions_includesFolderInstructionsRecommendation() {
        AiReadinessReport ai = ai(true, true, true, true, true, false);

        List<String> recs = generator.generate(allPassingHealth(), ai);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0)).containsIgnoringCase(".instructions.md");
    }

    // ===================================================================
    // Multiple failures produce multiple recommendations
    // ===================================================================

    @Test
    void generate_allChecksFailing_returnsRecommendationForEachFailure() {
        RepoHealthReport health = new RepoHealthReport(
                false, false, null, false, null,
                false, false, false, false,
                90, 100, 200,
                false, 0,
                0
        );
        AiReadinessReport ai = new AiReadinessReport(false, false, false, false, false, false, 0, 6);

        List<String> recs = generator.generate(health, ai);

        // 9 boolean health checks + 1 issue (>75%) + 1 commit (>90 days) + 6 AI checks = 17
        assertThat(recs).hasSize(17);
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    private static RepoHealthReport allPassingHealth() {
        return new RepoHealthReport(
                true, true, "MIT", true, "GitHub Actions",
                true, true, true, true,
                2, 20, 3,
                true, 10,
                100
        );
    }

    private static AiReadinessReport allPassingAi() {
        return new AiReadinessReport(true, true, true, true, true, true, 6, 6);
    }

    /**
     * Builds a {@link RepoHealthReport} with a fixed passing score, varying the check fields.
     * Parameters: hasReadme, hasLicense, licenseType, hasCi, hasDescription, hasTopics,
     * hasCodeowners, hasSecurityPolicy, openIssues, totalIssues, lastCommitDaysAgo, hasStars, starCount.
     */
    private static RepoHealthReport health(
            boolean hasReadme, boolean hasLicense, String licenseType, boolean hasCi,
            boolean hasDescription, boolean hasTopics, boolean hasCodeowners, boolean hasSecurityPolicy,
            int openIssues, int totalIssues, long lastCommitDaysAgo, boolean hasStars, int starCount) {
        return new RepoHealthReport(
                hasReadme, hasLicense, licenseType,
                hasCi, hasCi ? "GitHub Actions" : null,
                hasDescription, hasTopics, hasCodeowners, hasSecurityPolicy,
                openIssues, totalIssues, lastCommitDaysAgo,
                hasStars, starCount,
                100
        );
    }

    /**
     * Builds an {@link AiReadinessReport} from the six boolean indicators.
     */
    private static AiReadinessReport ai(
            boolean copilotInstructions, boolean customAgents, boolean customSkills,
            boolean promptFiles, boolean gitignore, boolean folderInstructions) {
        int score = (copilotInstructions ? 1 : 0) + (customAgents ? 1 : 0) + (customSkills ? 1 : 0)
                + (promptFiles ? 1 : 0) + (gitignore ? 1 : 0) + (folderInstructions ? 1 : 0);
        return new AiReadinessReport(
                copilotInstructions, customAgents, customSkills, promptFiles, gitignore, folderInstructions,
                score, 6
        );
    }
}
