# when building on Mac you need to specify the platform so
# Docker builds an image compatible with AWS ECS runtime
FROM --platform=linux/amd64 eclipse-temurin:17-jre

COPY build/libs/app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
