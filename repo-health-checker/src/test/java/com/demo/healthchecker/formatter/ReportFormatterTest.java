package com.demo.healthchecker.formatter;

import com.demo.healthchecker.model.AiReadinessReport;
import com.demo.healthchecker.model.RepoHealthReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFormatterTest {

    private ReportFormatter formatter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        formatter = new ReportFormatter();
    }

    // ===================================================================
    // Text output — contains all expected sections and fields
    // ===================================================================

    @Test
    void formatText_containsAllHealthSections() throws IOException {
        RepoHealthReport health = new RepoHealthReport(
                true, true, "MIT", true, "GitHub Actions",
                true, true, true, true,
                5, 100, 3,
                true, 42,
                95
        );
        AiReadinessReport ai = new AiReadinessReport(
                true, false, false, true, true, false, 3, 6
        );

        String output = formatter.format(health, ai, "text");

        assertThat(output).contains("=== Repository Health Report ===");
        assertThat(output).contains("README:");
        assertThat(output).contains("License:");
        assertThat(output).contains("License type:    MIT");
        assertThat(output).contains("CI:");
        assertThat(output).contains("GitHub Actions");
        assertThat(output).contains("Description:");
        assertThat(output).contains("Topics:");
        assertThat(output).contains("CODEOWNERS:");
        assertThat(output).contains("Security policy:");
        assertThat(output).contains("✓ 42 stars");
        assertThat(output).contains("Open issues:     5 / 100");
        assertThat(output).contains("Last commit:     3 days ago");
        assertThat(output).contains("Health score:    95 / 100");
        assertThat(output).contains("Rating:          Excellent");
        assertThat(output).contains("=== AI Readiness Report ===");
        assertThat(output).contains("Copilot instructions:");
        assertThat(output).contains("AI readiness score:     3 / 6");
    }

    @Test
    void formatText_noStars_showsNoStarsMarker() throws IOException {
        RepoHealthReport health = new RepoHealthReport(
                false, false, null, false, null,
                false, false, false, false,
                0, 0, 0,
                false, 0,
                10
        );
        AiReadinessReport ai = new AiReadinessReport(
                false, false, false, false, false, false, 0, 6
        );

        String output = formatter.format(health, ai, "text");

        assertThat(output).contains("✗ No stars");
        assertThat(output).contains("License type:    N/A");
    }

    @Test
    void formatText_defaultFormatProducesText() throws IOException {
        RepoHealthReport health = buildHealthReport(50);
        AiReadinessReport ai = buildAiReport(0);

        String textOutput = formatter.format(health, ai, "text");
        String defaultOutput = formatter.format(health, ai, "anything");

        assertThat(defaultOutput).isEqualTo(textOutput);
    }

    // ===================================================================
    // JSON output — valid JSON containing all fields
    // ===================================================================

    @Test
    void formatJson_isValidJsonWithAllFields() throws IOException {
        RepoHealthReport health = new RepoHealthReport(
                true, true, "Apache-2.0", true, "GitHub Actions",
                true, true, true, true,
                10, 200, 7,
                true, 100,
                90
        );
        AiReadinessReport ai = new AiReadinessReport(
                true, true, true, true, true, true, 6, 6
        );

        String json = formatter.format(health, ai, "json");

        // Must parse as valid JSON
        JsonNode root = objectMapper.readTree(json);

        // Repo health fields
        JsonNode healthNode = root.get("repoHealth");
        assertThat(healthNode).isNotNull();
        assertThat(healthNode.get("hasReadme").asBoolean()).isTrue();
        assertThat(healthNode.get("hasLicense").asBoolean()).isTrue();
        assertThat(healthNode.get("licenseType").asText()).isEqualTo("Apache-2.0");
        assertThat(healthNode.get("hasCi").asBoolean()).isTrue();
        assertThat(healthNode.get("ciType").asText()).isEqualTo("GitHub Actions");
        assertThat(healthNode.get("hasDescription").asBoolean()).isTrue();
        assertThat(healthNode.get("hasTopics").asBoolean()).isTrue();
        assertThat(healthNode.get("hasCodeowners").asBoolean()).isTrue();
        assertThat(healthNode.get("hasSecurityPolicy").asBoolean()).isTrue();
        assertThat(healthNode.get("hasStars").asBoolean()).isTrue();
        assertThat(healthNode.get("starCount").asInt()).isEqualTo(100);
        assertThat(healthNode.get("openIssues").asInt()).isEqualTo(10);
        assertThat(healthNode.get("totalIssues").asInt()).isEqualTo(200);
        assertThat(healthNode.get("lastCommitDaysAgo").asLong()).isEqualTo(7);
        assertThat(healthNode.get("healthScore").asInt()).isEqualTo(90);
        assertThat(healthNode.get("rating").asText()).isEqualTo("Excellent");

        // AI readiness fields
        JsonNode aiNode = root.get("aiReadiness");
        assertThat(aiNode).isNotNull();
        assertThat(aiNode.get("hasCopilotInstructions").asBoolean()).isTrue();
        assertThat(aiNode.get("hasCustomAgents").asBoolean()).isTrue();
        assertThat(aiNode.get("hasCustomSkills").asBoolean()).isTrue();
        assertThat(aiNode.get("hasPromptFiles").asBoolean()).isTrue();
        assertThat(aiNode.get("hasGitignore").asBoolean()).isTrue();
        assertThat(aiNode.get("hasFolderInstructions").asBoolean()).isTrue();
        assertThat(aiNode.get("score").asInt()).isEqualTo(6);
        assertThat(aiNode.get("maxScore").asInt()).isEqualTo(6);
    }

    @Test
    void formatJson_nullLicenseType_rendersAsNullNode() throws IOException {
        RepoHealthReport health = new RepoHealthReport(
                false, false, null, false, null,
                false, false, false, false,
                0, 0, 0,
                false, 0,
                0
        );
        AiReadinessReport ai = buildAiReport(0);

        String json = formatter.format(health, ai, "json");
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("repoHealth").get("licenseType").isNull()).isTrue();
        assertThat(root.get("repoHealth").get("ciType").isNull()).isTrue();
    }

    // ===================================================================
    // Rating boundary tests
    // ===================================================================

    @ParameterizedTest(name = "score={0} → rating={1}")
    @CsvSource({
            "80,  Excellent",
            "81,  Excellent",
            "100, Excellent",
            "79,  Good",
            "60,  Good",
            "70,  Good",
            "59,  Fair",
            "40,  Fair",
            "50,  Fair",
            "39,  Needs Work",
            "0,   Needs Work",
            "20,  Needs Work"
    })
    void rating_returnsCorrectLabel(int score, String expectedRating) {
        assertThat(ReportFormatter.rating(score)).isEqualTo(expectedRating);
    }

    @Test
    void formatText_score79_ratingIsGood() throws IOException {
        RepoHealthReport health = buildHealthReport(79);
        AiReadinessReport ai = buildAiReport(0);

        String output = formatter.format(health, ai, "text");

        assertThat(output).contains("Rating:          Good");
    }

    @Test
    void formatText_score80_ratingIsExcellent() throws IOException {
        RepoHealthReport health = buildHealthReport(80);
        AiReadinessReport ai = buildAiReport(0);

        String output = formatter.format(health, ai, "text");

        assertThat(output).contains("Rating:          Excellent");
    }

    @Test
    void formatText_score39_ratingIsNeedsWork() throws IOException {
        RepoHealthReport health = buildHealthReport(39);
        AiReadinessReport ai = buildAiReport(0);

        String output = formatter.format(health, ai, "text");

        assertThat(output).contains("Rating:          Needs Work");
    }

    @Test
    void formatText_score40_ratingIsFair() throws IOException {
        RepoHealthReport health = buildHealthReport(40);
        AiReadinessReport ai = buildAiReport(0);

        String output = formatter.format(health, ai, "text");

        assertThat(output).contains("Rating:          Fair");
    }

    @Test
    void formatJson_ratingFieldReflectsScore() throws IOException {
        RepoHealthReport health = buildHealthReport(79);
        AiReadinessReport ai = buildAiReport(0);

        String json = formatter.format(health, ai, "json");
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("repoHealth").get("rating").asText()).isEqualTo("Good");
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    private static RepoHealthReport buildHealthReport(int score) {
        return new RepoHealthReport(
                false, false, null, false, null,
                false, false, false, false,
                0, 0, 0,
                false, 0,
                score
        );
    }

    private static AiReadinessReport buildAiReport(int score) {
        return new AiReadinessReport(false, false, false, false, false, false, score, 6);
    }
}

