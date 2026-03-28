plugins {
    id("java-library")
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
