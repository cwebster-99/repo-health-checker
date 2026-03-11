package com.demo.healthchecker.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("removal")
class GitHubApiClientTest {

    private static final String OWNER = "test-owner";
    private static final String REPO = "test-repo";

    // -----------------------------------------------------------------------
    // getStarCount — deprecated convenience method
    // -----------------------------------------------------------------------

    @Test
    void getStarCount_returnsStargazersCount() throws IOException {
        GitHubApiClient client = spy(new GitHubApiClient("fake-token"));
        doReturn(Map.of("stargazers_count", 42)).when(client).getRepoInfo(OWNER, REPO);

        int stars = client.getStarCount(OWNER, REPO);

        assertThat(stars).isEqualTo(42);
    }

    @Test
    void getStarCount_returnsZeroWhenFieldMissing() throws IOException {
        GitHubApiClient client = spy(new GitHubApiClient("fake-token"));
        doReturn(Map.<String, Object>of()).when(client).getRepoInfo(OWNER, REPO);

        int stars = client.getStarCount(OWNER, REPO);

        assertThat(stars).isZero();
    }

    @Test
    void getStarCount_returnsZeroWhenFieldIsNotANumber() throws IOException {
        GitHubApiClient client = spy(new GitHubApiClient("fake-token"));
        doReturn(Map.<String, Object>of("stargazers_count", "not-a-number"))
                .when(client).getRepoInfo(OWNER, REPO);

        int stars = client.getStarCount(OWNER, REPO);

        assertThat(stars).isZero();
    }
}


