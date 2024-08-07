package com.renaghan.todo.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

/** CDK App */
public class Infra {
  public static void main(String[] args) {
    App app = new App();
    new InfraStack(app, "TodoStack", StackProps.builder().build());
    app.synth();
  }
}
