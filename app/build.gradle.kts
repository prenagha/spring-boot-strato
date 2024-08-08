plugins {
    java
    alias(libs.plugins.spring)
    alias(libs.plugins.springDep)
    idea
}

group = "com.renaghan"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.spring)
    developmentOnly(libs.bundles.springDev)
    testImplementation(libs.bundles.springTest)
}

tasks.bootRun.configure {
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
        @Suppress("UnstableApiUsage") val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junitVer)
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.javaVer.get())
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

