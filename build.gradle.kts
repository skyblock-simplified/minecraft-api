plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    maven(url = "https://jitpack.io")
}

dependencies {
    // IntelliJ Annotations
    implementation(group = "org.jetbrains", name = "annotations", version = "24.0.1")

    // Resource Checker Annotations
    implementation(group = "dev.sbs", name = "simplified-annotations", version = "1.0.4")
    annotationProcessor(group = "dev.sbs", name = "simplified-annotations", version = "1.0.4")

    // Lombok Annotations
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.30")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.30")
    testCompileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.30")
    testAnnotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.30")

    // Tests
    testImplementation(group = "org.hamcrest", name = "hamcrest", version = "2.2")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.10.0")
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.10.0")
    testImplementation(group = "com.h2database", name = "h2", version = "2.2.224")

    // Logging
    implementation(group = "org.apache.logging.log4j", name = "log4j-core", version = "2.25.3")
    implementation(group = "org.apache.logging.log4j", name = "log4j-slf4j-impl", version = "2.25.3")

    // Deserialization
    implementation(group = "com.google.code.gson", name = "gson", version = "2.10.1")
    implementation(group = "org.yaml", name = "snakeyaml", version = "2.0")

    // Database
    implementation(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "3.1.4")
    implementation(group = "org.ehcache", name = "ehcache", version = "3.10.8")
    implementation(group = "org.hibernate", name = "hibernate-hikaricp", version = "5.6.15.Final") // 7.0.2.Final
    implementation(group = "org.hibernate", name = "hibernate-jcache", version = "5.6.15.Final")

    // Api Clients
    implementation(group = "io.github.openfeign", name = "feign-gson", version = "13.6")
    implementation(group = "io.github.openfeign", name = "feign-httpclient", version = "13.6")

    // Projects
    implementation(optionalProject(":api", "com.github.skyblock-simplified:api:master-SNAPSHOT"))
}

fun Project.optionalProject(path: String, fallbackDependency: String): Any =
    if (project.rootProject.findProject(path) != null) {
        project(path)
    } else {
        fallbackDependency
    }

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    test {
        useJUnitPlatform()
    }
}
