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

/**
 * Lightweight client for the GitHub REST API (v3).
 *
 * <p>Provides methods to query repository metadata, file existence, issue
 * counts, commit history, and license information. All network calls use
 * the Java {@link HttpClient} and responses are parsed with Jackson.
 */
public class GitHubApiClient {

    private static final Logger logger = LoggerFactory.getLogger(GitHubApiClient.class);
    private static final String BASE_URL = "https://api.github.com";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;

    /**
     * Creates a new client, optionally authenticated with a personal-access token.
     *
     * @param token GitHub API token, or {@code null} for unauthenticated requests
     */
    public GitHubApiClient(String token) {
        this.token = token;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Package-private constructor for testing with a custom {@link HttpClient}.
     *
     * @param token      GitHub API token
     * @param httpClient the HTTP client to use
     */
    GitHubApiClient(String token, HttpClient httpClient) {
        this.token = token;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks whether a file (or directory) exists at the given path in the repository.
     *
     * @param owner repository owner (user or organization)
     * @param repo  repository name
     * @param path  path inside the repository (e.g. {@code "README.md"})
     * @return {@code true} if the path exists, {@code false} otherwise
     * @throws IOException if the API call fails
     */
    public boolean checkFileExists(String owner, String repo, String path) throws IOException {
        String url = String.format("%s/repos/%s/%s/contents/%s", BASE_URL, owner, repo, path);
        logger.debug("Checking file exists: {}", url);

        HttpResponse<String> response = sendRequest(url);
        return response.statusCode() == 200;
    }

    /**
     * Returns general repository information as a Map.
     *
     * @param owner repository owner
     * @param repo  repository name
     * @return a map of repository metadata fields
     * @throws IOException if the API call fails
     */
    public Map<String, Object> getRepoInfo(String owner, String repo) throws IOException {
        String url = String.format("%s/repos/%s/%s", BASE_URL, owner, repo);
        logger.debug("Fetching repo info: {}", url);

        HttpResponse<String> response = sendRequest(url);
        checkSuccessful(response, url);

        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    /**
     * Returns the date of the last commit, or empty if there are no commits.
     *
     * @param owner repository owner
     * @param repo  repository name
     * @return an {@link Optional} containing the ISO-8601 date string, or empty
     * @throws IOException if the API call fails
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

        Map<String, Object> firstCommit = commits.get(0);
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
     *
     * @param owner repository owner
     * @param repo  repository name
     * @return an {@link Optional} containing the SPDX identifier (e.g. {@code "MIT"}), or empty
     * @throws IOException if the API call fails
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
     *
     * @param owner repository owner
     * @param repo  repository name
     * @param path  directory path inside the repository
     * @return {@code true} if the directory exists, {@code false} otherwise
     * @throws IOException if the API call fails
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
     *
     * @param owner repository owner
     * @param repo  repository name
     * @param path  directory path to list
     * @return a list of entry maps, or an empty list if the path does not exist
     * @throws IOException if the API call fails for reasons other than a 404
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
     *
     * @param owner repository owner
     * @param repo  repository name
     * @param state {@code "open"}, {@code "closed"}, or {@code null} for all issues
     * @return the matching issue count
     * @throws IOException if the API call fails
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





