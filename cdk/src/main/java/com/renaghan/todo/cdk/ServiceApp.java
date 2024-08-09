package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.Service;
import java.util.Map;
import java.util.Objects;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/** CDK App */
public class ServiceApp {
  public static void main(String[] args) {
    App app = new App();
    Environment awsEnvironment = DockerRepositoryApp.makeEnv(app);

    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    Objects.requireNonNull(environmentName, "Environment Name is required");

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    Objects.requireNonNull(applicationName, "Application Name is required");

    String dockerRepositoryName = (String) app.getNode().tryGetContext("dockerRepositoryName");
    Objects.requireNonNull(dockerRepositoryName, "Docker Repository Name is required");

    String dockerImageTag = (String) app.getNode().tryGetContext("dockerImageTag");
    Objects.requireNonNull(dockerRepositoryName, "Docker Image Tag is required");

    ApplicationEnvironment applicationEnvironment =
        new ApplicationEnvironment(applicationName, environmentName);

    Stack serviceStack =
        new Stack(
            app,
            "ServiceStack",
            StackProps.builder()
                .stackName(environmentName + "-" + applicationName + "-Service")
                .env(awsEnvironment)
                .build());

    Network.NetworkOutputParameters networkOutputParameters =
        Network.getOutputParametersFromParameterStore(
            serviceStack, applicationEnvironment.getEnvironmentName());

    Service service =
        new Service(
            serviceStack,
            "Service",
            awsEnvironment,
            applicationEnvironment,
            new Service.ServiceInputParameters(
                new Service.DockerImageSource(dockerRepositoryName, dockerImageTag), Map.of()),
            networkOutputParameters);

    app.synth();
  }
}
