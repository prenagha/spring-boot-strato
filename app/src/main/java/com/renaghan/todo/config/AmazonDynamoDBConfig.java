package com.renaghan.todo.config;

import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver;
import jakarta.annotation.Nonnull;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AmazonDynamoDBConfig {

  @Bean
  public DynamoDbTableNameResolver dynamoDbTableNameResolver(Environment environment) {
    String environmentName = environment.getProperty("custom.environment");
    String applicationName = environment.getProperty("spring.application.name");

    return new DynamoDbTableNameResolver() {
      @Override
      @Nonnull
      public <T> String resolve(@Nonnull Class<T> clazz) {
        String className =
            clazz.getSimpleName().replaceAll("(.)(\\p{Lu})", "$1_$2").toLowerCase(Locale.ROOT);
        return environmentName + "-" + applicationName + "-" + className;
      }
    };
  }
}
