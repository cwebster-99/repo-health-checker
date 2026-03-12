# GitHub Repository Health Checker

[![CI](https://github.com/Sweekriti91/repo-health-checker/actions/workflows/ci.yml/badge.svg)](https://github.com/Sweekriti91/repo-health-checker/actions/workflows/ci.yml)

A CLI tool that analyzes GitHub repositories and generates a health score report with AI readiness assessment for GitHub Copilot.

## Requirements

- Java 21+
- Maven

## Building

```bash
mvn clean package
```

## Usage

```bash
java -jar target/healthchecker-1.0-SNAPSHOT.jar \
  --repo owner/repo-name \
  --token ghp_yourGitHubToken \
  --format text
```

| Flag       | Required | Description                                              |
|------------|----------|----------------------------------------------------------|
| `--repo`   | Yes      | GitHub repository in `owner/name` format                 |
| `--token`  | No       | GitHub API token (defaults to `GITHUB_TOKEN` env var)    |
| `--format` | No       | Output format: `text` (default) or `json`                |

## Example Output

```
=== Repository Health Report ===
  README:          yes
  License:         yes
  License type:    MIT
  CI:              yes (GitHub Actions)
  Description:     yes
  Topics:          yes
  CODEOWNERS:      no
  Security policy: no
  Open issues:     3 / 12
  Last commit:     2 days ago
  Health score:    82 / 100

=== AI Readiness Report ===
  Copilot instructions:   yes
  Custom agents:          no
  Custom skills:          no
  Prompt files:           yes
  .gitignore:             yes
  Folder instructions:    no
  AI readiness score:     3 / 6
```

## Built with GitHub Copilot

This project was built entirely with GitHub Copilot across multiple interaction modes:

| Feature Used | Where | What It Did |
|---|---|---|
| 🏗️ Agent Mode | Project setup, API client, wiring | Created pom.xml, directory structure, full classes from natural language prompts |
| ⌨️ Inline Completions | Domain models | Tab-completed 2 Java records with 21 fields total |
| 🔌 MCP (GitHub) | API client design | Queried live repos to understand API structure before generating client code |
| 💬 Chat | Scoring logic, AI readiness checker | Generated business logic from requirements descriptions |
| 🧪 /tests + Generate Tests | Test suite | Generated JUnit 5 tests with Mockito mocks and parameterized cases |
| 🔧 /fix | Bug fix | Found divide-by-zero from stack trace, suggested exact fix |
| 📝 /doc | Javadoc (via coding agent) | Generated documentation for all public APIs |
| 📋 Custom Instructions | Enforced team conventions | SLF4J logging, Optional returns, Java records |
| 🤖 Coding Agent (IntelliJ) | Report Formatter | Created class + tests from a prompt inside the IDE |
| 🤖 Coding Agent (Issue) | Javadoc | Assigned a GitHub issue, received working PR |
| ✍️ Commit Messages | All commits | Auto-generated Conventional Commit format messages |

