# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

This is a Gradle submodule (Java 21, Gradle 9.4+) of the SkyBlock-Simplified multi-module project. It can be built standalone or as part of the parent project.

```bash
# Build (from parent root)
./gradlew :minecraft-api:build

# Build standalone (from this directory)
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "dev.sbs.minecraftapi.client.SkyBlockIslandTest"

# Run a single test method
./gradlew test --tests "dev.sbs.minecraftapi.client.SkyBlockIslandTest.getGuildLevels_ok"
```

Tests use JUnit 5 with Hamcrest matchers. Many tests require a live database connection and Hypixel API key — they connect via `MinecraftApi.getSessionManager().connect(JpaConfig.defaultSql())` and must call `disconnect()` in a `finally` block.

## Architecture

### Entry Point: `MinecraftApi`

`MinecraftApi` is a non-instantiable static service locator that bootstraps everything in a single static initializer block:
- Registers `Gson` with the `JpaExclusionStrategy` plus all custom type adapters (NBT, SkyBlock dates, SBS responses) in one combined build
- Registers core services: `Scheduler`, `SessionManager`, `ImageFactory`, `NbtFactory`
- Registers Feign clients: `MojangProxy`, `SbsClient`, `HypixelClient`, `MinecraftServerPing`
- Connects an H2-backed JPA session for JSON-sourced models
- Exposes the global `KeyManager` (case-insensitive) and `ServiceManager` static fields via Lombok `@Getter`

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
