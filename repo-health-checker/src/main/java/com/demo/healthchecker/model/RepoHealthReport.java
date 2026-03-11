package com.demo.healthchecker.model;

/**
 * Immutable data carrier for the results of a repository health check.
 *
 * @param hasReadme         whether a {@code README.md} file exists
 * @param hasLicense        whether a {@code LICENSE} file exists
 * @param licenseType       SPDX identifier of the detected license, or {@code null}
 * @param hasCi             whether a CI configuration was found
 * @param ciType            the CI system name (e.g. {@code "GitHub Actions"}), or {@code null}
 * @param hasDescription    whether the repository has a description
 * @param hasTopics         whether the repository has at least one topic
 * @param hasCodeowners     whether a {@code CODEOWNERS} file exists
 * @param hasSecurityPolicy whether a {@code SECURITY.md} file exists
 * @param openIssues        number of open issues (excluding pull requests)
 * @param totalIssues       total number of issues (excluding pull requests)
 * @param lastCommitDaysAgo number of days since the most recent commit
 * @param healthScore       overall weighted health score (0–100)
 */
public record RepoHealthReport(
        boolean hasReadme,
        boolean hasLicense,
        String licenseType,
        boolean hasCi,
        String ciType,
        boolean hasDescription,
        boolean hasTopics,
        boolean hasCodeowners,
        boolean hasSecurityPolicy,
        int openIssues,
        int totalIssues,
        long lastCommitDaysAgo,
        int healthScore
) {}