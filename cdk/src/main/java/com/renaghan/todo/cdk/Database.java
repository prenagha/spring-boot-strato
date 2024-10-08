package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ec2.CfnSecurityGroup;
import software.amazon.awscdk.services.rds.CfnDBInstance;
import software.amazon.awscdk.services.rds.CfnDBSubnetGroup;
import software.amazon.awscdk.services.secretsmanager.CfnSecretTargetAttachment;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

/**
 * Creates a Postgres database in the isolated subnets of a given VPC.
 *
 * <p>The following parameters need to exist in the SSM parameter store for this stack to
 * successfully deploy:
 *
 * <ul>
 *   <li><strong>&lt;environmentName&gt;-Network-vpcId:</strong> ID of the VPC to deploy the
 *       database into.
 *   <li><strong>&lt;environmentName&gt;-Network-isolatedSubnetOne:</strong> ID of the first
 *       isolated subnet to deploy the database into.
 *   <li><strong>&lt;environmentName&gt;-Network-isolatedSubnetTwo:</strong> ID of the first
 *       isolated subnet to deploy the database into.
 *   <li><strong>&lt;environmentName&gt;-Network-availabilityZoneOne:</strong> ID of the first AZ to
 *       deploy the database into.
 *   <li><strong>&lt;environmentName&gt;-Network-availabilityZoneTwo:</strong> ID of the second AZ
 *       to deploy the database into.
 * </ul>
 *
 * <p>The stack exposes the following output parameters in the SSM parameter store to be used in
 * other stacks:
 *
 * <ul>
 *   <li><strong>&lt;environmentName&gt;-&lt;applicationName&gt;-Database-endpointAddress:</strong>
 *       URL of the database
 *   <li><strong>&lt;environmentName&gt;-&lt;applicationName&gt;-Database-endpointPort:</strong>
 *       port to access the database
 *   <li><strong>&lt;environmentName&gt;-&lt;applicationName&gt;-Database-databaseName:</strong>
 *       name of the database
 *   <li><strong>&lt;environmentName&gt;-&lt;applicationName&gt;-Database-securityGroupId:</strong>
 *       ID of the database's security group
 *   <li><strong>&lt;environmentName&gt;-&lt;applicationName&gt;-Database-secretArn:</strong> ARN of
 *       the secret that stores the fields "username" and "password"
 *   <li><strong>&lt;environmentName&gt;-&lt;applicationName&gt;-Database-instanceId:</strong> ID of
 *       the database
 * </ul>
 *
 * The static getter methods provide a convenient access to retrieve these parameters from the
 * parameter store for use in other stacks.
 */
class Database {

  private static final String PARAMETER_ENDPOINT_ADDRESS = "endpointAddress";
  private static final String PARAMETER_ENDPOINT_PORT = "endpointPort";
  private static final String PARAMETER_DATABASE_NAME = "databaseName";
  private static final String PARAMETER_SECURITY_GROUP_ID = "securityGroupId";
  private static final String PARAMETER_SECRET_ARN = "secretArn";
  private static final String PARAMETER_INSTANCE_ID = "instanceId";

  private final CDKApp app;
  private final Stack stack;

  private final CfnSecurityGroup databaseSecurityGroup;
  private final CfnDBInstance dbInstance;
  private final ISecret databaseSecret;

  Database(
      CDKApp app, Stack stack, Network network, DatabaseInputParameters databaseInputParameters) {
    this.app = app;
    this.stack = stack;

    String username = sanitizeDbParameterName(app.appEnv().prefix("dbUser"));

    databaseSecurityGroup =
        CfnSecurityGroup.Builder.create(stack, "databaseSecurityGroup")
            .vpcId(network.getVpc().getVpcId())
            .groupDescription("Security Group for the database instance")
            .groupName(app.appEnv().prefix("dbSecurityGroup"))
            .build();

    // This will generate a JSON object with the keys "username" and "password".
    databaseSecret =
        Secret.Builder.create(stack, "databaseSecret")
            .secretName(app.appEnv().prefix("DatabaseSecret"))
            .description("Credentials to the RDS instance")
            .generateSecretString(
                SecretStringGenerator.builder()
                    .secretStringTemplate(String.format("{\"username\": \"%s\"}", username))
                    .generateStringKey("password")
                    .passwordLength(32)
                    .excludeCharacters("@/\\\" ")
                    .build())
            .build();

    CfnDBSubnetGroup subnetGroup =
        CfnDBSubnetGroup.Builder.create(stack, "dbSubnetGroup")
            .dbSubnetGroupDescription("Subnet group for the RDS instance")
            .dbSubnetGroupName(app.appEnv().prefix("dbSubnetGroup"))
            .subnetIds(
                Arrays.asList(
                    network.getVpc().getIsolatedSubnets().get(0).getSubnetId(),
                    network.getVpc().getIsolatedSubnets().get(1).getSubnetId()))
            .build();

    dbInstance =
        CfnDBInstance.Builder.create(stack, "postgresInstance")
            .dbInstanceIdentifier(app.appEnv().prefix("database"))
            .allocatedStorage(String.valueOf(databaseInputParameters.storageInGb))
            .availabilityZone(network.getVpc().getAvailabilityZones().get(0))
            .dbInstanceClass(databaseInputParameters.instanceClass)
            .dbName(sanitizeDbParameterName(app.appEnv().prefix("database")))
            .dbSubnetGroupName(subnetGroup.getDbSubnetGroupName())
            .engine("postgres")
            .engineVersion(databaseInputParameters.postgresVersion)
            .masterUsername(username)
            .masterUserPassword(databaseSecret.secretValueFromJson("password").unsafeUnwrap())
            .publiclyAccessible(false)
            .vpcSecurityGroups(Collections.singletonList(databaseSecurityGroup.getAttrGroupId()))
            .build();

    CfnSecretTargetAttachment.Builder.create(stack, "secretTargetAttachment")
        .secretId(databaseSecret.getSecretArn())
        .targetId(dbInstance.getRef())
        .targetType("AWS::RDS::DBInstance")
        .build();

    createOutputParameters();
  }

  CfnSecurityGroup getSecurityGroup() {
    return databaseSecurityGroup;
  }

  CfnDBInstance getDbInstance() {
    return dbInstance;
  }

  ISecret getDatabaseSecret() {
    return databaseSecret;
  }

  @NotNull
  private static String createParameterName(
      ApplicationEnvironment applicationEnvironment, String parameterName) {
    return applicationEnvironment.getEnvironmentName()
        + "-"
        + applicationEnvironment.getApplicationName()
        + "-Database-"
        + parameterName;
  }

  /**
   * Collects the output parameters of an already deployed {@link Database} construct from the
   * parameter store. This requires that a {@link Database} construct has been deployed previously.
   * If you want to access the parameters from the same stack that the {@link Database} construct is
   * in, use the plain {@link #getOutputParameters()} method.
   *
   * @param scope the construct in which we need the output parameters
   * @param environment the environment for which to load the output parameters. The deployed {@link
   *     Database} construct must have been deployed into this environment.
   */
  public static DatabaseOutputParameters getOutputParametersFromParameterStore(
      Construct scope, ApplicationEnvironment environment) {
    return new DatabaseOutputParameters(
        getEndpointAddress(scope, environment),
        getEndpointPort(scope, environment),
        getDbName(scope, environment),
        getDatabaseSecretArn(scope, environment),
        getDatabaseSecurityGroupId(scope, environment),
        getDatabaseIdentifier(scope, environment));
  }

  private static String getDatabaseIdentifier(Construct scope, ApplicationEnvironment environment) {
    return StringParameter.fromStringParameterName(
            scope, PARAMETER_INSTANCE_ID, createParameterName(environment, PARAMETER_INSTANCE_ID))
        .getStringValue();
  }

  private static String getEndpointAddress(Construct scope, ApplicationEnvironment environment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_ENDPOINT_ADDRESS,
            createParameterName(environment, PARAMETER_ENDPOINT_ADDRESS))
        .getStringValue();
  }

  private static String getEndpointPort(Construct scope, ApplicationEnvironment environment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_ENDPOINT_PORT,
            createParameterName(environment, PARAMETER_ENDPOINT_PORT))
        .getStringValue();
  }

  private static String getDbName(Construct scope, ApplicationEnvironment environment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_DATABASE_NAME,
            createParameterName(environment, PARAMETER_DATABASE_NAME))
        .getStringValue();
  }

  private static String getDatabaseSecretArn(Construct scope, ApplicationEnvironment environment) {
    String secretArn =
        StringParameter.fromStringParameterName(
                scope, PARAMETER_SECRET_ARN, createParameterName(environment, PARAMETER_SECRET_ARN))
            .getStringValue();
    return secretArn;
  }

  private static String getDatabaseSecurityGroupId(
      Construct scope, ApplicationEnvironment environment) {
    String securityGroupId =
        StringParameter.fromStringParameterName(
                scope,
                PARAMETER_SECURITY_GROUP_ID,
                createParameterName(environment, PARAMETER_SECURITY_GROUP_ID))
            .getStringValue();
    return securityGroupId;
  }

  /** Creates the outputs of this stack to be consumed by other stacks. */
  private void createOutputParameters() {

    StringParameter endpointAddress =
        StringParameter.Builder.create(stack, "endpointAddress")
            .parameterName(createParameterName(this.app.appEnv(), PARAMETER_ENDPOINT_ADDRESS))
            .stringValue(this.dbInstance.getAttrEndpointAddress())
            .build();

    StringParameter endpointPort =
        StringParameter.Builder.create(stack, "endpointPort")
            .parameterName(createParameterName(this.app.appEnv(), PARAMETER_ENDPOINT_PORT))
            .stringValue(this.dbInstance.getAttrEndpointPort())
            .build();

    StringParameter databaseName =
        StringParameter.Builder.create(stack, "databaseName")
            .parameterName(createParameterName(this.app.appEnv(), PARAMETER_DATABASE_NAME))
            .stringValue(this.dbInstance.getDbName())
            .build();

    StringParameter securityGroupId =
        StringParameter.Builder.create(stack, "securityGroupId")
            .parameterName(createParameterName(this.app.appEnv(), PARAMETER_SECURITY_GROUP_ID))
            .stringValue(this.databaseSecurityGroup.getAttrGroupId())
            .build();

    StringParameter secret =
        StringParameter.Builder.create(stack, "secret")
            .parameterName(createParameterName(this.app.appEnv(), PARAMETER_SECRET_ARN))
            .stringValue(this.databaseSecret.getSecretArn())
            .build();

    StringParameter instanceId =
        StringParameter.Builder.create(stack, "instanceId")
            .parameterName(createParameterName(this.app.appEnv(), PARAMETER_INSTANCE_ID))
            .stringValue(this.dbInstance.getDbInstanceIdentifier())
            .build();
  }

  private String sanitizeDbParameterName(String dbParameterName) {
    return dbParameterName
        // db name must have only alphanumerical characters
        .replaceAll("[^a-zA-Z0-9]", "")
        // db name must start with a letter
        .replaceAll("^[0-9]", "a");
  }

  /** Collects the output parameters that other constructs might be interested in. */
  public DatabaseOutputParameters getOutputParameters() {
    return new DatabaseOutputParameters(
        this.dbInstance.getAttrEndpointAddress(),
        this.dbInstance.getAttrEndpointPort(),
        this.dbInstance.getDbName(),
        this.databaseSecret.getSecretArn(),
        this.databaseSecurityGroup.getAttrGroupId(),
        this.dbInstance.getDbInstanceIdentifier());
  }

  public static class DatabaseInputParameters {
    private int storageInGb = 20;
    private String instanceClass = "db.t2.micro";
    private String postgresVersion = "12.9";

    /**
     * The storage allocated for the database in GB.
     *
     * <p>Default: 20.
     */
    public DatabaseInputParameters withStorageInGb(int storageInGb) {
      this.storageInGb = storageInGb;
      return this;
    }

    /**
     * The class of the database instance.
     *
     * <p>Default: "db.t2.micro".
     */
    public DatabaseInputParameters withInstanceClass(String instanceClass) {
      Objects.requireNonNull(instanceClass);
      this.instanceClass = instanceClass;
      return this;
    }

    /**
     * The version of the PostGres database.
     *
     * <p>Default: "11.5".
     */
    public DatabaseInputParameters withPostgresVersion(String postgresVersion) {
      Objects.requireNonNull(postgresVersion);
      this.postgresVersion = postgresVersion;
      return this;
    }
  }

  public record DatabaseOutputParameters(
      String endpointAddress,
      String endpointPort,
      String dbName,
      String databaseSecretArn,
      String databaseSecurityGroupId,
      String instanceId) {}
}
