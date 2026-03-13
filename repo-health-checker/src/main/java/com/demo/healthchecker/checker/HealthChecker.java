package com.demo.healthchecker.checker;

import com.demo.healthchecker.client.GitHubApiClient;
import com.demo.healthchecker.model.RepoHealthReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluates the overall health of a GitHub repository.
 *
 * <p>Checks for the presence of common project artifacts (README, LICENSE,
 * CI configuration, CODEOWNERS, security policy, etc.), analyses issue
 * activity and commit recency, then produces a weighted score out of 100.
 */
public class HealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(HealthChecker.class);

    // Boolean check weights
    private static final int WEIGHT_README = 15;
    private static final int WEIGHT_LICENSE = 10;
    private static final int WEIGHT_LICENSE_TYPE = 10;
    private static final int WEIGHT_CI = 15;
    private static final int WEIGHT_DESCRIPTION = 5;
    private static final int WEIGHT_TOPICS = 5;
    private static final int WEIGHT_CODEOWNERS = 5;
    private static final int WEIGHT_SECURITY = 5;
    private static final int WEIGHT_STARS = 5;

    // Issue ratio scoring
    private static final int WEIGHT_ISSUES_EXCELLENT = 15;
    private static final int WEIGHT_ISSUES_GOOD = 10;
    private static final int WEIGHT_ISSUES_FAIR = 5;
    private static final int WEIGHT_ISSUES_POOR = 0;

    // Commit recency scoring
    private static final int WEIGHT_COMMIT_RECENT = 15;
    private static final int WEIGHT_COMMIT_MODERATE = 10;
    private static final int WEIGHT_COMMIT_STALE = 5;
    private static final int WEIGHT_COMMIT_OLD = 0;

    private final GitHubApiClient client;

    /**
     * Creates a new health checker backed by the given API client.
     *
     * @param client the GitHub API client used for all repository queries
     */
    public HealthChecker(GitHubApiClient client) {
        this.client = client;
    }

    /**
     * Runs the full health check against the specified repository.
     *
     * @param owner repository owner (user or organization)
     * @param repo  repository name
     * @return a {@link RepoHealthReport} summarizing the results
     * @throws IOException if any GitHub API call fails
     */
    public RepoHealthReport check(String owner, String repo) throws IOException {
        logger.info("Starting health check for {}/{}", owner, repo);
        int score = 0;

        boolean hasReadme = checkReadme(owner, repo);
        if (hasReadme) score += WEIGHT_README;

        boolean hasLicense = checkLicense(owner, repo);
        if (hasLicense) score += WEIGHT_LICENSE;

        String licenseType = checkLicenseType(owner, repo);
        if (licenseType != null) score += WEIGHT_LICENSE_TYPE;

        boolean hasCi = checkCi(owner, repo);
        String ciType = hasCi ? "GitHub Actions" : null;
        if (hasCi) score += WEIGHT_CI;

        Map<String, Object> repoInfo = client.getRepoInfo(owner, repo);

        boolean hasDescription = checkDescription(repoInfo);
        if (hasDescription) score += WEIGHT_DESCRIPTION;

        boolean hasTopics = checkTopics(repoInfo);
        if (hasTopics) score += WEIGHT_TOPICS;

        boolean hasCodeowners = checkCodeowners(owner, repo);
        if (hasCodeowners) score += WEIGHT_CODEOWNERS;

        boolean hasSecurityPolicy = checkSecurityPolicy(owner, repo);
        if (hasSecurityPolicy) score += WEIGHT_SECURITY;

        Object starCountObj = repoInfo.get("stargazers_count");
        int starCount = (starCountObj instanceof Number n) ? n.intValue() : 0;
        boolean hasStars = starCount > 0;
        score += scoreStars(starCount);

        int openIssues = client.getIssueCount(owner, repo, "open");
        int totalIssues = client.getIssueCount(owner, repo, null);
        score += scoreIssues(openIssues, totalIssues);

        Optional<String> lastCommitDate = client.getLastCommitDate(owner, repo);
        long lastCommitDaysAgo = calculateDaysAgo(lastCommitDate);
        score += scoreCommitRecency(lastCommitDaysAgo);

        logger.info("Health check complete for {}/{}: score={}/100", owner, repo, score);

        return new RepoHealthReport(
                hasReadme,
                hasLicense,
                licenseType,
                hasCi,
                ciType,
                hasDescription,
                hasTopics,
                hasCodeowners,
                hasSecurityPolicy,
                openIssues,
                totalIssues,
                lastCommitDaysAgo,
                hasStars,
                starCount,
                score
        );
    }

    private boolean checkReadme(String owner, String repo) throws IOException {
        logger.info("Checking README.md...");
        boolean exists = client.checkFileExists(owner, repo, "README.md");
        logger.info("README.md exists: {}", exists);
        return exists;
    }

    private boolean checkLicense(String owner, String repo) throws IOException {
        logger.info("Checking LICENSE...");
        boolean exists = client.checkFileExists(owner, repo, "LICENSE");
        logger.info("LICENSE exists: {}", exists);
        return exists;
    }

    private String checkLicenseType(String owner, String repo) throws IOException {
        logger.info("Checking license type...");
        String type = client.getLicenseType(owner, repo).orElse(null);
        logger.info("License type: {}", type);
        return type;
    }

    private boolean checkCi(String owner, String repo) throws IOException {
        logger.info("Checking CI configuration...");
        boolean exists = client.hasDirectory(owner, repo, ".github/workflows");
        logger.info("GitHub Actions CI: {}", exists);
        return exists;
    }

    private boolean checkDescription(Map<String, Object> repoInfo) {
        Object description = repoInfo.get("description");
        boolean has = description != null && !description.toString().isBlank();
        logger.info("Has description: {}", has);
        return has;
    }

    private boolean checkTopics(Map<String, Object> repoInfo) {
        Object topics = repoInfo.get("topics");
        boolean has = topics instanceof List<?> list && !list.isEmpty();
        logger.info("Has topics: {}", has);
        return has;
    }

    private boolean checkCodeowners(String owner, String repo) throws IOException {
        logger.info("Checking CODEOWNERS...");
        boolean exists = client.checkFileExists(owner, repo, ".github/CODEOWNERS")
                || client.checkFileExists(owner, repo, "CODEOWNERS")
                || client.checkFileExists(owner, repo, "docs/CODEOWNERS");
        logger.info("CODEOWNERS exists: {}", exists);
        return exists;
    }

    private boolean checkSecurityPolicy(String owner, String repo) throws IOException {
        logger.info("Checking security policy...");
        boolean exists = client.checkFileExists(owner, repo, "SECURITY.md")
                || client.checkFileExists(owner, repo, ".github/SECURITY.md");
        logger.info("Security policy exists: {}", exists);
        return exists;
    }

    private int scoreStars(int starCount) {
        logger.info("Star count: {}", starCount);
        return starCount > 0 ? WEIGHT_STARS : 0;
    }

    private int scoreIssues(int openIssues, int totalIssues) {
        // Calculate ratio of open to total issues
        if (totalIssues == 0) return WEIGHT_ISSUES_EXCELLENT;
        double ratio = (double) openIssues / totalIssues;
        logger.info("Issue ratio: {}/{} = {}", openIssues, totalIssues, String.format("%.2f", ratio));
        if (ratio < 0.25) return WEIGHT_ISSUES_EXCELLENT;
        if (ratio < 0.50) return WEIGHT_ISSUES_GOOD;
        if (ratio <= 0.75) return WEIGHT_ISSUES_FAIR;
        return WEIGHT_ISSUES_POOR;
    }

    private long calculateDaysAgo(Optional<String> lastCommitDate) {
        if (lastCommitDate.isEmpty()) {
            return Long.MAX_VALUE;
        }
        try {
            Instant commitInstant = Instant.parse(lastCommitDate.get());
            return Duration.between(commitInstant, Instant.now()).toDays();
        } catch (Exception e) {
            logger.warn("Failed to parse commit date: {}", lastCommitDate.get(), e);
            return Long.MAX_VALUE;
        }
    }

    private int scoreCommitRecency(long daysAgo) {
        logger.info("Last commit was {} days ago", daysAgo);
        if (daysAgo <= 7) return WEIGHT_COMMIT_RECENT;
        if (daysAgo <= 30) return WEIGHT_COMMIT_MODERATE;
        if (daysAgo <= 90) return WEIGHT_COMMIT_STALE;
        return WEIGHT_COMMIT_OLD;
    }
}

