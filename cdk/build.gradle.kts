import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.testing

plugins {
    `java-library`
    idea
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.151.1")
    implementation("software.constructs:constructs:10.3.0")
    implementation("dev.stratospheric:cdk-constructs:0.1.15")
    implementation("org.passay:passay:1.6.4")
}

tasks.register<JavaExec>("infra") {
    group = "app"
    mainClass = "com.renaghan.todo.cdk.Infrastructure"
    classpath = project.sourceSets["main"].runtimeClasspath
}
tasks.register<JavaExec>("service") {
    group = "app"
    mainClass = "com.renaghan.todo.cdk.ServiceApp"
    classpath = project.sourceSets["main"].runtimeClasspath
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        getting(JvmTestSuite::class) {
            useJUnitJupiter("5.10.3")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        inheritOutputDirs = true
        excludeDirs = setOf(
            file("build"),
            file("node_modules"),
            file("cdk.out")
        )
    }
}