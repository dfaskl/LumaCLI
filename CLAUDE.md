# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LumaCLI is a Java Agent CLI product benchmarked against Claude Code. It supports three execution modes: ReAct (default), Plan-and-Execute (`/plan`), and Multi-Agent (`/team`). The project has evolved through 23 phases from a basic agent loop to a full TUI product with MCP, RAG, HITL, skills, and multi-model support.

## Build & Test Commands

```bash
mvn clean package                       # Build fat JAR (tests skipped by default)
java -jar target/lumacli-1.0-SNAPSHOT.jar  # Run CLI

mvn test -Pquick                        # Quick regression (excludes slow MCP/network tests)
mvn test -Pphase16-smoke                # TUI/inline renderer smoke tests
mvn test -Dtest=XxxTest -DskipTests=false  # Run a single test class
mvn test -DskipTests=false              # Full regression
```

No linting tools are configured. Tests use JUnit 5 + Mockito + OkHttp MockWebServer.

## Architecture

**Entry point:** `com.lumacli.cli.Main` (src/main/java/com/lumacli/cli/Main.java)

**Three execution paths** share ToolRegistry / MemoryManager / SnapshotService:

| Path | Entry Class | Trigger |
|------|-------------|---------|
| ReAct | `agent/Agent.java` | Default |
| Plan-and-Execute | `agent/PlanExecuteAgent.java` | `/plan` |
| Multi-Agent | `agent/AgentOrchestrator.java` | `/team` |

**Core built-in tools (11):** `read_file`, `write_file`, `list_dir`, `glob_files`, `grep_code`, `execute_command`, `create_project`, `search_code`, `web_search`, `web_fetch`, `revert_turn`. All tool execution goes through `ToolRegistry` + `executeTools()` (parallel, max 4 concurrent).

**MCP dynamic tools:** `mcp__{server}__{tool}`. Config merges user-level `~/.lumacli/mcp.json` with project-level `.lumacli/mcp.json`. Startup waits max 8s for MCP servers before proceeding.

**Key packages:**

| Package | Purpose |
|---------|---------|
| `llm/` | Multi-provider LLM clients (GLM, DeepSeek, Step, Kimi, FreeLLMAPI, Agnes, Xfyun). All extend `AbstractOpenAiCompatibleClient`. Factory: `LlmClientFactory` |
| `memory/` | Short-term + long-term memory, conversation history compaction, token budgeting |
| `rag/` | Semantic code search via SQLite vector store. `search_code` is RAG辅助; precise code lookup uses `glob_files`/`grep_code`/`read_file` |
| `mcp/` | Full MCP client stack: transport (stdio/HTTP), JSON-RPC, protocol, resources, @mention expansion |
| `hitl/` | Human-in-the-loop approval. Intercept order: HitlToolRegistry → ToolRegistry → PathGuard/CommandGuard |
| `policy/` | PathGuard (path confinement), CommandGuard (shell blacklist), AuditLog |
| `prompt/` | Layered system prompt assembly from `src/main/resources/prompts/`. Supports `~/.lumacli/prompts/` and `.lumacli/prompts/` overrides |
| `render/` | Three renderer modes: inline (default, JLine 4 based), lanterna (full-screen TUI), plain (fallback) |
| `snapshot/` | Side-Git snapshots via JGit (pure Java, no system git dependency) |
| `skill/` | Skill registry with bundled web-access skill. Skills loaded via `load_skill` tool |
| `runtime/` | Background tasks (`DurableTaskManager`) + HTTP Runtime API (`RuntimeApiServer`) |
| `wechat/` | WeChat iLink channel integration (non-interactive, policy-restricted) |
| `cli/` | JLine 4 terminal interaction, command parsing, tab completion, syntax highlighting |
| `browser/` | Chrome DevTools Protocol integration with CDP session reuse |

**Prompt layering:** `PromptAssembler` builds system prompts from `base.md` + mode-specific (`agent.md`, `plan.md`, `planner.md`, `team-*.md`) + approval policy + context management + personality + project memory (`PAI.md`).

**Environment:** Copy `.env.example` to `.env` and set at least one API key. All config via env vars or system properties.

## Key Behavioral Constraints

- **Code exploration:** Use `glob_files` to find candidates, `grep_code` for precise symbol/string lookup (prefers ripgrep, falls back to Java scan), `read_file` for targeted reads. `search_code` is RAG辅助 for fuzzy/natural-language queries, not for precise code定位.
- **Memory:** Long-term memory only via `/save` or explicit user request. `PAI.md` is for stable team-shared project rules, not one-off collaboration经验.
- **HITL:** WeChat iLink channel has no interactive approval panel — must use non-interactive deny-by-default policy.
- **Inline renderer:** All interactive output should go through `Renderer.stream()`, not raw `System.out.println`. The bottom status bar is managed by JLine `Status` widget.
- **Parallel tools:** All three execution paths use `executeTools()` for parallel tool execution; never hand-write a for-loop for tool calls.

## Modifying Code — Hard Rules

When changing behavior, sync `AGENTS.md` / `README.md` / `ROADMAP.md` (ROADMAP only on status changes).

When changing any of these, update all linked files:

| Change | Must update |
|--------|-------------|
| Command entry points | `Main.java` + `CliCommandParser.java` + tests + `README.md` + `AGENTS.md` |
| Tool set | `ToolRegistry.java` + Agent/PlanExecuteAgent/SubAgent prompts + docs |
| Model/provider | Corresponding `*Client.java` + `LlmClientFactory.java` + `.env.example` + docs |
| MCP | `mcp/` + ToolRegistry + HITL + AuditLog + prompts + docs + tests |
| HITL/policy | `policy/` + ToolRegistry + HitlToolRegistry + prompts + docs + tests |
| Memory | `MemoryManager` + `LongTermMemory` + `TokenBudget` + tests + docs |

Never commit `.env`, real API keys, or `target/` artifacts.

## Quick Reference: Verification Paths

| Scenario | Command |
|----------|---------|
| Code search tools | `mvn test -Dtest=ToolRegistryTest,CodeSearchGoldenSetTest,ApprovalPolicyTest` |
| Command parsing | `mvn test -Dtest=CliCommandParserTest,PlanReviewInputParserTest,MainInputNormalizationTest` |
| DAG/Plan | `mvn test -Dtest=ExecutionPlanTest` |
| Multi-Agent | `mvn test -Dtest=AgentRoleTest,AgentMessageTest,AgentOrchestratorTest` |
| TUI/Terminal | `mvn test -Pphase16-smoke` |
| RAG | `mvn test -Dtest=CodeChunkerTest,CodeAnalyzerTest,VectorStoreTest,CodeIndexTest` |

## Detailed Reference

For comprehensive agent instructions, behavioral constraints, and modification rules, see `AGENTS.md`. For project-level memory injected into the system prompt, see `PAI.md`.
