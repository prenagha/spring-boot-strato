package com.renaghan.todo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class AwsConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "custom",
      name = "use-cognito-as-identity-provider",
      havingValue = "true")
  public CognitoIdentityProviderClient cognitoIdentityProviderClient(
      AwsRegionProvider regionProvider, AwsCredentialsProvider credentialsProvider) {
    return CognitoIdentityProviderClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(regionProvider.getRegion())
        .build();
  }
}
