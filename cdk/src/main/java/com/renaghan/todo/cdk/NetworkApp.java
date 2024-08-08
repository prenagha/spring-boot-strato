package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.Network;
import java.util.Objects;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/** CDK App */
public class NetworkApp {
  public static void main(String[] args) {
    App app = new App();
    Environment awsEnvironment = DockerRepositoryApp.makeEnv(app);

    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    Objects.requireNonNull(environmentName, "Environment Name is required");

    Stack networkStack =
        new Stack(
            app,
            "NetworkStack",
            StackProps.builder()
                .stackName(environmentName + "-Network")
                .env(awsEnvironment)
                .build());

    Network network =
        new Network(
            networkStack,
            "Network",
            awsEnvironment,
            environmentName,
            new Network.NetworkInputParameters());
    app.synth();
  }
}
