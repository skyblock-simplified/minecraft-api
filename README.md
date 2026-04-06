# Minecraft API

Minecraft and Hypixel API client library for the
[SkyBlock Simplified](https://github.com/SkyBlock-Simplified) ecosystem.
Provides Feign-based HTTP clients, JPA entity models backed by JSON resources,
NBT serialization, and Minecraft text/image rendering.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Quick Example](#quick-example)
- [Architecture](#architecture)
  - [Entry Point](#entry-point)
  - [HTTP Clients](#http-clients)
  - [JPA Models](#jpa-models)
  - [NBT](#nbt)
  - [Rendering](#rendering)
  - [SkyBlock Utilities](#skyblock-utilities)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Hypixel API v2 client** - Full Feign-based client for the Hypixel API
  including SkyBlock profiles, player data, auctions, and more
- **Mojang API client** - Player UUID/name lookups with IPv6 rotation via
  `MojangProxy` to avoid rate limits
- **SBS API client** - Item emojis, images, and cached profile lookups from
  the SkyBlock Simplified backend
- **JPA entity models** - 33+ SkyBlock data models (items, enchantments,
  reforges, pets, skills, collections, etc.) backed by JSON classpath resources
  and loaded into an embedded H2 database at startup
- **NBT serialization** - Read and write Minecraft NBT data in multiple
  formats: Base64, byte arrays, files, streams, SNBT strings, and JSON
- **Text rendering** - Convert Minecraft chat format codes to styled images
  with bundled Minecraft and ComicSans fonts
- **Skin rendering** - Render Minecraft player heads from skin textures
- **SkyBlock calendar** - Convert between real-time milliseconds and SkyBlock's
  in-game calendar system

## Getting Started

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| [JDK](https://adoptium.net/) | **21+** | Required |
| [Gradle](https://gradle.org/) | **9.4+** | Included via wrapper (`./gradlew`) |

### Installation

This module depends on the [api](../api) module. For local development, clone
both repositories side by side and use composite builds:

```bash
git clone https://github.com/SkyBlock-Simplified/api.git
git clone https://github.com/SkyBlock-Simplified/minecraft-api.git
cd minecraft-api
```

Build the library:

```bash
./gradlew build
```

Run tests:

```bash
# Unit tests (excludes slow integration tests)
./gradlew test

# Slow integration tests (require live database + Hypixel API key)
./gradlew slowTest
```

> [!NOTE]
> Many tests require a live MariaDB database and a valid `HYPIXEL_API_KEY`
> environment variable. These are tagged as slow and excluded from the default
> test task.

<details>
<summary>Using as a dependency in another Gradle project</summary>

**JitPack** (for snapshot builds):

```kotlin
repositories {
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.SkyBlock-Simplified:minecraft-api:master-SNAPSHOT")
}
```

**Composite build** (for local development):

```kotlin
// settings.gradle.kts
includeBuild("../minecraft-api")

// build.gradle.kts
dependencies {
    api("dev.sbs:minecraft-api:0.1.0")
}
```

</details>

## Quick Example

```java
public class ExampleApp {
    public static void main(String[] args) {
        // MinecraftApi bootstraps automatically via its static initializer.
        // All Feign clients, Gson adapters, and the H2 JPA session are ready.

        // Query a JSON-backed repository
        ConcurrentList<Item> swords = MinecraftApi.getRepository(Item.class)
            .stream()
            .filter(item -> item.getMaterial().equals("DIAMOND_SWORD"))
            .collect(Concurrent.toList());

        // Use the Hypixel API client
        HypixelClient hypixel = MinecraftApi.getClient(HypixelClient.class);

        // Read NBT from a Base64 string
        CompoundTag nbt = MinecraftApi.getNbtFactory().fromBase64(base64String);

        // Convert between real time and SkyBlock calendar
        SkyBlockDate.SkyBlockTime sbTime = SkyBlockDate.SkyBlockTime.now();
    }
}
```

## Architecture

### Entry Point

`MinecraftApi` is a non-instantiable static service locator that bootstraps all
shared and game-related infrastructure in a single static initializer block:

1. Configures `Gson` with the `JpaExclusionStrategy` plus Minecraft/Hypixel type adapters in one combined build
2. Registers core services: `Scheduler`, `SessionManager`, `ImageFactory`, `NbtFactory`
3. Registers Feign clients: `MojangProxy` (with IPv6 rotation), `SbsClient`, `HypixelClient`, `MinecraftServerPing`
4. Connects an H2 in-memory JPA session that loads JSON model files from `skyblock/`
5. Exposes static `KeyManager` and `ServiceManager` fields via Lombok `@Getter`

### HTTP Clients

| Client | Route | Purpose |
|--------|-------|---------|
| `HypixelClient` | `api.hypixel.net/v2` | Hypixel API v2 (profiles, players, auctions) |
| `MojangClient` | Various Mojang endpoints | UUID/name lookups, skin textures |
| `MojangProxy` | (pool of `MojangClient`) | IPv6 rotation to avoid Mojang rate limits |
| `SbsClient` | `api.sbs.dev` | Item emojis, images, cached profiles |
| `MinecraftServerPing` | Direct TCP | Minecraft server list ping |

### JPA Models

33+ entity classes in `model/` are backed by JSON files in
`src/main/resources/skyblock/`. At startup, these are deserialized and
persisted into an embedded H2 database, enabling Hibernate queries and
caching against static SkyBlock game data.

Models use `@Entity`, `@Table`, `@Id`, `@Column`, and `@ManyToOne` /
`@JoinColumn` for relationships. Dependencies between models are resolved
via topological sort, and `@PostInit` methods resolve cross-entity references
after loading.

### NBT

`NbtFactory` reads and writes Minecraft NBT data in multiple formats:

| Format | Read | Write |
|--------|------|-------|
| Base64 | `fromBase64()` | `toBase64()` |
| Byte array | `fromByteArray()` | `toByteArray()` |
| File | `fromFile()` | `toFile()` |
| InputStream / OutputStream | `fromStream()` | `toStream()` |
| SNBT (string) | `fromSnbt()` | `toSnbt()` |
| JSON | - | `toJson()` |
| Classpath resource | `fromResource()` | - |
| URL | `fromUrl()` | - |

Tag hierarchy: `Tag` -> primitives (`IntTag`, `StringTag`, etc.), arrays
(`ByteArrayTag`, etc.), collections (`CompoundTag`, `ListTag`).

### Rendering

| Class | Purpose |
|-------|---------|
| `MinecraftText` | Render Minecraft chat text to images with formatting codes |
| `MinecraftHead` | Render player heads from skin textures |
| `ChatFormat` | Minecraft color and formatting code constants |

Bundled fonts: Minecraft (Regular, Bold, Italic, BoldItalic) and ComicSans.

### SkyBlock Utilities

| Class | Purpose |
|-------|---------|
| `SkyBlockDate` | Real-time <-> in-game calendar conversion |
| `Rarity` | SkyBlock item rarity tiers |
| `GameMode` | Ironman, Stranded, Bingo, etc. |
| `Weight` | Senither/Lily weight calculation |
| `Experience` | XP curve utilities |

## Project Structure

```
minecraft-api/
├── src/main/java/dev/sbs/minecraftapi/
│   ├── MinecraftApi.java               # Entry point (self-contained service locator)
│   ├── client/
│   │   ├── hypixel/                    # Hypixel API v2 client and responses
│   │   │   └── response/skyblock/      # SkyBlockIsland, SkyBlockMember, etc.
│   │   ├── mojang/                     # Mojang API client and MojangProxy
│   │   └── sbs/                        # SBS API client (emojis, images)
│   ├── model/                          # JPA entities (33+ SkyBlock data models)
│   ├── nbt/                            # NBT serialization (NbtFactory, Tag types)
│   ├── render/
│   │   ├── image/                      # MinecraftHead, MinecraftText
│   │   ├── text/                       # ChatFormat, text segments
│   │   └── font/                       # Bundled Minecraft and ComicSans fonts
│   └── skyblock/                       # Game utilities (calendar, rarity, etc.)
├── src/main/resources/skyblock/        # JSON model data files
├── src/test/java/                      # JUnit 5 + Hamcrest tests
├── build.gradle.kts
└── gradle/libs.versions.toml           # Version catalog
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, code style
guidelines, and how to submit a pull request.

## License

This project is licensed under the **Apache License 2.0** - see
[LICENSE.md](LICENSE.md) for the full text.
