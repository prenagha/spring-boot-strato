package com.renaghan.todo.cdk;

/** CDK App */
public class CognitoApp {
  public static void main(String[] args) {
    CDKApp app = new CDKApp();
    new CognitoStack(
        app,
        "Cognito",
        app.awsEnv(),
        app.appEnv(),
        new CognitoStack.CognitoInputParameters(
            app.getContext("applicationName"),
            app.getContext("applicationUrl"),
            app.getContext("loginPageDomainPrefix")));
    app.synth();
  }
}
