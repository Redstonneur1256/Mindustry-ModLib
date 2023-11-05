import java.time.Instant

plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version ("8.1.1") apply (false)
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "fr.redstonneur1256"
    version = System.getenv("GITHUB_VERSION")?.subSequence(0, 8) ?: "dev-SNAPSHOT"

    java {
        withJavadocJar()
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    tasks.withType(JavaCompile::class.java).configureEach {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenCentral()
        maven("https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository")
        maven("https://jitpack.io")
        maven("https://repo.mc-skyplex.net/releases")
    }

    sourceSets {
        main {
            java.srcDirs("src")
            resources.srcDirs("res")
        }
        test {
            java.srcDirs()
            resources.srcDirs()
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }

    tasks.processResources {
        filesMatching(listOf("*.properties", "mod.json")) {
            expand(mapOf(
                    "version" to version,
                    "build" to (System.getenv("GITHUB_SHA") ?: "dev"),
                    "built" to Instant.now().toString()
            ))
        }
        outputs.upToDateWhen {
            false
        }
    }
}
