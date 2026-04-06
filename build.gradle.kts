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
    api("dev.sbs:api:0.1.0")
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
}
