package com.demo.healthchecker.checker;

import com.demo.healthchecker.client.GitHubApiClient;
import com.demo.healthchecker.model.AiReadinessReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiReadinessCheckerTest {

    private static final String OWNER = "test-owner";
    private static final String REPO = "test-repo";

    @Mock
    private GitHubApiClient client;

    private AiReadinessChecker checker;

    @BeforeEach
    void setUp() {
        checker = new AiReadinessChecker(client);
    }

    // -----------------------------------------------------------------------
    // 1. Fully AI-ready — all 6 checks pass → score 6/6
    // -----------------------------------------------------------------------
    @Test
    void check_fullyReady_scoreIs6of6() throws IOException {
        when(client.checkFileExists(OWNER, REPO, ".github/copilot-instructions.md"))
                .thenReturn(true);
        when(client.checkFileExists(OWNER, REPO, ".github/copilot/agents.md"))
                .thenReturn(true);
        when(client.hasDirectory(OWNER, REPO, ".github/copilot/skills"))
                .thenReturn(true);
        when(client.checkFileExists(OWNER, REPO, ".gitignore"))
                .thenReturn(true);

        // .github directory contains a .prompt.md file
        when(client.listDirectoryEntries(OWNER, REPO, ".github"))
                .thenReturn(List.of(
                        Map.of("type", "file", "name", "review.prompt.md", "path", ".github/review.prompt.md")
                ));

        // src directory contains a .instructions.md file
        when(client.listDirectoryEntries(OWNER, REPO, "src"))
                .thenReturn(List.of(
                        Map.of("type", "file", "name", "copilot.instructions.md", "path", "src/copilot.instructions.md")
                ));

        AiReadinessReport report = checker.check(OWNER, REPO);

        assertThat(report.score()).isEqualTo(6);
        assertThat(report.maxScore()).isEqualTo(6);
        assertThat(report.hasCopilotInstructions()).isTrue();
        assertThat(report.hasCustomAgents()).isTrue();
        assertThat(report.hasCustomSkills()).isTrue();
        assertThat(report.hasPromptFiles()).isTrue();
        assertThat(report.hasGitignore()).isTrue();
        assertThat(report.hasFolderInstructions()).isTrue();
    }

    // -----------------------------------------------------------------------
    // 2. Completely unready — nothing exists → score 0/6
    // -----------------------------------------------------------------------
    @Test
    void check_completelyUnready_scoreIs0() throws IOException {
        when(client.checkFileExists(anyString(), anyString(), anyString()))
                .thenReturn(false);
        when(client.hasDirectory(OWNER, REPO, ".github/copilot/skills"))
                .thenReturn(false);
        when(client.listDirectoryEntries(OWNER, REPO, ".github"))
                .thenReturn(List.of());
        when(client.listDirectoryEntries(OWNER, REPO, "src"))
                .thenReturn(List.of());

        AiReadinessReport report = checker.check(OWNER, REPO);

        assertThat(report.score()).isZero();
        assertThat(report.maxScore()).isEqualTo(6);
        assertThat(report.hasCopilotInstructions()).isFalse();
        assertThat(report.hasCustomAgents()).isFalse();
        assertThat(report.hasCustomSkills()).isFalse();
        assertThat(report.hasPromptFiles()).isFalse();
        assertThat(report.hasGitignore()).isFalse();
        assertThat(report.hasFolderInstructions()).isFalse();
    }

    // -----------------------------------------------------------------------
    // 3. Partial — only .gitignore exists → score 1/6
    // -----------------------------------------------------------------------
    @Test
    void check_onlyGitignore_scoreIs1() throws IOException {
        when(client.checkFileExists(OWNER, REPO, ".github/copilot-instructions.md"))
                .thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, ".github/copilot/agents.md"))
                .thenReturn(false);
        when(client.hasDirectory(OWNER, REPO, ".github/copilot/skills"))
                .thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, ".gitignore"))
                .thenReturn(true);
        when(client.listDirectoryEntries(OWNER, REPO, ".github"))
                .thenReturn(List.of());
        when(client.listDirectoryEntries(OWNER, REPO, "src"))
                .thenReturn(List.of());

        AiReadinessReport report = checker.check(OWNER, REPO);

        assertThat(report.score()).isEqualTo(1);
        assertThat(report.hasGitignore()).isTrue();
        assertThat(report.hasCopilotInstructions()).isFalse();
        assertThat(report.hasCustomAgents()).isFalse();
        assertThat(report.hasCustomSkills()).isFalse();
        assertThat(report.hasPromptFiles()).isFalse();
        assertThat(report.hasFolderInstructions()).isFalse();
    }

    // -----------------------------------------------------------------------
    // 4. Partial — copilot-instructions + agents only → score 2/6
    // -----------------------------------------------------------------------
    @Test
    void check_copilotInstructionsAndAgentsOnly_scoreIs2() throws IOException {
        when(client.checkFileExists(OWNER, REPO, ".github/copilot-instructions.md"))
                .thenReturn(true);
        when(client.checkFileExists(OWNER, REPO, ".github/copilot/agents.md"))
                .thenReturn(true);
        when(client.hasDirectory(OWNER, REPO, ".github/copilot/skills"))
                .thenReturn(false);
        when(client.checkFileExists(OWNER, REPO, ".gitignore"))
                .thenReturn(false);
        when(client.listDirectoryEntries(OWNER, REPO, ".github"))
                .thenReturn(List.of());
        when(client.listDirectoryEntries(OWNER, REPO, "src"))
                .thenReturn(List.of());

        AiReadinessReport report = checker.check(OWNER, REPO);

        assertThat(report.score()).isEqualTo(2);
        assertThat(report.maxScore()).isEqualTo(6);
        assertThat(report.hasCopilotInstructions()).isTrue();
        assertThat(report.hasCustomAgents()).isTrue();
        assertThat(report.hasCustomSkills()).isFalse();
        assertThat(report.hasPromptFiles()).isFalse();
        assertThat(report.hasGitignore()).isFalse();
        assertThat(report.hasFolderInstructions()).isFalse();
    }

    // -----------------------------------------------------------------------
    // 5. Prompt file found in a subdirectory (recursive scan)
    // -----------------------------------------------------------------------
    @Test
    void check_promptFileInSubdirectory_detected() throws IOException {
        when(client.checkFileExists(anyString(), anyString(), anyString()))
                .thenReturn(false);
        when(client.hasDirectory(OWNER, REPO, ".github/copilot/skills"))
                .thenReturn(false);

        // .github contains a subdirectory "prompts"
        when(client.listDirectoryEntries(OWNER, REPO, ".github"))
                .thenReturn(List.of(
                        Map.of("type", "dir", "name", "prompts", "path", ".github/prompts")
                ));
        // That subdirectory contains the prompt file
        when(client.listDirectoryEntries(OWNER, REPO, ".github/prompts"))
                .thenReturn(List.of(
                        Map.of("type", "file", "name", "fix.prompt.md", "path", ".github/prompts/fix.prompt.md")
                ));

        when(client.listDirectoryEntries(OWNER, REPO, "src"))
                .thenReturn(List.of());

        AiReadinessReport report = checker.check(OWNER, REPO);

        assertThat(report.hasPromptFiles()).isTrue();
        assertThat(report.score()).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // 6. API failure propagates as IOException
    // -----------------------------------------------------------------------
    @Test
    void check_apiFailure_propagatesException() throws IOException {
        when(client.checkFileExists(OWNER, REPO, ".github/copilot-instructions.md"))
                .thenThrow(new IOException("API error for .github/copilot-instructions.md"));

        assertThatThrownBy(() -> checker.check(OWNER, REPO))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("API error");
    }

    // -----------------------------------------------------------------------
    // 7. Directory entries with missing/invalid type are skipped
    // -----------------------------------------------------------------------
    @Test
    void check_malformedDirectoryEntries_skippedGracefully() throws IOException {
        when(client.checkFileExists(anyString(), anyString(), anyString()))
                .thenReturn(false);
        when(client.hasDirectory(OWNER, REPO, ".github/copilot/skills"))
                .thenReturn(false);

        // Entry missing "type" key — should be skipped without error
        when(client.listDirectoryEntries(OWNER, REPO, ".github"))
                .thenReturn(List.of(
                        Map.of("name", "orphan.prompt.md", "path", ".github/orphan.prompt.md")
                ));
        when(client.listDirectoryEntries(OWNER, REPO, "src"))
                .thenReturn(List.of(
                        Map.of("type", 42, "name", "bad.instructions.md", "path", "src/bad.instructions.md")
                ));

        AiReadinessReport report = checker.check(OWNER, REPO);

        assertThat(report.hasPromptFiles()).isFalse();
        assertThat(report.hasFolderInstructions()).isFalse();
        assertThat(report.score()).isZero();
    }
}

