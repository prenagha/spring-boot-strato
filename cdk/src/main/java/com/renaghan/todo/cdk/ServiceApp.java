package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.Service;
import java.util.Map;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/** CDK App */
public class ServiceApp {
  public static void main(String[] args) {
    CDKApp app = new CDKApp();

    Stack serviceStack =
        new Stack(
            app,
            "ServiceStack",
            StackProps.builder()
                .stackName(
                    app.appEnv().getEnvironmentName()
                        + "-"
                        + app.appEnv().getApplicationName()
                        + "-Service")
                .env(app.awsEnv())
                .build());

    Network.NetworkOutputParameters networkOutputParameters =
        Network.getOutputParametersFromParameterStore(
            serviceStack, app.appEnv().getEnvironmentName());

    /*
    CognitoStack.CognitoOutputParameters cognitoOutputParameters =
        CognitoStack.getOutputParametersFromParameterStore(serviceStack, app.appEnv());
    Map<String, String> vars =
        environmentVariables(app.getContext("springProfile"), cognitoOutputParameters);
    vars.put("SPRING_PROFILES_ACTIVE", springProfile);
    vars.put("COGNITO_CLIENT_ID", cognitoOutputParameters.userPoolClientId());
    vars.put("COGNITO_CLIENT_SECRET", cognitoOutputParameters.userPoolClientSecret());
    vars.put("COGNITO_USER_POOL_ID", cognitoOutputParameters.userPoolId());
    vars.put("COGNITO_LOGOUT_URL", cognitoOutputParameters.logoutUrl());
    vars.put("COGNITO_PROVIDER_URL", cognitoOutputParameters.providerUrl());
     */

    Map<String, String> vars = Map.of();

    Service.ServiceInputParameters inputParameters =
        new Service.ServiceInputParameters(
                new Service.DockerImageSource(
                    app.getContext("dockerRepositoryName"), app.getContext("dockerImageTag")),
                vars)
            .withHealthCheckPath("/mgmt/health");

    new Service(
        serviceStack,
        "Service",
        app.awsEnv(),
        app.appEnv(),
        inputParameters,
        networkOutputParameters);

    app.synth();
  }
}
