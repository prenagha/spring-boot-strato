
plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.cdklib)
    implementation(libs.constructs)
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
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "com.renaghan.todo.cdk.App"
}
