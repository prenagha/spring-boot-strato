plugins {
    application
    idea
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.cdklib)
    implementation(libs.constructs)
    implementation(libs.strato)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.10.2")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.javaVer.get())
    }
}

application {
    mainClass = "com.renaghan.todo.cdk.Infra"
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