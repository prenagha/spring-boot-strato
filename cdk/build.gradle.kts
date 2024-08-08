import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.testing

plugins {
    `java-library`
    idea
}

repositories {
    mavenCentral()
}

val lib = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(lib.findBundle("cdk").get())
}

tasks.register<JavaExec>("repository") {
    group = "app"
    mainClass = "com.renaghan.todo.cdk.DockerRepositoryApp"
    classpath = project.sourceSets["main"].runtimeClasspath
}
tasks.register<JavaExec>("network") {
    group = "app"
    mainClass = "com.renaghan.todo.cdk.NetworkApp"
    classpath = project.sourceSets["main"].runtimeClasspath
}
tasks.register<JavaExec>("service") {
    group = "app"
    mainClass = "com.renaghan.todo.cdk.ServiceApp"
    classpath = project.sourceSets["main"].runtimeClasspath
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
            file("node_modules"),
            file("cdk.out")
        )
    }
}