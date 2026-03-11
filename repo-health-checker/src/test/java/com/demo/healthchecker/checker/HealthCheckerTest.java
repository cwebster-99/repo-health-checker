package com.demo.healthchecker.checker;

import com.demo.healthchecker.client.GitHubApiClient;
import com.demo.healthchecker.model.RepoHealthReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthCheckerTest {

    private static final String OWNER = "test-owner";
    private static final String REPO = "test-repo";

    @Mock
    private GitHubApiClient client;

    private HealthChecker healthChecker;

    @BeforeEach
    void setUp() {
        healthChecker = new HealthChecker(client);
    }

    // -----------------------------------------------------------------------
    // 1. Perfect repo — all checks pass, score should be 100
    // -----------------------------------------------------------------------
    @Test
    void check_perfectRepo_allChecksPassed_scoreIs100() throws IOException {
        // Files
        when(client.checkFileExists(OWNER, REPO, "README.md")).thenReturn(true);
        when(client.checkFileExists(OWNER, REPO, "LICENSE")).thenReturn(true);
        when(client.getLicenseType(OWNER, REPO)).thenReturn(Optional.of("MIT"));
        when(client.hasDirectory(OWNER, REPO, ".github/workflows")).thenReturn(true);

        // Repo metadata with description and topics
        when(client.getRepoInfo(OWNER, REPO)).thenReturn(Map.of(
                "description", "A well-maintained repository",
                "topics", List.of("java", "testing")
        ));

        // CODEOWNERS — first path matches, short-circuit skips the rest
        when(client.checkFileExists(OWNER, REPO, ".github/CODEOWNERS")).thenReturn(true);

        // Security policy
        when(client.checkFileExists(OWNER, REPO, "SECURITY.md")).thenReturn(true);

        // Issues: excellent ratio (< 25 %)
        when(client.getIssueCount(OWNER, REPO, "open")).thenReturn(2);
        when(client.getIssueCount(OWNER, REPO, null)).thenReturn(100);

        // Recent commit (within 7 days)
        String recentDate = Instant.now().minus(1, ChronoUnit.DAYS).toString();
        when(client.getLastCommitDate(OWNER, REPO)).thenReturn(Optional.of(recentDate));

        RepoHealthReport report = healthChecker.check(OWNER, REPO);

        assertThat(report.healthScore()).isEqualTo(100);
        assertThat(report.hasReadme()).isTrue();
        assertThat(report.hasLicense()).isTrue();
        assertThat(report.licenseType()).isEqualTo("MIT");
        assertThat(report.hasCi()).isTrue();
        assertThat(report.ciType()).isEqualTo("GitHub Actions");
        assertThat(report.hasDescription()).isTrue();
        assertThat(report.hasTopics()).isTrue();
        assertThat(report.hasCodeowners()).isTrue();
        assertThat(report.hasSecurityPolicy()).isTrue();
        assertThat(report.openIssues()).isEqualTo(2);
        assertThat(report.totalIssues()).isEqualTo(100);
        assertThat(report.lastCommitDaysAgo()).isLessThanOrEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // 2. Empty repo — nothing exists
    //    Score is 15 (not 0) because zero total issues yields EXCELLENT (15)
    // -----------------------------------------------------------------------
    @Test
    void check_emptyRepo_nothingExists_scoreIsMinimal() throws IOException {
        when(client.checkFileExists(anyString(), anyString(), anyString())).thenReturn(false);
        when(client.getLicenseType(OWNER, REPO)).thenReturn(Optional.empty());
        when(client.hasDirectory(OWNER, REPO, ".github/workflows")).thenReturn(false);
        when(client.getRepoInfo(OWNER, REPO)).thenReturn(Map.of());
        when(client.getIssueCount(OWNER, REPO, "open")).thenReturn(0);
        when(client.getIssueCount(OWNER, REPO, null)).thenReturn(0);
        when(client.getLastCommitDate(OWNER, REPO)).thenReturn(Optional.empty());

        RepoHealthReport report = healthChecker.check(OWNER, REPO);

        // Only the issue-excellent weight (15) contributes; commit is OLD (0)
        assertThat(report.healthScore()).isEqualTo(15);
        assertThat(report.hasReadme()).isFalse();
        assertThat(report.hasLicense()).isFalse();
        assertThat(report.licenseType()).isNull();
        assertThat(report.hasCi()).isFalse();
        assertThat(report.ciType()).isNull();
        assertThat(report.hasDescription()).isFalse();
        assertThat(report.hasTopics()).isFalse();
        assertThat(report.hasCodeowners()).isFalse();
        assertThat(report.hasSecurityPolicy()).isFalse();
    }

    // -----------------------------------------------------------------------
    // 3. Minimal repo — only README exists
    // -----------------------------------------------------------------------
    @Test
    void check_minimalRepo_onlyReadmeExists() throws IOException {
        when(client.checkFileExists(OWNER, REPO, "README.md")).thenReturn(true);
        when(client.checkFileExists(OWNER, REPO, "LICENSE")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, ".github/CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "docs/CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "SECURITY.md")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, ".github/SECURITY.md")).thenReturn(false);

        when(client.getLicenseType(OWNER, REPO)).thenReturn(Optional.empty());
        when(client.hasDirectory(OWNER, REPO, ".github/workflows")).thenReturn(false);
        when(client.getRepoInfo(OWNER, REPO)).thenReturn(Map.of());
        when(client.getIssueCount(OWNER, REPO, "open")).thenReturn(0);
        when(client.getIssueCount(OWNER, REPO, null)).thenReturn(0);
        when(client.getLastCommitDate(OWNER, REPO)).thenReturn(Optional.empty());

        RepoHealthReport report = healthChecker.check(OWNER, REPO);

        // README (15) + issues excellent (15) = 30
        assertThat(report.healthScore()).isEqualTo(30);
        assertThat(report.hasReadme()).isTrue();
        assertThat(report.hasLicense()).isFalse();
        assertThat(report.licenseType()).isNull();
        assertThat(report.hasCi()).isFalse();
    }

    // -----------------------------------------------------------------------
    // 4. Repo with CI but no license
    // -----------------------------------------------------------------------
    @Test
    void check_repoWithCiButNoLicense() throws IOException {
        when(client.checkFileExists(OWNER, REPO, "README.md")).thenReturn(true);
        when(client.checkFileExists(OWNER, REPO, "LICENSE")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, ".github/CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "docs/CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "SECURITY.md")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, ".github/SECURITY.md")).thenReturn(false);

        when(client.getLicenseType(OWNER, REPO)).thenReturn(Optional.empty());
        when(client.hasDirectory(OWNER, REPO, ".github/workflows")).thenReturn(true);
        when(client.getRepoInfo(OWNER, REPO)).thenReturn(Map.of(
                "description", "Has CI but no license"
        ));
        when(client.getIssueCount(OWNER, REPO, "open")).thenReturn(5);
        when(client.getIssueCount(OWNER, REPO, null)).thenReturn(50);

        String recentDate = Instant.now().minus(3, ChronoUnit.DAYS).toString();
        when(client.getLastCommitDate(OWNER, REPO)).thenReturn(Optional.of(recentDate));

        RepoHealthReport report = healthChecker.check(OWNER, REPO);

        // README(15) + CI(15) + description(5) + issues excellent(15) + commit recent(15) = 65
        assertThat(report.healthScore()).isEqualTo(65);
        assertThat(report.hasCi()).isTrue();
        assertThat(report.ciType()).isEqualTo("GitHub Actions");
        assertThat(report.hasLicense()).isFalse();
        assertThat(report.licenseType()).isNull();
    }

    // -----------------------------------------------------------------------
    // 5. Repo with stale last commit (> 90 days ago)
    // -----------------------------------------------------------------------
    @Test
    void check_repoWithStaleCommit_scoreReflectsOldCommit() throws IOException {
        when(client.checkFileExists(OWNER, REPO, "README.md")).thenReturn(true);
        when(client.checkFileExists(OWNER, REPO, "LICENSE")).thenReturn(true);
        when(client.checkFileExists(OWNER, REPO, ".github/CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "docs/CODEOWNERS")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, "SECURITY.md")).thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, ".github/SECURITY.md")).thenReturn(false);

        when(client.getLicenseType(OWNER, REPO)).thenReturn(Optional.of("Apache-2.0"));
        when(client.hasDirectory(OWNER, REPO, ".github/workflows")).thenReturn(true);
        when(client.getRepoInfo(OWNER, REPO)).thenReturn(Map.of(
                "description", "A project with old commits",
                "topics", List.of("java")
        ));
        when(client.getIssueCount(OWNER, REPO, "open")).thenReturn(1);
        when(client.getIssueCount(OWNER, REPO, null)).thenReturn(10);

        // Stale commit: 120 days ago (> 90 threshold)
        String staleDate = Instant.now().minus(120, ChronoUnit.DAYS).toString();
        when(client.getLastCommitDate(OWNER, REPO)).thenReturn(Optional.of(staleDate));

        RepoHealthReport report = healthChecker.check(OWNER, REPO);

        // README(15) + LICENSE(10) + licenseType(10) + CI(15) + desc(5) + topics(5)
        // + issues excellent(15) + commit OLD(0) = 75
        assertThat(report.healthScore()).isEqualTo(75);
        assertThat(report.lastCommitDaysAgo()).isGreaterThan(90);
    }

    // -----------------------------------------------------------------------
    // 6. Parameterized test — open-issue ratio scoring
    //    Base score is 0 (all boolean checks false, commit OLD) so the total
    //    score equals exactly the issue-ratio score.
    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "open={0}, total={1} → issueScore={2}")
    @CsvSource({
            "0,   0,  15",   // no issues  → EXCELLENT
            "1,  10,  15",   // 10 % ratio → EXCELLENT (< 25 %)
            "2,  10,  15",   // 20 % ratio → EXCELLENT (< 25 %)
            "3,  10,  10",   // 30 % ratio → GOOD      (25 %–49 %)
            "4,  10,  10",   // 40 % ratio → GOOD      (25 %–49 %)
            "5,  10,   5",   // 50 % ratio → FAIR      (50 %–75 %)
            "7,  10,   5",   // 70 % ratio → FAIR      (50 %–75 %)
            "8,  10,   0",   // 80 % ratio → POOR      (> 75 %)
            "10, 10,   0"    // 100 % ratio→ POOR      (> 75 %)
    })
    void check_issueRatioScoring(int openIssues, int totalIssues, int expectedIssueScore) throws IOException {
        stubMinimalRepo();

        when(client.getIssueCount(OWNER, REPO, "open")).thenReturn(openIssues);
        when(client.getIssueCount(OWNER, REPO, null)).thenReturn(totalIssues);

        RepoHealthReport report = healthChecker.check(OWNER, REPO);

        assertThat(report.healthScore()).isEqualTo(expectedIssueScore);
        assertThat(report.openIssues()).isEqualTo(openIssues);
        assertThat(report.totalIssues()).isEqualTo(totalIssues);
    }

    // -----------------------------------------------------------------------
    // 7. Zero total issues — edge case for ratio calculation (no division by zero)
    // -----------------------------------------------------------------------
    @Test
    void check_zeroTotalIssues_getsExcellentIssueScore() throws IOException {
        stubMinimalRepo();

        when(client.getIssueCount(OWNER, REPO, "open")).thenReturn(0);
        when(client.getIssueCount(OWNER, REPO, null)).thenReturn(0);

        RepoHealthReport report = healthChecker.check(OWNER, REPO);

        // Zero total issues should yield EXCELLENT (15) without a division-by-zero error
        assertThat(report.healthScore()).isEqualTo(15);
        assertThat(report.openIssues()).isZero();
        assertThat(report.totalIssues()).isZero();
    }

    // -----------------------------------------------------------------------
    // Helper: stubs a minimal repo where every check returns false / empty
    // so the base score from boolean checks + commit recency is 0.
    // -----------------------------------------------------------------------
    private void stubMinimalRepo() throws IOException {
        when(client.checkFileExists(anyString(), anyString(), anyString())).thenReturn(false);
        when(client.getLicenseType(OWNER, REPO)).thenReturn(Optional.empty());
        when(client.hasDirectory(OWNER, REPO, ".github/workflows")).thenReturn(false);
        when(client.getRepoInfo(OWNER, REPO)).thenReturn(Map.of());
        when(client.getLastCommitDate(OWNER, REPO)).thenReturn(Optional.empty());
    }
}

