import org.gradle.kotlin.dsl.named

plugins {
    id("org.springframework.boot") version "3.3.2"
    java
    idea
}

apply(plugin = "io.spring.dependency-management")

group = "com.renaghan"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

val lib = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(lib.findBundle("spring").get())
    developmentOnly(lib.findBundle("springDev").get())
    testImplementation(lib.findBundle("springTest").get())
}

tasks.named<JavaExec>("bootRun").configure {
    jvmArgs = listOf(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
        "-Dspring.profiles.active=dev",
    )
}

tasks.jar {
    enabled = false
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(lib.findVersion("junitVer").get().requiredVersion)
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(lib.findVersion("javaVer").get().requiredVersion)
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        inheritOutputDirs = true
        excludeDirs = setOf(
            file("build"),
        )
    }
}

