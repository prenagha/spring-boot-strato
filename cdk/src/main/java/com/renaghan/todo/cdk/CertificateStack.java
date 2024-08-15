package com.renaghan.todo.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.ssm.StringParameter;

@SuppressWarnings("deprecation")
public class CertificateStack extends Stack {
  private static final String PARAMETER_CERT_ARN = "sslCertARN";

  CertificateStack(CDKApp app, String id) {
    super(
        app,
        id,
        StackProps.builder().stackName(app.appEnv().prefix("SSLCert")).env(app.awsEnv()).build());

    IHostedZone hostedZone =
        HostedZone.fromLookup(
            this,
            "HostedZone",
            HostedZoneProviderProps.builder()
                .domainName(app.getContext("hostedZoneDomain"))
                .build());

    DnsValidatedCertificate websiteCertificate =
        DnsValidatedCertificate.Builder.create(this, "WebsiteCertificate")
            .hostedZone(hostedZone)
            .region(app.awsEnv().getRegion())
            .domainName(app.getApplicationDomain())
            .build();

    StringParameter.Builder.create(this, PARAMETER_CERT_ARN)
        .parameterName(app.appEnv().prefix(PARAMETER_CERT_ARN))
        .stringValue(websiteCertificate.getCertificateArn())
        .build();
  }

  static CertificateStack.CertificateOutputParameters getOutputParametersFromParameterStore(
      CDKApp app) {
    String sslCertARN =
        StringParameter.fromStringParameterName(
                app, PARAMETER_CERT_ARN, app.appEnv().prefix(PARAMETER_CERT_ARN))
            .getStringValue();

    return new CertificateStack.CertificateOutputParameters(sslCertARN);
  }

  public record CertificateOutputParameters(String sslCertARN) {}
}
