package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.DockerRepository;
import dev.stratospheric.cdk.Network;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

@SuppressWarnings("deprecation")
public class Infrastructure {
  private final CDKApp app;
  private final Stack stack;

  private IHostedZone hostedZone;
  private String certARN;
  private Network network;

  public Infrastructure() {
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

  private void generate() {
    dockerRepo();
    cert();
    network();
    dns();
    cognito();
    app.synth();
  }

  public static void main(String[] args) {
    new Infrastructure().generate();
  }
}
