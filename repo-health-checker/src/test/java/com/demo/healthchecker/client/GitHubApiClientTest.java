package com.demo.healthchecker.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("removal")
class GitHubApiClientTest {

    private static final String OWNER = "test-owner";
    private static final String REPO = "test-repo";

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private HttpHeaders httpHeaders;

    private GitHubApiClient client;

    @BeforeEach
    void setUp() {
        client = new GitHubApiClient("fake-token", httpClient);
    }

    // ===================================================================
    // checkFileExists
    // ===================================================================

    @Test
    void checkFileExists_200Response_returnsTrue() throws Exception {
        stubResponse(200, "{}");

        boolean exists = client.checkFileExists(OWNER, REPO, "README.md");

        assertThat(exists).isTrue();
    }

    @Test
    void checkFileExists_404Response_returnsFalse() throws Exception {
        stubResponse(404, "{}");

        boolean exists = client.checkFileExists(OWNER, REPO, "README.md");

        assertThat(exists).isFalse();
    }

    // ===================================================================
    // getRepoInfo
    // ===================================================================

    @Test
    void getRepoInfo_successfulResponse_returnsMap() throws Exception {
        String json = "{\"full_name\":\"test-owner/test-repo\",\"stargazers_count\":10}";
        stubResponse(200, json);

        Map<String, Object> info = client.getRepoInfo(OWNER, REPO);

        assertThat(info)
                .containsEntry("full_name", "test-owner/test-repo")
                .containsEntry("stargazers_count", 10);
    }

    @Test
    void getRepoInfo_nonSuccessResponse_throwsIOException() throws Exception {
        stubResponse(500, "{\"message\":\"Internal Server Error\"}");

        assertThatThrownBy(() -> client.getRepoInfo(OWNER, REPO))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("500");
    }

    // ===================================================================
    // Rate-limit handling (403 + X-RateLimit-Remaining: 0)
    // ===================================================================

    @Test
    void sendRequest_rateLimitExceeded_throwsIOException() throws Exception {
        stubResponseWithHeaders(403, "{\"message\":\"rate limit\"}");
        when(httpHeaders.firstValue("X-RateLimit-Remaining"))
                .thenReturn(Optional.of("0"));
        when(httpHeaders.firstValue("X-RateLimit-Reset"))
                .thenReturn(Optional.of("1700000000"));

        assertThatThrownBy(() -> client.checkFileExists(OWNER, REPO, "README.md"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("rate limit exceeded");
    }

    @Test
    void sendRequest_403WithoutRateLimitHeader_doesNotThrow() throws Exception {
        stubResponseWithHeaders(403, "{}");
        when(httpHeaders.firstValue("X-RateLimit-Remaining"))
                .thenReturn(Optional.empty());

        // checkFileExists just checks status == 200, so returns false for 403
        boolean exists = client.checkFileExists(OWNER, REPO, "README.md");

        assertThat(exists).isFalse();
    }

    // ===================================================================
    // Network timeout / IOException propagation
    // ===================================================================

    @Test
    void sendRequest_networkTimeout_throwsIOException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection timed out"));

        assertThatThrownBy(() -> client.checkFileExists(OWNER, REPO, "README.md"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Connection timed out");
    }

    @Test
    void sendRequest_interruptedException_wrappedInIOException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Thread interrupted"));

        assertThatThrownBy(() -> client.checkFileExists(OWNER, REPO, "README.md"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Request interrupted");
    }

    // ===================================================================
    // getLastCommitDate
    // ===================================================================

    @Test
    void getLastCommitDate_withCommits_returnsDate() throws Exception {
        String json = "[{\"commit\":{\"committer\":{\"date\":\"2026-01-15T10:00:00Z\"}}}]";
        stubResponse(200, json);

        Optional<String> date = client.getLastCommitDate(OWNER, REPO);

        assertThat(date).isPresent().contains("2026-01-15T10:00:00Z");
    }

    @Test
    void getLastCommitDate_emptyCommitList_returnsEmpty() throws Exception {
        stubResponse(200, "[]");

        Optional<String> date = client.getLastCommitDate(OWNER, REPO);

        assertThat(date).isEmpty();
    }

    // ===================================================================
    // getLicenseType
    // ===================================================================

    @Test
    void getLicenseType_validSpdxId_returnsLicense() throws Exception {
        String json = "{\"license\":{\"spdx_id\":\"MIT\"}}";
        stubResponse(200, json);

        Optional<String> license = client.getLicenseType(OWNER, REPO);

        assertThat(license).isPresent().contains("MIT");
    }

    @Test
    void getLicenseType_noassertion_returnsEmpty() throws Exception {
        String json = "{\"license\":{\"spdx_id\":\"NOASSERTION\"}}";
        stubResponse(200, json);

        Optional<String> license = client.getLicenseType(OWNER, REPO);

        assertThat(license).isEmpty();
    }

    @Test
    void getLicenseType_noLicenseKey_returnsEmpty() throws Exception {
        String json = "{\"full_name\":\"owner/repo\"}";
        stubResponse(200, json);

        Optional<String> license = client.getLicenseType(OWNER, REPO);

        assertThat(license).isEmpty();
    }

    // ===================================================================
    // hasDirectory
    // ===================================================================

    @Test
    void hasDirectory_200_returnsTrue() throws Exception {
        stubResponse(200, "[]");

        boolean exists = client.hasDirectory(OWNER, REPO, ".github/workflows");

        assertThat(exists).isTrue();
    }

    @Test
    void hasDirectory_404_returnsFalse() throws Exception {
        stubResponse(404, "{}");

        boolean exists = client.hasDirectory(OWNER, REPO, ".github/workflows");

        assertThat(exists).isFalse();
    }

    // ===================================================================
    // listDirectoryEntries
    // ===================================================================

    @Test
    void listDirectoryEntries_found_returnsEntries() throws Exception {
        String json = "[{\"name\":\"file.txt\",\"type\":\"file\",\"path\":\"src/file.txt\"}]";
        stubResponse(200, json);

        var entries = client.listDirectoryEntries(OWNER, REPO, "src");

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0)).containsEntry("name", "file.txt");
    }

    @Test
    void listDirectoryEntries_404_returnsEmptyList() throws Exception {
        stubResponse(404, "{\"message\":\"Not Found\"}");

        var entries = client.listDirectoryEntries(OWNER, REPO, "nonexistent");

        assertThat(entries).isEmpty();
    }

    // ===================================================================
    // getStarCount (deprecated)
    // ===================================================================

    @Test
    void getStarCount_returnsStargazersCount() throws IOException {
        GitHubApiClient spyClient = spy(new GitHubApiClient("fake-token"));
        doReturn(Map.of("stargazers_count", 42)).when(spyClient).getRepoInfo(OWNER, REPO);

        int stars = spyClient.getStarCount(OWNER, REPO);

        assertThat(stars).isEqualTo(42);
    }

    @Test
    void getStarCount_returnsZeroWhenFieldMissing() throws IOException {
        GitHubApiClient spyClient = spy(new GitHubApiClient("fake-token"));
        doReturn(Map.<String, Object>of()).when(spyClient).getRepoInfo(OWNER, REPO);

        int stars = spyClient.getStarCount(OWNER, REPO);

        assertThat(stars).isZero();
    }

    @Test
    void getStarCount_returnsZeroWhenFieldIsNotANumber() throws IOException {
        GitHubApiClient spyClient = spy(new GitHubApiClient("fake-token"));
        doReturn(Map.<String, Object>of("stargazers_count", "not-a-number"))
                .when(spyClient).getRepoInfo(OWNER, REPO);

        int stars = spyClient.getStarCount(OWNER, REPO);

        assertThat(stars).isZero();
    }

    // ===================================================================
    // getIssueCount
    // ===================================================================

    @Test
    void getIssueCount_returnsTotal() throws Exception {
        String json = "{\"total_count\":42,\"items\":[]}";
        stubResponse(200, json);

        int count = client.getIssueCount(OWNER, REPO, "open");

        assertThat(count).isEqualTo(42);
    }

    @Test
    void getIssueCount_nullState_queriesAll() throws Exception {
        String json = "{\"total_count\":100,\"items\":[]}";
        stubResponse(200, json);

        int count = client.getIssueCount(OWNER, REPO, null);

        assertThat(count).isEqualTo(100);
    }

    // ===================================================================
    // getContributorCount
    // ===================================================================

    @Test
    void getContributorCount_withLinkHeader_parsesLastPage() throws Exception {
        stubResponseWithHeaders(200, "[{}]");
        String linkValue = "<https://api.github.com/repos/o/r/contributors?per_page=1&page=2>; rel=\"next\", "
                + "<https://api.github.com/repos/o/r/contributors?per_page=1&page=57>; rel=\"last\"";
        when(httpHeaders.firstValue("Link")).thenReturn(Optional.of(linkValue));

        int count = client.getContributorCount(OWNER, REPO);

        assertThat(count).isEqualTo(57);
    }

    @Test
    void getContributorCount_noLinkHeader_countsSinglePage() throws Exception {
        stubResponseWithHeaders(200, "[{\"login\":\"alice\"},{\"login\":\"bob\"}]");
        when(httpHeaders.firstValue("Link")).thenReturn(Optional.empty());

        int count = client.getContributorCount(OWNER, REPO);

        assertThat(count).isEqualTo(2);
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    @SuppressWarnings("unchecked")
    private void stubResponse(int statusCode, String body) throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(statusCode);
        lenient().when(httpResponse.body()).thenReturn(body);
    }

    @SuppressWarnings("unchecked")
    private void stubResponseWithHeaders(int statusCode, String body) throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(statusCode);
        lenient().when(httpResponse.body()).thenReturn(body);
        when(httpResponse.headers()).thenReturn(httpHeaders);
    }
}
