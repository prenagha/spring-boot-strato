package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.DockerRepository;
import dev.stratospheric.cdk.Network;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableEncryption;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;

@SuppressWarnings("deprecation")
public class Infrastructure {
  private final CDKApp app;
  private final Stack stack;

  private IHostedZone hostedZone;
  private String certARN;
  private Network network;
  private Database database;

  Infrastructure() {
    this.app = new CDKApp();
    this.stack =
        new Stack(
            app,
            "infra",
            StackProps.builder()
                .stackName(app.getContext("applicationName") + "-infra")
                .env(app.awsEnv())
                .build());
  }

  private void dockerRepo() {
    new DockerRepository(
        stack,
        "DockerRepository",
        app.awsEnv(),
        new DockerRepository.DockerRepositoryInputParameters(
            app.getContext("applicationName"), app.awsEnv().getAccount()));
  }

  private void cert() {
    this.hostedZone =
        HostedZone.fromLookup(
            stack,
            "HostedZone",
            HostedZoneProviderProps.builder()
                .domainName(app.getContext("hostedZoneDomain"))
                .build());

    DnsValidatedCertificate websiteCertificate =
        DnsValidatedCertificate.Builder.create(stack, "WebsiteCertificate")
            .hostedZone(hostedZone)
            .region(app.awsEnv().getRegion())
            .domainName(app.getApplicationDomain())
            .build();

    this.certARN = websiteCertificate.getCertificateArn();
  }

  private void dns() {
    ARecord.Builder.create(stack, "ARecord")
        .recordName(app.getApplicationDomain())
        .zone(hostedZone)
        .target(RecordTarget.fromAlias(new LoadBalancerTarget(network.getLoadBalancer())))
        .build();
  }

  private void network() {
    this.network =
        new Network(
            stack,
            "Network",
            app.awsEnv(),
            app.getContext("environmentName"),
            new Network.NetworkInputParameters().withSslCertificateArn(certARN));
  }

  private void cognito() {
    new Cognito(app, stack);
  }

  private void database() {
    this.database =
        new Database(
            app,
            stack,
            network,
            new Database.DatabaseInputParameters()
                .withPostgresVersion("16.4")
                .withInstanceClass("db.t4g.micro"));
  }

  private void messaging() {
    Queue todoSharingDlq =
        Queue.Builder.create(stack, "todoSharingDlq")
            .queueName(app.appEnv().prefix("todo-sharing-dead-letter-queue"))
            .retentionPeriod(Duration.days(14))
            .build();

    Queue.Builder.create(stack, "todoSharingQueue")
        .queueName(app.appEnv().prefix("todo-sharing-queue"))
        .visibilityTimeout(Duration.seconds(30))
        .retentionPeriod(Duration.days(14))
        .deadLetterQueue(DeadLetterQueue.builder().queue(todoSharingDlq).maxReceiveCount(3).build())
        .build();
  }

  private void activeMQ() {
    new ActiveMQ(app, stack, network);
  }

  private void dynamoDB() {
    new Table(
        stack,
        "BreadcrumbsDynamoDbTable",
        TableProps.builder()
            .partitionKey(Attribute.builder().type(AttributeType.STRING).name("id").build())
            .tableName(app.appEnv().prefix("breadcrumb"))
            .encryption(TableEncryption.AWS_MANAGED)
            .billingMode(BillingMode.PROVISIONED)
            .readCapacity(10)
            .writeCapacity(10)
            .removalPolicy(RemovalPolicy.DESTROY)
            .build());
  }

  private void cloudwatch() {
    new CloudWatchDashboard(app, stack, network, database);
    new CloudWatchAlarms(app, stack, network);
  }

  private void generate() {
    dockerRepo();
    cert();
    network();
    dns();
    cognito();
    messaging();
    database();
    activeMQ();
    dynamoDB();
    cloudwatch();
    app.appEnv().tag(stack);
    app.synth();
  }

  public static void main(String[] args) {
    new Infrastructure().generate();
  }
}
