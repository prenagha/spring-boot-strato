package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import java.util.Arrays;
import java.util.Collections;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AutoVerifiedAttrs;
import software.amazon.awscdk.services.cognito.CognitoDomainOptions;
import software.amazon.awscdk.services.cognito.Mfa;
import software.amazon.awscdk.services.cognito.OAuthFlows;
import software.amazon.awscdk.services.cognito.OAuthScope;
import software.amazon.awscdk.services.cognito.OAuthSettings;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.StandardAttribute;
import software.amazon.awscdk.services.cognito.StandardAttributes;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolClientIdentityProvider;
import software.amazon.awscdk.services.cognito.UserPoolDomain;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

class CognitoStack extends Stack {

  private final UserPool userPool;
  private final UserPoolClient userPoolClient;
  private final String logoutUrl;

  public CognitoStack(CDKApp app, String id) {
    super(
        app,
        id,
        StackProps.builder().stackName(app.appEnv().prefix("Cognito")).env(app.awsEnv()).build());

    this.logoutUrl =
        String.format(
            "https://%s.auth.%s.amazoncognito.com/logout",
            app.getContext("loginPageDomainPrefix"), app.awsEnv().getRegion());

    this.userPool =
        UserPool.Builder.create(this, "userPool")
            .userPoolName(app.getContext("applicationName") + "-user-pool")
            .selfSignUpEnabled(false)
            .accountRecovery(AccountRecovery.EMAIL_ONLY)
            .autoVerify(AutoVerifiedAttrs.builder().email(true).build())
            .signInAliases(SignInAliases.builder().username(true).email(true).build())
            .signInCaseSensitive(true)
            .standardAttributes(
                StandardAttributes.builder()
                    .email(StandardAttribute.builder().required(true).mutable(false).build())
                    .build())
            .mfa(Mfa.OFF)
            .passwordPolicy(
                PasswordPolicy.builder()
                    .requireLowercase(true)
                    .requireDigits(true)
                    .requireSymbols(true)
                    .requireUppercase(true)
                    .minLength(12)
                    .tempPasswordValidity(Duration.days(7))
                    .build())
            .build();

    this.userPoolClient =
        UserPoolClient.Builder.create(this, "userPoolClient")
            .userPoolClientName(app.getContext("applicationName") + "-client")
            .generateSecret(true)
            .userPool(this.userPool)
            .oAuth(
                OAuthSettings.builder()
                    .callbackUrls(
                        Arrays.asList(
                            String.format("%s/login/oauth2/code/cognito", app.getApplicationURL()),
                            "http://localhost:8080/login/oauth2/code/cognito"))
                    .logoutUrls(Arrays.asList(app.getApplicationURL(), "http://localhost:8080"))
                    .flows(OAuthFlows.builder().authorizationCodeGrant(true).build())
                    .scopes(Arrays.asList(OAuthScope.EMAIL, OAuthScope.OPENID, OAuthScope.PROFILE))
                    .build())
            .supportedIdentityProviders(
                Collections.singletonList(UserPoolClientIdentityProvider.COGNITO))
            .build();

    UserPoolDomain.Builder.create(this, "userPoolDomain")
        .userPool(this.userPool)
        .cognitoDomain(
            CognitoDomainOptions.builder()
                .domainPrefix(app.getContext("loginPageDomainPrefix"))
                .build())
        .build();

    createOutputParameters(app);

    app.appEnv().tag(this);
  }

  private static final String PARAMETER_USER_POOL_ID = "userPoolId";
  private static final String PARAMETER_USER_POOL_CLIENT_ID = "userPoolClientId";
  private static final String PARAMETER_USER_POOL_CLIENT_SECRET = "userPoolClientSecret";
  private static final String PARAMETER_USER_POOL_LOGOUT_URL = "userPoolLogoutUrl";
  private static final String PARAMETER_USER_POOL_PROVIDER_URL = "userPoolProviderUrl";

  private void createOutputParameters(CDKApp app) {

    StringParameter.Builder.create(this, PARAMETER_USER_POOL_ID)
        .parameterName(createParameterName(app.appEnv(), PARAMETER_USER_POOL_ID))
        .stringValue(this.userPool.getUserPoolId())
        .build();

    StringParameter.Builder.create(this, PARAMETER_USER_POOL_CLIENT_ID)
        .parameterName(createParameterName(app.appEnv(), PARAMETER_USER_POOL_CLIENT_ID))
        .stringValue(this.userPoolClient.getUserPoolClientId())
        .build();

    StringParameter.Builder.create(this, "logoutUrl")
        .parameterName(createParameterName(app.appEnv(), PARAMETER_USER_POOL_LOGOUT_URL))
        .stringValue(this.logoutUrl)
        .build();

    StringParameter.Builder.create(this, "providerUrl")
        .parameterName(createParameterName(app.appEnv(), PARAMETER_USER_POOL_PROVIDER_URL))
        .stringValue(this.userPool.getUserPoolProviderUrl())
        .build();

    String userPoolClientSecret = this.userPoolClient.getUserPoolClientSecret().unsafeUnwrap();

    StringParameter.Builder.create(this, PARAMETER_USER_POOL_CLIENT_SECRET)
        .parameterName(createParameterName(app.appEnv(), PARAMETER_USER_POOL_CLIENT_SECRET))
        .stringValue(userPoolClientSecret)
        .build();
  }

  private static String createParameterName(
      ApplicationEnvironment applicationEnvironment, String parameterName) {
    return applicationEnvironment.getEnvironmentName()
        + "-"
        + applicationEnvironment.getApplicationName()
        + "-Cognito-"
        + parameterName;
  }

  public static CognitoOutputParameters getOutputParametersFromParameterStore(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return new CognitoOutputParameters(
        getParameterUserPoolId(scope, applicationEnvironment),
        getParameterUserPoolClientId(scope, applicationEnvironment),
        getParameterUserPoolClientSecret(scope, applicationEnvironment),
        getParameterLogoutUrl(scope, applicationEnvironment),
        getParameterUserPoolProviderUrl(scope, applicationEnvironment));
  }

  private static String getParameterUserPoolId(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_USER_POOL_ID,
            createParameterName(applicationEnvironment, PARAMETER_USER_POOL_ID))
        .getStringValue();
  }

  private static String getParameterLogoutUrl(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_USER_POOL_LOGOUT_URL,
            createParameterName(applicationEnvironment, PARAMETER_USER_POOL_LOGOUT_URL))
        .getStringValue();
  }

  private static String getParameterUserPoolProviderUrl(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_USER_POOL_PROVIDER_URL,
            createParameterName(applicationEnvironment, PARAMETER_USER_POOL_PROVIDER_URL))
        .getStringValue();
  }

  private static String getParameterUserPoolClientId(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_USER_POOL_CLIENT_ID,
            createParameterName(applicationEnvironment, PARAMETER_USER_POOL_CLIENT_ID))
        .getStringValue();
  }

  private static String getParameterUserPoolClientSecret(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_USER_POOL_CLIENT_SECRET,
            createParameterName(applicationEnvironment, PARAMETER_USER_POOL_CLIENT_SECRET))
        .getStringValue();
  }

  public record CognitoOutputParameters(
      String userPoolId,
      String userPoolClientId,
      String userPoolClientSecret,
      String logoutUrl,
      String providerUrl) {}
}
