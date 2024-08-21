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

dependencyManagement {
    imports {
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.1")
        mavenBom("org.testcontainers:testcontainers-bom:1.19.8")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-activemq")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")


    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("de.siegmar:logback-awslogs-json-encoder:2.0.0")
    implementation("io.micrometer:micrometer-registry-cloudwatch2")

    // monitoring endpoints
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // auth
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // web stuff
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
    implementation("org.webjars:webjars-locator-core")
    implementation("org.webjars:bootstrap:5.3.3")
    implementation("org.webjars:font-awesome:6.5.2")
    implementation("org.webjars:jquery:3.7.1")
    implementation("org.webjars:popper.js:2.11.7")
    implementation("org.webjars:sockjs-client:1.5.1")
    implementation("org.webjars:stomp-websocket:2.3.4")

    // aws
    implementation("io.awspring.cloud:spring-cloud-aws-starter")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-ses")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-dynamodb")
    implementation("software.amazon.awssdk:cognitoidentityprovider")
    implementation("org.apache.activemq:activemq-kahadb-store")
    implementation("org.apache.activemq:activemq-stomp")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:localstack")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:localstack")
    testImplementation("com.codeborne:selenide:7.3.2")

    runtimeOnly("org.postgresql:postgresql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

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

