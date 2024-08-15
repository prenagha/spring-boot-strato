package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.DockerRepository;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/** CDK App */
public class DockerRepositoryApp {
  public static void main(String[] args) {
    CDKApp app = new CDKApp();
    Stack dockerRepositoryStack =
        new Stack(
            app,
            "DockerRepositoryStack",
            StackProps.builder()
                .stackName(app.getContext("applicationName") + "-DockerRepository")
                .env(app.awsEnv())
                .build());
    new DockerRepository(
        dockerRepositoryStack,
        "DockerRepository",
        app.awsEnv(),
        new DockerRepository.DockerRepositoryInputParameters(
            app.getContext("applicationName"), app.awsEnv().getAccount()));
    app.synth();
  }
}
