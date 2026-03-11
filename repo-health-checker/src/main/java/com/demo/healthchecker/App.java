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

import java.util.concurrent.Callable;

/**
 * CLI entry point for the repository health checker.
 *
 * <p>Parses command-line arguments via picocli, runs both a general health
 * check and an AI-readiness check against a GitHub repository, and prints
 * the combined report to standard output.
 */
@Command(
        name = "repo-health-checker",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Checks the health of a GitHub repository."
)
public class App implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /** GitHub repository in {@code owner/name} format (e.g. {@code octocat/Hello-World}). */
    @Option(names = "--repo", required = true, description = "GitHub repository in owner/name format")
    private String repo;

    /** GitHub personal-access token. Defaults to the {@code GITHUB_TOKEN} environment variable. */
    @Option(names = "--token", description = "GitHub API token (defaults to GITHUB_TOKEN env var)",
            defaultValue = "${GITHUB_TOKEN}")
    private String token;

    /** Output format — either {@code text} (default) or {@code json}. */
    @Option(names = "--format", description = "Output format: text, json", defaultValue = "text")
    private String format;

    /**
     * Executes the health check and prints the report.
     *
     * @return {@code 0} on success, {@code 1} on invalid input or runtime error
     */
    @Override
    public Integer call() {
        String[] parts = repo.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            System.err.println("Error: --repo must be in owner/name format, e.g. octocat/Hello-World");
            return 1;
        }
        String owner = parts[0];
        String name = parts[1];

        try {
            GitHubApiClient client = new GitHubApiClient(token);
            HealthChecker healthChecker = new HealthChecker(client);
            AiReadinessChecker aiChecker = new AiReadinessChecker(client);

            RepoHealthReport healthReport = healthChecker.check(owner, name);
            AiReadinessReport aiReport = aiChecker.check(owner, name);

            ReportFormatter formatter = new ReportFormatter();
            String output = "json".equalsIgnoreCase(format)
                    ? formatter.format(healthReport, aiReport, "json")
                    : formatter.format(healthReport, aiReport, "text");

            System.out.println(output);
            return 0;
        } catch (Exception e) {
            logger.error("Failed to check repository {}/{}: {}", owner, name, e.getMessage(), e);
            System.err.println("Error: Unable to check repository " + owner + "/" + name + " — " + e.getMessage());
            return 1;
        }
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
