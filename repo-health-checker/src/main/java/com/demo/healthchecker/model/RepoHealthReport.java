package com.demo.healthchecker.model;

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
        boolean hasStars,
        int starCount,
        int healthScore
) {}