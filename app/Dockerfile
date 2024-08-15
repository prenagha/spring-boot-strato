# when building on Mac you need to specify the platform so
# Docker builds an image compatible with
# Mac Silicon (arm64) and AWS ECS x86 (amd64) runtime
FROM --platform=linux/arm64,linux/amd64 eclipse-temurin:17-jre

COPY build/libs/app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
