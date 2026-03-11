package com.demo.healthchecker.model;

public record AiReadinessReport(
        boolean hasCopilotInstructions,
        boolean hasCustomAgents,
        boolean hasCustomSkills,
        boolean hasPromptFiles,
        boolean hasGitignore,
        boolean hasFolderInstructions,
        int score,
        int maxScore
) {}