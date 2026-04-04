# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

This is a Gradle submodule (Java 21, Gradle 9.4+) of the SkyBlock-Simplified multi-module project. It can be built standalone or as part of the parent project.

```bash
# Build (from parent root)
./gradlew :minecraft-api:build

# Build standalone (from this directory, uses JitPack fallback for :api dependency)
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "dev.sbs.minecraftapi.client.SkyBlockIslandTest"

# Run a single test method
./gradlew test --tests "dev.sbs.minecraftapi.client.SkyBlockIslandTest.getGuildLevels_ok"
```

Tests use JUnit 5 with Hamcrest matchers. Many tests require a live database connection and Hypixel API key — they connect via `SimplifiedApi.getSessionManager().connect(JpaConfig.defaultSql())` and must call `disconnect()` in a `finally` block.

## Architecture

### Entry Point: `MinecraftApi`

`MinecraftApi` extends `SimplifiedApi` (from the `:api` module) and bootstraps everything in a static initializer block:
- Registers custom Gson type adapters (NBT, SkyBlock dates, SBS responses)
- Registers `NbtFactory` as a service
- Registers Feign client builders (`SbsClient`, `MojangClient`, `HypixelClient`)
- Connects an H2-backed JPA session for JSON-sourced models

Access pattern: `MinecraftApi.getClient(HypixelClient.class)`, `MinecraftApi.getRepository(Item.class)`, `MinecraftApi.getNbtFactory()`.

### Package Structure

**`client/`** — Feign-based HTTP clients for three external APIs:
- `hypixel/` — Hypixel API v2 (`@Route("api.hypixel.net/v2")`). Requires `HYPIXEL_API_KEY` via `KeyManager`. Endpoints defined as a Feign interface in `HypixelEndpoint` using `@RequestLine` annotations.
- `mojang/` — Mojang/Minecraft services. `MojangProxy` manages a pool of `MojangClient` instances with IPv6 rotation to avoid rate limits. Uses `@MojangDomain` annotation for per-method route switching.
- `sbs/` — SBS API (`@Route("api.sbs.dev")`). Serves item emojis, images, and cached Mojang profiles.

Each client follows the pattern: `Client<Endpoint>` with `configureErrorDecoder()` returning a custom exception type.

**`client/hypixel/response/skyblock/`** — Hypixel SkyBlock response models:
- `SkyBlockIsland` — Top-level profile/island containing a map of `UUID -> SkyBlockMember`
- `SkyBlockMember` — Player data within an island: skills, pets, dungeons, slayers, rift, crimson isle, mining, inventory, etc. Implements `PostInit` for post-deserialization processing.
- Sub-packages (`member/dungeon/`, `member/crimson/`, `member/rift/`, `member/mining/`, `member/pet/`, `member/slayer/`, `member/skill/`, `member/hoppity/`) contain nested progression data.

**`model/`** — JPA entities backed by JSON resource files in `src/main/resources/skyblock/`. Each model implements `JpaModel` and maps to a JSON file (e.g., `Item` -> `items.json`, `Stat` -> `stats.json`). These are loaded into an embedded H2 database at startup. Models use `@Entity`, `@Table`, `@Id`, `@Column`, `@ManyToOne`/`@JoinColumn` for relationships, and manual `equals()`/`hashCode()` via `Objects.equals()`/`Objects.hash()`.

**`nbt/`** — Minecraft NBT (Named Binary Tag) serialization. `NbtFactory` is the main API for reading/writing NBT data in multiple formats: Base64, byte arrays, files, streams, SNBT strings, and JSON. Tag hierarchy: `Tag` -> primitives (`IntTag`, `StringTag`, etc.), arrays (`ByteArrayTag`, etc.), collections (`CompoundTag`, `ListTag`).

**`render/`** — Minecraft text and image rendering:
- `text/` — `ChatFormat` (Minecraft color/formatting codes), `TextSegment`/`LineSegment`/`ColorSegment` for structured text with click/hover events
- `image/` — `MinecraftHead` (skin rendering), `MinecraftText` (text-to-image), sprite atlas handling
- `font/` — `MinecraftFont`, `ComicSansFont` for text rendering with bundled OTF/TTF files

**`skyblock/`** — SkyBlock game utilities:
- `date/SkyBlockDate` — Converts between real-time milliseconds and SkyBlock's in-game calendar (12 seasons/months, 31 days each). Key constants in `SkyBlockDate.Launch` and `SkyBlockDate.Length`.
- `common/` — Shared enums/types: `Rarity`, `GameMode`, `GameStage`, `Profile`, `NbtContent`, `Weight`, `Experience`
- `crafting/` — Recipe data structures

## Module-Specific Patterns

- **`Optional<T>`** fields are common — Gson deserializes missing JSON fields as `Optional.empty()`.
- **`Objects.equals()`/`Objects.hash()`** — Models implement `equals()`/`hashCode()` manually using `java.util.Objects` (not Lombok's `@EqualsAndHashCode`).

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **minecraft-api** (5184 symbols, 15332 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## When Debugging

1. `gitnexus_query({query: "<error or symptom>"})` — find execution flows related to the issue
2. `gitnexus_context({name: "<suspect function>"})` — see all callers, callees, and process participation
3. `READ gitnexus://repo/minecraft-api/process/{processName}` — trace the full execution flow step by step
4. For regressions: `gitnexus_detect_changes({scope: "compare", base_ref: "main"})` — see what your branch changed

## When Refactoring

- **Renaming**: MUST use `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` first. Review the preview — graph edits are safe, text_search edits need manual review. Then run with `dry_run: false`.
- **Extracting/Splitting**: MUST run `gitnexus_context({name: "target"})` to see all incoming/outgoing refs, then `gitnexus_impact({target: "target", direction: "upstream"})` to find all external callers before moving code.
- After any refactor: run `gitnexus_detect_changes({scope: "all"})` to verify only expected files changed.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Tools Quick Reference

| Tool | When to use | Command |
|------|-------------|---------|
| `query` | Find code by concept | `gitnexus_query({query: "auth validation"})` |
| `context` | 360-degree view of one symbol | `gitnexus_context({name: "validateUser"})` |
| `impact` | Blast radius before editing | `gitnexus_impact({target: "X", direction: "upstream"})` |
| `detect_changes` | Pre-commit scope check | `gitnexus_detect_changes({scope: "staged"})` |
| `rename` | Safe multi-file rename | `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` |
| `cypher` | Custom graph queries | `gitnexus_cypher({query: "MATCH ..."})` |

## Impact Risk Levels

| Depth | Meaning | Action |
|-------|---------|--------|
| d=1 | WILL BREAK — direct callers/importers | MUST update these |
| d=2 | LIKELY AFFECTED — indirect deps | Should test |
| d=3 | MAY NEED TESTING — transitive | Test if critical path |

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/minecraft-api/context` | Codebase overview, check index freshness |
| `gitnexus://repo/minecraft-api/clusters` | All functional areas |
| `gitnexus://repo/minecraft-api/processes` | All execution flows |
| `gitnexus://repo/minecraft-api/process/{name}` | Step-by-step execution trace |

## Self-Check Before Finishing

Before completing any code modification task, verify:
1. `gitnexus_impact` was run for all modified symbols
2. No HIGH/CRITICAL risk warnings were ignored
3. `gitnexus_detect_changes()` confirms changes match expected scope
4. All d=1 (WILL BREAK) dependents were updated

## Keeping the Index Fresh

After committing code changes, the GitNexus index becomes stale. Re-run analyze to update it:

```bash
npx gitnexus analyze
```

If the index previously included embeddings, preserve them by adding `--embeddings`:

```bash
npx gitnexus analyze --embeddings
```

To check whether embeddings exist, inspect `.gitnexus/meta.json` — the `stats.embeddings` field shows the count (0 means no embeddings). **Running analyze without `--embeddings` will delete any previously generated embeddings.**

> Claude Code users: A PostToolUse hook handles this automatically after `git commit` and `git merge`.

## CLI

| Task | Read this skill file                                |
|------|-----------------------------------------------------|
| Understand architecture / "How does X work?" | `~/.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `~/.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `~/.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `~/.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `~/.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `~/.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
