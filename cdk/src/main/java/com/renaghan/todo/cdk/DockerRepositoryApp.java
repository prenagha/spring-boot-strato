package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.DockerRepository;
import java.util.Objects;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/** CDK App */
public class DockerRepositoryApp {
  public static void main(String[] args) {
    App app = new App();
    Environment awsEnvironment = makeEnv(app);

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    Objects.requireNonNull(applicationName, "Application Name is required");

    Stack dockerRepositoryStack =
        new Stack(
            app,
            "DockerRepositoryStack",
            StackProps.builder()
                .stackName(applicationName + "-DockerRepository")
                .env(awsEnvironment)
                .build());

    DockerRepository dockerRepository =
        new DockerRepository(
            dockerRepositoryStack,
            "DockerRepository",
            awsEnvironment,
            new DockerRepository.DockerRepositoryInputParameters(
                applicationName, awsEnvironment.getAccount()));

    app.synth();
  }

  static Environment makeEnv(App app) {
    String accountId = (String) app.getNode().tryGetContext("accountId");
    Objects.requireNonNull(accountId, "Account Id is required");
    String region = (String) app.getNode().tryGetContext("region");
    Objects.requireNonNull(region, "Region is required");
    return Environment.builder().account(accountId).region(region).build();
  }
}
