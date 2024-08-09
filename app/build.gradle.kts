plugins {
    java
    alias(libs.plugins.spring)
    alias(libs.plugins.springDep)
    idea
}

group = "com.renaghan"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.spring)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    developmentOnly(libs.bundles.springDev)
    testImplementation(libs.bundles.springTest)

    modules {
        module("org.springframework.boot:spring-boot-starter-tomcat") {
            replacedBy("org.springframework.boot:spring-boot-starter-jetty", "Use Jetty instead of Tomcat")
        }
    }
}

tasks.bootRun.configure {
    jvmArgs = listOf(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
        "-Dspring.profiles.active=dev",
    )
}

tasks.jar.configure {
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

