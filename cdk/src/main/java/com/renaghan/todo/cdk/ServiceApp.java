package com.renaghan.todo.cdk;

import static java.util.Collections.singletonList;

import dev.stratospheric.cdk.Network;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;

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

    Map<String, String> vars = new HashMap<>();
    vars.put("ENVIRONMENT_NAME", app.appEnv().getEnvironmentName());
    vars.put("SPRING_PROFILES_ACTIVE", app.getContext("springProfile"));

    Cognito.CognitoOutputParameters cognitoOutputParameters =
        Cognito.getOutputParametersFromParameterStore(serviceStack, app.appEnv());
    vars.put("COGNITO_CLIENT_ID", cognitoOutputParameters.userPoolClientId());
    vars.put("COGNITO_CLIENT_SECRET", cognitoOutputParameters.userPoolClientSecret());
    vars.put("COGNITO_USER_POOL_ID", cognitoOutputParameters.userPoolId());
    vars.put("COGNITO_LOGOUT_URL", cognitoOutputParameters.logoutUrl());
    vars.put("COGNITO_PROVIDER_URL", cognitoOutputParameters.providerUrl());

    Database.DatabaseOutputParameters databaseOutputParameters =
        Database.getOutputParametersFromParameterStore(serviceStack, app.appEnv());
    ISecret databaseSecret =
        Secret.fromSecretCompleteArn(
            serviceStack, "databaseSecret", databaseOutputParameters.databaseSecretArn());
    vars.put(
        "SPRING_DATASOURCE_URL",
        String.format(
            "jdbc:postgresql://%s:%s/%s",
            databaseOutputParameters.endpointAddress(),
            databaseOutputParameters.endpointPort(),
            databaseOutputParameters.dbName()));
    vars.put(
        "SPRING_DATASOURCE_USERNAME", databaseSecret.secretValueFromJson("username").toString());

    vars.put(
        "SPRING_DATASOURCE_PASSWORD", databaseSecret.secretValueFromJson("password").toString());

    ActiveMQ.ActiveMqOutputParameters activeMqOutputParameters =
        ActiveMQ.getOutputParametersFromParameterStore(serviceStack, app.appEnv());

    vars.put("WEB_SOCKET_RELAY_ENDPOINT", activeMqOutputParameters.stompEndpoint());
    vars.put("WEB_SOCKET_RELAY_USERNAME", activeMqOutputParameters.activeMqUsername());
    vars.put("WEB_SOCKET_RELAY_PASSWORD", activeMqOutputParameters.activeMqPassword());

    List<String> securityGroupIdsToGrantIngressFromEcs =
        Arrays.asList(
            databaseOutputParameters.databaseSecurityGroupId(),
            activeMqOutputParameters.activeMqSecurityGroupId());

    ServiceStack.ServiceInputParameters inputParameters =
        new ServiceStack.ServiceInputParameters(
                new ServiceStack.DockerImageSource(
                    app.getContext("dockerRepositoryName"), app.getContext("dockerImageTag")),
                securityGroupIdsToGrantIngressFromEcs,
                vars)
            .withCpu(512)
            .withMemory(1024)
            .withTaskRolePolicyStatements(
                List.of(
                    PolicyStatement.Builder.create()
                        .sid("AllowSQSAccess")
                        .effect(Effect.ALLOW)
                        .resources(
                            List.of(
                                String.format(
                                    "arn:aws:sqs:%s:%s:%s",
                                    app.getContext("region"),
                                    app.getContext("accountId"),
                                    app.appEnv().prefix("todo-sharing-queue"))))
                        .actions(
                            Arrays.asList(
                                "sqs:DeleteMessage",
                                "sqs:GetQueueUrl",
                                "sqs:ListDeadLetterSourceQueues",
                                "sqs:ListQueues",
                                "sqs:ListQueueTags",
                                "sqs:ReceiveMessage",
                                "sqs:SendMessage",
                                "sqs:ChangeMessageVisibility",
                                "sqs:GetQueueAttributes"))
                        .build(),
                    PolicyStatement.Builder.create()
                        .sid("AllowCreatingUsers")
                        .effect(Effect.ALLOW)
                        .resources(
                            List.of(
                                String.format(
                                    "arn:aws:cognito-idp:%s:%s:userpool/%s",
                                    app.getContext("region"),
                                    app.getContext("accountId"),
                                    cognitoOutputParameters.userPoolId())))
                        .actions(List.of("cognito-idp:AdminCreateUser"))
                        .build(),
                    PolicyStatement.Builder.create()
                        .sid("AllowSendingEmails")
                        .effect(Effect.ALLOW)
                        .resources(
                            List.of(
                                String.format(
                                    "arn:aws:ses:%s:%s:identity/renaghan.com",
                                    app.getContext("region"), app.getContext("accountId")),
                                String.format(
                                    "arn:aws:ses:%s:%s:configuration-set/*",
                                    app.getContext("region"), app.getContext("accountId"))))
                        .actions(List.of("ses:SendEmail", "ses:SendRawEmail"))
                        .build(),
                    PolicyStatement.Builder.create()
                        .sid("AllowDynamoTableAccess")
                        .effect(Effect.ALLOW)
                        .resources(
                            List.of(
                                String.format(
                                    "arn:aws:dynamodb:%s:%s:table/%s",
                                    app.getContext("region"),
                                    app.getContext("accountId"),
                                    app.appEnv().prefix("breadcrumb"))))
                        .actions(
                            List.of(
                                "dynamodb:Scan",
                                "dynamodb:Query",
                                "dynamodb:PutItem",
                                "dynamodb:GetItem",
                                "dynamodb:BatchWriteItem",
                                "dynamodb:BatchWriteGet"))
                        .build(),
                    PolicyStatement.Builder.create()
                        .sid("AllowSendingMetricsToCloudWatch")
                        .effect(Effect.ALLOW)
                        // CloudWatch does not have any resource-level permissions,
                        // see https://stackoverflow.com/a/38055068/9085273
                        .resources(singletonList("*"))
                        .actions(singletonList("cloudwatch:PutMetricData"))
                        .build()))
            .withStickySessionsEnabled(true)
            .withHealthCheckPath("/mgmt/health")
            // needs to be long enough to allow for slow start up with low-end computing instances
            .withHealthCheckIntervalSeconds(30);

    new ServiceStack(
        serviceStack,
        "Service",
        app.awsEnv(),
        app.appEnv(),
        inputParameters,
        networkOutputParameters);

    app.appEnv().tag(serviceStack);

    app.synth();
  }
}
