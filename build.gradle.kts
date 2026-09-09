import org.gradle.api.tasks.bundling.Jar
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import java.security.MessageDigest

plugins {
    java
}

group = "com.zpkdxgames"
version = "2.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

val coreJar = layout.projectDirectory.file("libs/PlexonCore-2.0.4.jar")

dependencies {
    compileOnly(files(coreJar))
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    compileOnly("me.clip:placeholderapi:2.12.1")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(files(coreJar))
    testImplementation("io.papermc.paper:paper-api:26.2.build.121-stable")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("PlexonSkills-${project.version}.jar")
    manifest {
        attributes["Implementation-Title"] = "PlexonSkills"
        attributes["Implementation-Version"] = project.version
        attributes["Built-By"] = "PlexonSkills reproducible build"
    }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// PlexonSkills intentionally does not shade Paper/Core/PAPI and uses Core-owned SQLite,
// so the normal JAR is the installable distribution JAR.
tasks.register("shadowJar") {
    group = "build"
    description = "Builds the installable PlexonSkills JAR (no runtime shading required)."
    dependsOn(tasks.named("jar"))
}

tasks.register("verifyDistribution") {
    group = "verification"
    dependsOn(tasks.named("jar"))
    doLast {
        val jarFile = tasks.named<Jar>("jar").get().archiveFile.get().asFile
        val tree = zipTree(jarFile)
        val names = tree.files.map { it.toString().replace('\\', '/') }
        fun requireEntry(fragment: String) {
            check(names.any { it.endsWith(fragment) }) { "Distribution missing $fragment" }
        }
        requireEntry("com/zpkdxgames/plexonskills/PlexonSkillsPlugin.class")
        requireEntry("com/zpkdxgames/plexonskills/api/PlexonSkillsAPI.class")
        requireEntry("plugin.yml")
        check(names.none { it.contains("com/zpkdxgames/plexoncore/") }) { "PlexonCore classes must not be bundled" }
        check(names.none { it.contains("org/bukkit/") || it.contains("io/papermc/") }) { "Paper API classes must not be bundled" }
        check(names.none { it.contains("me/clip/placeholderapi/") }) { "PlaceholderAPI classes must not be bundled" }
        check(names.none { it.contains("/test/") || it.endsWith("Test.class") }) { "Test/debug classes must not be bundled" }
        println("Distribution verification passed: ${jarFile.name} (${jarFile.length()} bytes)")
    }
}

tasks.register("writeSha256") {
    group = "distribution"
    dependsOn(tasks.named("jar"))
    doLast {
        val jarFile = tasks.named<Jar>("jar").get().archiveFile.get().asFile
        val digest = MessageDigest.getInstance("SHA-256").digest(jarFile.readBytes()).joinToString("") { "%02x".format(it) }
        val out = layout.buildDirectory.file("distributions/SHA256SUMS.txt").get().asFile
        out.parentFile.mkdirs()
        out.writeText("$digest  ${jarFile.name}\n")
        println("SHA-256: $digest")
    }
}

tasks.named("check") {
    dependsOn("verifyDistribution")
}
