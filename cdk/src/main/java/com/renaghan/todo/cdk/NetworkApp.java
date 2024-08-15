package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.Network;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/** CDK App */
public class NetworkApp {
  public static void main(String[] args) {
    CDKApp app = new CDKApp();
    Stack networkStack =
        new Stack(
            app,
            "NetworkStack",
            StackProps.builder()
                .stackName(app.getContext("environmentName") + "-Network")
                .env(app.awsEnv())
                .build());
    CertificateStack.CertificateOutputParameters certOutput =
        CertificateStack.getOutputParametersFromParameterStore(app, networkStack);
    new Network(
        networkStack,
        "Network",
        app.awsEnv(),
        app.getContext("environmentName"),
        new Network.NetworkInputParameters().withSslCertificateArn(certOutput.sslCertARN()));
    app.synth();
  }
}
