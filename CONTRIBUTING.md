# Contributing to Minecraft API

Thank you for your interest in contributing! This document explains how to get
started, what to expect during the review process, and the conventions this
project follows.

## Table of Contents

- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Development Setup](#development-setup)
- [Making Changes](#making-changes)
  - [Branching Strategy](#branching-strategy)
  - [Code Style](#code-style)
  - [Working with JPA Models](#working-with-jpa-models)
  - [Commit Messages](#commit-messages)
  - [Testing](#testing)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Reporting Issues](#reporting-issues)
- [Project Architecture](#project-architecture)
- [Legal](#legal)

## Getting Started

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| [JDK](https://adoptium.net/) | **21+** | Required |
| [Git](https://git-scm.com/) | 2.x+ | For cloning and contributing |
| [IntelliJ IDEA](https://www.jetbrains.com/idea/) | Latest | Recommended IDE |

For running the full test suite:

| Requirement | Notes |
|-------------|-------|
| MariaDB or Docker | For SQL integration tests |
| Hypixel API key | Required for live API tests |
| Environment variables | `DATABASE_HOST`, `DATABASE_SCHEMA`, `DATABASE_PORT`, `DATABASE_USER`, `DATABASE_PASSWORD`, `HYPIXEL_API_KEY` |

### Development Setup

1. **Fork and clone the repository**

   [Fork the repository](https://github.com/SkyBlock-Simplified/minecraft-api/fork),
   then clone your fork:

   ```bash
   git clone https://github.com/<your-username>/minecraft-api.git
   cd minecraft-api
   ```

2. **Clone the `api` module alongside** (for local development)

   This module depends on the [api](https://github.com/SkyBlock-Simplified/api)
   module. If the `api` subproject is not present locally, the build falls back
   to a JitPack snapshot. For active development across both modules, clone
   them side by side:

   ```bash
   cd ..
   git clone https://github.com/SkyBlock-Simplified/api.git
   ```

3. **Build the project**

   ```bash
   cd minecraft-api
   ./gradlew build
   ```

4. **Open in IntelliJ IDEA**

   Open the project root as a Gradle project. Ensure the Lombok plugin is
   installed and annotation processing is enabled.

5. **Generate the H2 schema** (optional, for IntelliJ JPA column resolution)

   ```bash
   ./gradlew generateSchema
   ```

   This writes DDL to `.schema/` so IntelliJ can resolve `@Column` references
   in the editor.

6. **Verify the setup**

   ```bash
   # Unit tests (no external dependencies required)
   ./gradlew test
   ```

## Making Changes

### Branching Strategy

- Create a feature branch from `master` for your work.
- Use a descriptive branch name: `fix/nbt-long-array-tag`,
  `feat/new-skyblock-model`, `docs/api-client-examples`.

```bash
git checkout -b feat/my-feature master
```

### Code Style

#### General

- **Collections** - Always use `Concurrent.newList()`, `Concurrent.newMap()`,
  `Concurrent.newSet()` instead of standard Java collections.
- **Annotations** - Use `@NotNull` / `@Nullable` from `org.jetbrains.annotations`
  on all public method parameters and return types.
- **Lombok** - Use `@Getter`, `@RequiredArgsConstructor`, `@Log4j2`, etc.
  The logger field is non-static (`lombok.log.fieldIsStatic = false`).
- **Equals/HashCode** - Models implement `equals()` / `hashCode()` manually
  using `Objects.equals()` / `Objects.hash()` (not Lombok's
  `@EqualsAndHashCode`).

#### Javadoc

- **Class level** - Noun phrase describing what the type is.
- **Method level** - Active verb, third person singular.
- **Tags** - `@param`, `@return`, `@throws` on public methods. Lowercase
  sentence fragments, no trailing period. Single space after param name.
- **Punctuation** - Only single hyphens (` - `) as separators.
- Never use `@author` or `@since`.

### Working with JPA Models

When adding or modifying JPA entity models in `model/`:

1. **Entity class** - Annotate with `@Entity` and `@Table(name = "snake_case_name")`.
   Every non-transient field needs `@Column(name = "snake_case", nullable = false)`.

2. **JSON data file** - Place the JSON array in
   `src/main/resources/skyblock/{table_name}.json`. The filename must match the
   `@Table.name` value.

3. **FK relationships** - Use `@ManyToOne` with `@JoinColumn` for single-object
   FKs. For list FKs backed by a JSON ID array, use `@ForeignIds` with a
   companion `ConcurrentList<String>` field.

4. **Inner classes** - Annotate inner static classes with `@GsonType` for
   JSON column storage. Inner enums and interfaces do not need `@GsonType`.

5. **Topological ordering** - FK dependencies are inferred automatically from
   entity fields. No manual ordering is needed.

6. **Tests** - Add a test case to `JpaModelTest` for any new model.

### Commit Messages

Write clear, concise commit messages that describe *what* changed and *why*.

```
Add SkyBlock garden model with visitor and crop entities

Supports the new garden API response fields added in Hypixel API v2.
```

- Use the imperative mood ("Add", "Fix", "Update").
- Keep the subject line under 72 characters.
- Add a body when the *why* isn't obvious from the subject.

### Testing

Tests use JUnit 5 (Jupiter) with Hamcrest matchers. Two test tasks are
configured:

```bash
# Fast unit tests (no external dependencies)
./gradlew test

# Slow integration tests (require live database + Hypixel API key)
./gradlew slowTest
```

- **JPA model tests** (`JpaModelTest`) - verify all models load correctly
  from JSON into H2. These run without external dependencies.
- **API client tests** - require a live Hypixel API key and database
  connection. Tag these with `@Tag("slow")`.

## Submitting a Pull Request

1. **Push your branch** to your fork.

   ```bash
   git push origin feat/my-feature
   ```

2. **Open a Pull Request** against the `master` branch of
   [SkyBlock-Simplified/minecraft-api](https://github.com/SkyBlock-Simplified/minecraft-api).

3. **In the PR description**, include:
   - A summary of the changes and the motivation behind them.
   - Steps to test or verify the changes.
   - For new models: the JSON data file and any FK relationships.

4. **Respond to review feedback.** PRs may go through one or more rounds of
   review before being merged.

### What gets reviewed

- Correctness of JPA annotations and JSON data files.
- NBT serialization round-trip correctness (read -> write -> read).
- Impact on downstream modules (`simplified-bot`). Breaking changes to the
  public API should be discussed before implementation.
- Test coverage for new models and API response types.

## Reporting Issues

Use [GitHub Issues](https://github.com/SkyBlock-Simplified/minecraft-api/issues)
to report bugs or request features.

When reporting a bug, include:

- **Java version** (`java --version`)
- **Operating system**
- **Hypixel API response** (if applicable, sanitize your API key)
- **Full error stacktrace**
- **Steps to reproduce**
- **Expected vs. actual behavior**

## Project Architecture

A brief overview to help you find your way around the codebase:

```
src/main/java/dev/sbs/minecraftapi/
├── MinecraftApi.java           # Entry point (static initializer bootstraps everything)
├── client/
│   ├── hypixel/                # Hypixel API v2
│   │   ├── HypixelClient.java  # Feign client
│   │   └── response/skyblock/  # SkyBlockIsland, SkyBlockMember, sub-packages
│   ├── mojang/                 # Mojang API + MojangProxy (IPv6 rotation)
│   └── sbs/                    # SBS API (emojis, images, cached profiles)
├── model/                      # 33+ JPA entities backed by JSON resources
├── nbt/                        # NbtFactory + Tag hierarchy
├── render/
│   ├── image/                  # MinecraftHead, MinecraftText
│   ├── text/                   # ChatFormat, text segments
│   └── font/                   # Bundled OTF/TTF font files
└── skyblock/                   # SkyBlockDate, Rarity, GameMode, Weight, etc.
```

### Key data flow

```
MinecraftApi static init
  → registers Gson adapters
  → registers Feign clients
  → connects H2 JPA session
      → RepositoryFactory discovers models in model/ package
      → topological sort resolves FK dependencies
      → JsonRefreshStrategy loads each model's JSON file
      → entities are persisted into H2 and cached
```

## Legal

By submitting a pull request, you agree that your contributions are licensed
under the [Apache License 2.0](LICENSE.md), the same license that covers this
project.
