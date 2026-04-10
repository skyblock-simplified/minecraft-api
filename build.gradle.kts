plugins {
    id("java-library")
    alias(libs.plugins.jmh)
    idea
}

group = "dev.sbs"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    maven(url = "https://jitpack.io")
}

dependencies {
    // Simplified Annotations
    annotationProcessor(libs.simplified.annotations)

    // Lombok Annotations
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Tests
    testImplementation(libs.hamcrest)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)

    // Projects
    api("dev.sbs:asset-renderer:0.1.0")

    // Simplified Libraries (extracted to github.com/simplified-dev)
    api("com.github.simplified-dev:collections:master-SNAPSHOT")
    api("com.github.simplified-dev:utils:master-SNAPSHOT")
    api("com.github.simplified-dev:reflection:master-SNAPSHOT")
    api("com.github.simplified-dev:image:68aec439ed")
    api("com.github.simplified-dev:gson-extras:master-SNAPSHOT")
    api("com.github.simplified-dev:scheduler:master-SNAPSHOT")
    api("com.github.simplified-dev:manager:master-SNAPSHOT")
    api("com.github.simplified-dev:client:master-SNAPSHOT")
    api("com.github.simplified-dev:persistence:master-SNAPSHOT")

    // Gson (used directly in MinecraftApi static init)
    api(libs.gson)
}

idea {
    module {
        testSources.from(sourceSets["jmh"].java.srcDirs)
        testResources.from(sourceSets["jmh"].resources.srcDirs)
        excludeDirs.addAll(listOf(
            layout.projectDirectory.dir(".gitnexus").asFile,
            layout.projectDirectory.dir(".schema").asFile,
            layout.projectDirectory.dir("customdata").asFile,
            layout.projectDirectory.dir("minecraft").asFile,
            layout.projectDirectory.dir("texturepacks").asFile
        ))
    }
}

tasks {
    test {
        useJUnitPlatform {
            excludeTags("slow")
        }
    }
    register<Test>("slowTest") {
        description = "Runs slow integration tests (shutdown, thread leak detection)"
        group = "verification"
        useJUnitPlatform {
            includeTags("slow")
        }
    }
    register<JavaExec>("generateSchema") {
        description = "Generates H2 DDL schema for IntelliJ JPA column resolution"
        group = "ide"
        mainClass.set("dev.sbs.minecraftapi.schema.SchemaExporter")
        classpath = sourceSets["test"].runtimeClasspath
        args = listOf(layout.projectDirectory.dir(".schema").asFile.absolutePath)
    }
    register<JavaExec>("generateAuctionFixture") {
        description = "Fetches the full SkyBlock auction house and writes the JMH benchmark fixture. Requires HYPIXEL_API_KEY env var. Idempotent."
        group = "jmh"
        mainClass.set("dev.sbs.minecraftapi.nbt.AuctionFixtureGenerator")
        classpath = sourceSets["test"].runtimeClasspath
    }
}
