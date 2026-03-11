package com.demo.healthchecker.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GitHubApiClient {

    private static final Logger logger = LoggerFactory.getLogger(GitHubApiClient.class);
    private static final String BASE_URL = "https://api.github.com";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;

    public GitHubApiClient(String token) {
        this.token = token;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks whether a file (or directory) exists at the given path in the repository.
     */
    public boolean checkFileExists(String owner, String repo, String path) throws IOException {
        String url = String.format("%s/repos/%s/%s/contents/%s", BASE_URL, owner, repo, path);
        logger.debug("Checking file exists: {}", url);

        HttpResponse<String> response = sendRequest(url);
        return response.statusCode() == 200;
    }

    /**
     * Returns general repository information as a Map.
     */
    public Map<String, Object> getRepoInfo(String owner, String repo) throws IOException {
        String url = String.format("%s/repos/%s/%s", BASE_URL, owner, repo);
        logger.debug("Fetching repo info: {}", url);

        HttpResponse<String> response = sendRequest(url);
        checkSuccessful(response, url);

        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    /**
     * Returns the number of open issues for the repository.
     */
    public int getOpenIssueCount(String owner, String repo) throws IOException {
        Map<String, Object> repoInfo = getRepoInfo(owner, repo);
        Object count = repoInfo.get("open_issues_count");
        if (count instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    /**
     * Returns the date of the last commit, or empty if there are no commits.
     */
    @SuppressWarnings("unchecked")
    public Optional<String> getLastCommitDate(String owner, String repo) throws IOException {
        String url = String.format("%s/repos/%s/%s/commits?per_page=1", BASE_URL, owner, repo);
        logger.debug("Fetching last commit date: {}", url);

        HttpResponse<String> response = sendRequest(url);
        checkSuccessful(response, url);

        List<Map<String, Object>> commits = objectMapper.readValue(
                response.body(), new TypeReference<>() {});

        if (commits == null || commits.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> firstCommit = commits.getFirst();
        Map<String, Object> commitObj = (Map<String, Object>) firstCommit.get("commit");
        if (commitObj == null) {
            return Optional.empty();
        }
        Map<String, Object> committer = (Map<String, Object>) commitObj.get("committer");
        if (committer == null) {
            return Optional.empty();
        }
        Object date = committer.get("date");
        return Optional.ofNullable(date != null ? date.toString() : null);
    }

    /**
     * Returns the SPDX license identifier for the repository, or empty if none.
     */
    public Optional<String> getLicenseType(String owner, String repo) throws IOException {
        Map<String, Object> repoInfo = getRepoInfo(owner, repo);
        Object licenseObj = repoInfo.get("license");
        if (licenseObj instanceof Map<?, ?> licenseMap) {
            Object spdxId = licenseMap.get("spdx_id");
            if (spdxId != null && !"NOASSERTION".equals(spdxId.toString())) {
                return Optional.of(spdxId.toString());
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether a directory exists at the given path in the repository.
     */
    public boolean hasDirectory(String owner, String repo, String path) throws IOException {
        String url = String.format("%s/repos/%s/%s/contents/%s", BASE_URL, owner, repo, path);
        logger.debug("Checking directory exists: {}", url);

        HttpResponse<String> response = sendRequest(url);
        return response.statusCode() == 200;
    }

    /**
     * Returns directory entries for a repository path.
     * Each entry mirrors GitHub's contents API fields (for example: name, path, type).
     */
    public List<Map<String, Object>> listDirectoryEntries(String owner, String repo, String path) throws IOException {
        String url = String.format("%s/repos/%s/%s/contents/%s", BASE_URL, owner, repo, path);
        logger.debug("Listing directory entries: {}", url);

        HttpResponse<String> response = sendRequest(url);
        if (response.statusCode() == 404) {
            return List.of();
        }
        checkSuccessful(response, url);

        List<Map<String, Object>> entries = objectMapper.readValue(response.body(), new TypeReference<>() {});
        return entries != null ? entries : new ArrayList<>();
    }

    /**
     * Returns the count of issues (excluding pull requests) matching the given state.
     * @param state "open", "closed", or null for all issues
     */
    public int getIssueCount(String owner, String repo, String state) throws IOException {
        String query = String.format("repo:%s/%s type:issue", owner, repo);
        if (state != null) {
            query += " state:" + state;
        }
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s/search/issues?q=%s&per_page=1", BASE_URL, encodedQuery);
        logger.debug("Searching issue count: {}", url);

        HttpResponse<String> response = sendRequest(url);
        checkSuccessful(response, url);

        Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {});
        Object totalCount = result.get("total_count");
        return (totalCount instanceof Number n) ? n.intValue() : 0;
    }

    // ---- Internal helpers ----

    private HttpRequest buildRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "repo-health-checker")
                .GET();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return builder.build();
    }

    private HttpResponse<String> sendRequest(String url) throws IOException {
        try {
            HttpRequest request = buildRequest(url);
            logger.info("Sending request: {} {}", request.method(), url);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("Response: {} for {}", response.statusCode(), url);
            checkRateLimit(response);

            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted: " + url, e);
        }
    }

    private void checkRateLimit(HttpResponse<String> response) throws IOException {
        if (response.statusCode() == 403) {
            String remaining = response.headers()
                    .firstValue("X-RateLimit-Remaining")
                    .orElse(null);
            if ("0".equals(remaining)) {
                String resetHeader = response.headers()
                        .firstValue("X-RateLimit-Reset")
                        .orElse("unknown");
                String message = String.format(
                        "GitHub API rate limit exceeded. Resets at epoch %s", resetHeader);
                logger.error(message);
                throw new IOException(message);
            }
        }
    }

    private void checkSuccessful(HttpResponse<String> response, String url) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = String.format(
                    "GitHub API request failed: %d for %s", response.statusCode(), url);
            logger.error(message);
            throw new IOException(message);
        }
    }
}





