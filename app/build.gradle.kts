@file:Suppress("PackageUpdate")

plugins {
    java
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    idea
}

group = "com.renaghan"

repositories {
    mavenCentral()
}

val awsSpringVersion = "3.1.1"

dependencyManagement {
    imports {
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:${awsSpringVersion}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    implementation("de.siegmar:logback-awslogs-json-encoder:2.0.0")

    // monitoring endpoints
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // auth
    //implementation("org.springframework.boot:spring-boot-starter-security")
    //implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    //implementation("software.amazon.awssdk:cognitoidentityprovider")

    // web stuff
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
    implementation("org.webjars:webjars-locator-core")
    implementation("org.webjars:bootstrap:4.6.1")
    implementation("org.webjars:font-awesome:5.15.3")


    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

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
        )
    }
}

