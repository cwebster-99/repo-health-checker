package com.demo.healthchecker.formatter;

import com.demo.healthchecker.model.AiReadinessReport;
import com.demo.healthchecker.model.RepoHealthReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Formats {@link RepoHealthReport} and {@link AiReadinessReport} into
 * human-readable text or JSON output.
 */
public class ReportFormatter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Formats the given reports according to the requested format.
     *
     * @param health the repository health report
     * @param ai     the AI-readiness report
     * @param format {@code "json"} for JSON output; any other value produces plain text
     * @return the formatted report string
     * @throws IOException if JSON serialization fails
     */
    public String format(RepoHealthReport health, AiReadinessReport ai, String format) throws IOException {
        return switch (format.toLowerCase()) {
            case "json" -> formatJson(health, ai);
            default -> formatText(health, ai);
        };
    }

    private String formatText(RepoHealthReport h, AiReadinessReport ai) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== Repository Health Report ===\n");
        sb.append(String.format("  README:          %s%n", bool(h.hasReadme())));
        sb.append(String.format("  License:         %s%n", bool(h.hasLicense())));
        sb.append(String.format("  License type:    %s%n", h.licenseType() != null ? h.licenseType() : "N/A"));
        sb.append(String.format("  CI:              %s%s%n", bool(h.hasCi()),
                h.ciType() != null ? " (" + h.ciType() + ")" : ""));
        sb.append(String.format("  Description:     %s%n", bool(h.hasDescription())));
        sb.append(String.format("  Topics:          %s%n", bool(h.hasTopics())));
        sb.append(String.format("  CODEOWNERS:      %s%n", bool(h.hasCodeowners())));
        sb.append(String.format("  Security policy: %s%n", bool(h.hasSecurityPolicy())));
        sb.append(String.format("  Open issues:     %d / %d%n", h.openIssues(), h.totalIssues()));
        sb.append(String.format("  Last commit:     %d days ago%n", h.lastCommitDaysAgo()));
        sb.append(String.format("  Health score:    %d / 100%n", h.healthScore()));

        sb.append("\n=== AI Readiness Report ===\n");
        sb.append(String.format("  Copilot instructions:   %s%n", bool(ai.hasCopilotInstructions())));
        sb.append(String.format("  Custom agents:          %s%n", bool(ai.hasCustomAgents())));
        sb.append(String.format("  Custom skills:          %s%n", bool(ai.hasCustomSkills())));
        sb.append(String.format("  Prompt files:           %s%n", bool(ai.hasPromptFiles())));
        sb.append(String.format("  .gitignore:             %s%n", bool(ai.hasGitignore())));
        sb.append(String.format("  Folder instructions:    %s%n", bool(ai.hasFolderInstructions())));
        sb.append(String.format("  AI readiness score:     %d / %d%n", ai.score(), ai.maxScore()));

        return sb.toString();
    }

    private String formatJson(RepoHealthReport h, AiReadinessReport ai) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode healthNode = root.putObject("repoHealth");
        healthNode.put("hasReadme", h.hasReadme());
        healthNode.put("hasLicense", h.hasLicense());
        healthNode.put("licenseType", h.licenseType());
        healthNode.put("hasCi", h.hasCi());
        healthNode.put("ciType", h.ciType());
        healthNode.put("hasDescription", h.hasDescription());
        healthNode.put("hasTopics", h.hasTopics());
        healthNode.put("hasCodeowners", h.hasCodeowners());
        healthNode.put("hasSecurityPolicy", h.hasSecurityPolicy());
        healthNode.put("openIssues", h.openIssues());
        healthNode.put("totalIssues", h.totalIssues());
        healthNode.put("lastCommitDaysAgo", h.lastCommitDaysAgo());
        healthNode.put("healthScore", h.healthScore());

        ObjectNode aiNode = root.putObject("aiReadiness");
        aiNode.put("hasCopilotInstructions", ai.hasCopilotInstructions());
        aiNode.put("hasCustomAgents", ai.hasCustomAgents());
        aiNode.put("hasCustomSkills", ai.hasCustomSkills());
        aiNode.put("hasPromptFiles", ai.hasPromptFiles());
        aiNode.put("hasGitignore", ai.hasGitignore());
        aiNode.put("hasFolderInstructions", ai.hasFolderInstructions());
        aiNode.put("score", ai.score());
        aiNode.put("maxScore", ai.maxScore());

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static String bool(boolean value) {
        return value ? "yes" : "no";
    }
}
