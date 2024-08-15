package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.Network;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancerAttributes;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationLoadBalancer;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

public class DomainStack extends Stack {

  DomainStack(CDKApp app, String id) {
    super(
        app,
        id,
        StackProps.builder().stackName(app.appEnv().prefix("DNS")).env(app.awsEnv()).build());

    IHostedZone hostedZone =
        HostedZone.fromLookup(
            this,
            "HostedZone",
            HostedZoneProviderProps.builder()
                .domainName(app.getContext("hostedZoneDomain"))
                .build());

    Network.NetworkOutputParameters networkOutputParameters =
        Network.getOutputParametersFromParameterStore(this, app.appEnv().getEnvironmentName());

    IApplicationLoadBalancer applicationLoadBalancer =
        ApplicationLoadBalancer.fromApplicationLoadBalancerAttributes(
            this,
            "LoadBalancer",
            ApplicationLoadBalancerAttributes.builder()
                .loadBalancerArn(networkOutputParameters.getLoadBalancerArn())
                .securityGroupId(networkOutputParameters.getLoadbalancerSecurityGroupId())
                .loadBalancerCanonicalHostedZoneId(
                    networkOutputParameters.getLoadBalancerCanonicalHostedZoneId())
                .loadBalancerDnsName(networkOutputParameters.getLoadBalancerDnsName())
                .build());

    ARecord.Builder.create(this, "ARecord")
        .recordName(app.getApplicationDomain())
        .zone(hostedZone)
        .target(RecordTarget.fromAlias(new LoadBalancerTarget(applicationLoadBalancer)))
        .build();
  }
}
