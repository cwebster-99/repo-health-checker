package com.demo.healthchecker.formatter;

import com.demo.healthchecker.model.AiReadinessReport;
import com.demo.healthchecker.model.RepoHealthReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFormatterTest {

    private ReportFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ReportFormatter();
    }

    // ===================================================================
    // Markdown output — contains all expected sections and fields
    // ===================================================================

    @Test
    void format_containsAllHealthSections() {
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

        String output = formatter.format(health, ai);

        assertThat(output).contains("# Repository Health Report");
        assertThat(output).contains("## Repository Health");
        assertThat(output).contains("| Indicator | Status |");
        assertThat(output).contains("| README | ✅ |");
        assertThat(output).contains("| License | ✅ MIT |");
        assertThat(output).contains("| CI | ✅ GitHub Actions |");
        assertThat(output).contains("| Description | ✅ |");
        assertThat(output).contains("| Topics | ✅ |");
        assertThat(output).contains("| CODEOWNERS | ✅ |");
        assertThat(output).contains("| Security Policy | ✅ |");
        assertThat(output).contains("| Stars | ⭐ 42 |");
        assertThat(output).contains("| Open Issues | 5 / 100 |");
        assertThat(output).contains("| Last Commit | 3 days ago |");
        assertThat(output).contains("| **Health Score** | **95 / 100** |");
        assertThat(output).contains("| **Rating** | **Excellent** |");
        assertThat(output).contains("## AI Readiness for GitHub Copilot");
        assertThat(output).contains("| Copilot Instructions | ✅ |");
        assertThat(output).contains("| **AI Readiness Score** | **3 / 6** |");
    }

    @Test
    void format_summaryLine_containsScores() {
        RepoHealthReport health = buildHealthReport(75);
        AiReadinessReport ai = buildAiReport(4);

        String output = formatter.format(health, ai);

        assertThat(output).contains("**Health Score: 75 / 100 (Good)**");
        assertThat(output).contains("**AI Readiness: 4 / 6**");
    }

    @Test
    void format_noStars_showsZero() {
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

        String output = formatter.format(health, ai);

        assertThat(output).contains("| Stars | 0 |");
        assertThat(output).contains("| License | ❌ None |");
        assertThat(output).contains("| CI | ❌ None |");
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
    void format_score79_ratingIsGood() {
        RepoHealthReport health = buildHealthReport(79);
        AiReadinessReport ai = buildAiReport(0);

        String output = formatter.format(health, ai);

        assertThat(output).contains("| **Rating** | **Good** |");
    }

    @Test
    void format_score80_ratingIsExcellent() {
        RepoHealthReport health = buildHealthReport(80);
        AiReadinessReport ai = buildAiReport(0);

        String output = formatter.format(health, ai);

        assertThat(output).contains("| **Rating** | **Excellent** |");
    }

    @Test
    void format_score39_ratingIsNeedsWork() {
        RepoHealthReport health = buildHealthReport(39);
        AiReadinessReport ai = buildAiReport(0);

        String output = formatter.format(health, ai);

        assertThat(output).contains("| **Rating** | **Needs Work** |");
    }

    @Test
    void format_score40_ratingIsFair() {
        RepoHealthReport health = buildHealthReport(40);
        AiReadinessReport ai = buildAiReport(0);

        String output = formatter.format(health, ai);

        assertThat(output).contains("| **Rating** | **Fair** |");
    }

    // ===================================================================
    // Recommendations section
    // ===================================================================

    @Test
    void format_failedChecks_containsRecommendationsSection() {
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

        String output = formatter.format(health, ai);

        assertThat(output).contains("## Recommendations");
        assertThat(output).contains("README.md");
        assertThat(output).contains("LICENSE");
        assertThat(output).contains(".github/workflows");
    }

    @Test
    void format_allChecksPassing_showsNoRecommendationsMessage() {
        RepoHealthReport health = new RepoHealthReport(
                true, true, "MIT", true, "GitHub Actions",
                true, true, true, true,
                2, 20, 3,
                true, 10,
                100
        );
        AiReadinessReport ai = new AiReadinessReport(
                true, true, true, true, true, true, 6, 6
        );

        String output = formatter.format(health, ai);

        assertThat(output).contains("## Recommendations");
        assertThat(output).contains("All checks passed");
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

