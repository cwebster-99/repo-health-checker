package com.demo.healthchecker;

import com.demo.healthchecker.checker.AiReadinessChecker;
import com.demo.healthchecker.checker.HealthChecker;
import com.demo.healthchecker.client.GitHubApiClient;
import com.demo.healthchecker.formatter.ReportFormatter;
import com.demo.healthchecker.model.AiReadinessReport;
import com.demo.healthchecker.model.RepoHealthReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * CLI entry point for the repository health checker.
 *
 * <p>Parses command-line arguments via picocli, runs both a general health
 * check and an AI-readiness check against a GitHub repository, and writes
 * the combined report to a Markdown file.
 */
@Command(
        name = "repo-health-checker",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Checks the health of a GitHub repository."
)
public class App implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final int EXPECTED_REPO_PARTS = 2;

    /** GitHub repository in {@code owner/name} format (e.g. {@code octocat/Hello-World}). */
    @Option(names = "--repo", required = true, description = "GitHub repository in owner/name format")
    private String repo;

    /** GitHub personal-access token. Defaults to the {@code GITHUB_TOKEN} environment variable. */
    @Option(names = "--token", description = "GitHub API token (defaults to GITHUB_TOKEN env var)",
            defaultValue = "${GITHUB_TOKEN}")
    private String token;

    /**
     * Executes the health check and writes the Markdown report to a file.
     *
     * @return {@code 0} on success, {@code 1} on invalid input or runtime error
     */
    @Override
    public Integer call() {
        String[] parts = repo.split("/", EXPECTED_REPO_PARTS);
        if (parts.length != EXPECTED_REPO_PARTS || parts[0].isBlank() || parts[1].isBlank()) {
            logger.error("Invalid --repo value '{}': must be in owner/name format, e.g. octocat/Hello-World", repo);
            return 1;
        }
        String owner = parts[0];
        String name = parts[1];

        try {
            String resolvedToken = resolveToken();
            GitHubApiClient client = new GitHubApiClient(resolvedToken);
            HealthChecker healthChecker = new HealthChecker(client);
            AiReadinessChecker aiChecker = new AiReadinessChecker(client);

            RepoHealthReport healthReport = healthChecker.check(owner, name);
            AiReadinessReport aiReport = aiChecker.check(owner, name);

            ReportFormatter formatter = new ReportFormatter();
            String output = formatter.format(healthReport, aiReport);

            String fileName = owner + "-" + name + "-health-report.md";
            Path outputPath = Path.of(fileName);
            Files.writeString(outputPath, output);
            logger.info("Report written to {}", outputPath.toAbsolutePath());
            return 0;
        } catch (Exception e) {
            logger.error("Failed to check repository {}/{}: {}", owner, name, e.getMessage(), e);
            return 1;
        }
    }

    /**
     * Resolves the GitHub token by checking (in order):
     * 1. The {@code --token} CLI option / {@code GITHUB_TOKEN} env var
     * 2. The {@code gh auth token} command (GitHub CLI)
     *
     * @return the resolved token, or {@code null} if none found
     */
    private String resolveToken() {
        if (token != null && !token.isBlank()) {
            return token;
        }
        try {
            Process process = new ProcessBuilder("gh", "auth", "token")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                int exit = process.waitFor();
                if (exit == 0 && line != null && !line.isBlank()) {
                    logger.debug("Using token from gh CLI");
                    return line.trim();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve token from gh CLI: {}", e.getMessage());
        }
        logger.warn("No GitHub token found. Requests will be unauthenticated (rate-limited to 60/hr).");
        return null;
    }

    /**
     * Program entry point.
     *
     * @param args command-line arguments forwarded to picocli
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
