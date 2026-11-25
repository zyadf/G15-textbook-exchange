# ITS66704 Group Assignment - Scenario Agent (student skeleton)

A JavaFX desktop agent built with **LangChain4j 1.20.0**, Java 21 and Maven.
No Spring Boot, no SQL database: all data lives in JSON files.

## 1. First run (Week 9)

1. Install JDK 21, Maven 3.9+, Git, and [Ollama](https://ollama.com) (or LM Studio) if you are going to host the LLM locally.
   You can also access hosted LLMs like ChatGPT (OpenAI) or Claude (Anthropic) using a valid API key. 
2. `ollama pull qwen3:8b` (8 GB laptops: `qwen3:4b`).
3. Edit `config.properties`: set `group.number` to YOUR group number.
4. See your scenario and house rules:
   `mvn -q compile exec:java -Dexec.mainClass=my.edu.taylors.ap.config.GroupSeeder`
5. Run the app: `mvn javafx:run`. Ask "what time is it?" and "what are your house rules?"
   and watch the **Agent trace** tab.
6. Run the tests: `mvn verify`.

## 2. What is already done for you

| Package | Status | What it does |
|---------|--------|--------------|
| `config` | complete | config file, group seeding, house-rule text |
| `llm` | complete | model providers (Ollama / LM Studio / OpenAI / Anthropic), AI Service assembly |
| `trace` | complete - DO NOT MODIFY | hash-chained JSONL trace of every model call, tool call and guardrail decision |
| `ui` | complete core | MVC chat UI, background Tasks, confirm dialog, data tab, **watermark (do not remove)** |
| `data` | complete | `JsonStore<T>`: generic JSON-file repository with atomic writes |
| `guardrail` | worked example | `ScopeInputGuardrail` |
| `scenario/common` | complete | `ScenarioModule`, `RuleViolationException`, `Money`, `DateTimes`, `StructuredResult` |
| `scenario/mine` | **YOUR WORK** | `MyScenarioModule`, `ExampleTools` (worked example), `MyOutputGuardrail` |
| `memory` | **YOUR WORK (A6)** | `FileChatMemoryStore` currently in-memory only |
| `rag` | **YOUR WORK (B2)** | `KnowledgeBase` |

Search for `TODO [` to find every task; the code in brackets matches the brief.

## 3. Rules

* Keep `runtime/traces/*.jsonl` - commit them. Verify with
  `mvn -q exec:java -Dexec.mainClass=my.edu.taylors.ap.trace.TraceVerifier -Dexec.args="runtime/traces"`.
* Never commit API keys. Put the NAME of the environment variable in `config.properties`.
* Do not delete `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.cursorrules` or `.github/copilot-instructions.md`.
* Record all AI use in `AI_USAGE_LOG.md`.
* Every member commits their own work from their own Git account.
