package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.amazonmq.CfnBroker;
import software.amazon.awscdk.services.ec2.CfnSecurityGroup;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

class ActiveMQ {

  private static final String PARAMETER_USERNAME = "activeMqUsername";
  private static final String PARAMETER_PASSWORD = "activeMqPassword";
  private static final String PARAMETER_AMQP_ENDPOINT = "amqpEndpoint";
  private static final String PARAMETER_STOMP_ENDPOINT = "stompEndpoint";
  private static final String PARAMETER_SECURITY_GROUP_ID = "activeMqSecurityGroupId";

  private final CDKApp app;
  private final Stack stack;
  private final CfnBroker broker;
  private final String username;
  private final String password;
  private final String securityGroupId;

  ActiveMQ(CDKApp app, Stack stack, Network network) {
    this.app = app;
    this.stack = stack;

    this.username = app.getContext("activeMqUsername");
    this.password = generatePassword();

    List<User> userList = new ArrayList<>();
    userList.add(new User(username, password));

    CfnSecurityGroup amqSecurityGroup =
        CfnSecurityGroup.Builder.create(stack, "amqSecurityGroup")
            .vpcId(network.getVpc().getVpcId())
            .groupDescription("Security Group for the Amazon MQ instance")
            .groupName(app.appEnv().prefix("amqSecurityGroup"))
            .build();

    this.securityGroupId = amqSecurityGroup.getAttrGroupId();

    this.broker =
        CfnBroker.Builder.create(stack, "amqBroker")
            .brokerName(app.appEnv().prefix("stratospheric-amq-message-broker"))
            .securityGroups(Collections.singletonList(this.securityGroupId))
            .subnetIds(
                Collections.singletonList(
                    network.getVpc().getIsolatedSubnets().get(0).getSubnetId()))
            .hostInstanceType("mq.t3.micro")
            .engineType("ACTIVEMQ")
            .engineVersion("5.18")
            .authenticationStrategy("SIMPLE")
            .encryptionOptions(
                CfnBroker.EncryptionOptionsProperty.builder().useAwsOwnedKey(true).build())
            .users(userList)
            .publiclyAccessible(false)
            .autoMinorVersionUpgrade(true)
            .deploymentMode("SINGLE_INSTANCE")
            .logs(CfnBroker.LogListProperty.builder().general(true).build())
            .build();

    createOutputParameters();
  }

  public static ActiveMqOutputParameters getOutputParametersFromParameterStore(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return new ActiveMqOutputParameters(
        getParameterUsername(scope, applicationEnvironment),
        getParameterPassword(scope, applicationEnvironment),
        getParameterAmqpEndpoint(scope, applicationEnvironment),
        getParameterStompEndpoint(scope, applicationEnvironment),
        getParameterSecurityGroupId(scope, applicationEnvironment));
  }

  private static String getParameterUsername(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_USERNAME,
            createParameterName(applicationEnvironment, PARAMETER_USERNAME))
        .getStringValue();
  }

  private static String getParameterPassword(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_PASSWORD,
            createParameterName(applicationEnvironment, PARAMETER_PASSWORD))
        .getStringValue();
  }

  private static String getParameterAmqpEndpoint(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_AMQP_ENDPOINT,
            createParameterName(applicationEnvironment, PARAMETER_AMQP_ENDPOINT))
        .getStringValue();
  }

  private static String getParameterStompEndpoint(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_STOMP_ENDPOINT,
            createParameterName(applicationEnvironment, PARAMETER_STOMP_ENDPOINT))
        .getStringValue();
  }

  private static String getParameterSecurityGroupId(
      Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(
            scope,
            PARAMETER_SECURITY_GROUP_ID,
            createParameterName(applicationEnvironment, PARAMETER_SECURITY_GROUP_ID))
        .getStringValue();
  }

  private String generatePassword() {
    PasswordGenerator passwordGenerator = new PasswordGenerator();
    CharacterData lowerCaseChars = EnglishCharacterData.LowerCase;
    CharacterRule lowerCaseRule = new CharacterRule(lowerCaseChars);
    lowerCaseRule.setNumberOfCharacters(5);
    CharacterData upperCaseChars = EnglishCharacterData.UpperCase;
    CharacterRule upperCaseRule = new CharacterRule(upperCaseChars);
    upperCaseRule.setNumberOfCharacters(5);
    CharacterData digitChars = EnglishCharacterData.Digit;
    CharacterRule digitRule = new CharacterRule(digitChars);
    digitRule.setNumberOfCharacters(5);
    return passwordGenerator.generatePassword(32, lowerCaseRule, upperCaseRule, digitRule);
  }

  private void createOutputParameters() {
    StringParameter.Builder.create(stack, PARAMETER_USERNAME)
        .parameterName(createParameterName(app.appEnv(), PARAMETER_USERNAME))
        .stringValue(username)
        .build();

    StringParameter.Builder.create(stack, PARAMETER_PASSWORD)
        .parameterName(createParameterName(app.appEnv(), PARAMETER_PASSWORD))
        .stringValue(password)
        .build();

    StringParameter.Builder.create(stack, PARAMETER_AMQP_ENDPOINT)
        .parameterName(createParameterName(app.appEnv(), PARAMETER_AMQP_ENDPOINT))
        .stringValue(Fn.select(0, this.broker.getAttrAmqpEndpoints()))
        .build();

    StringParameter.Builder.create(stack, PARAMETER_STOMP_ENDPOINT)
        .parameterName(createParameterName(app.appEnv(), PARAMETER_STOMP_ENDPOINT))
        .stringValue(Fn.select(0, this.broker.getAttrStompEndpoints()))
        .build();

    StringParameter.Builder.create(stack, PARAMETER_SECURITY_GROUP_ID)
        .parameterName(createParameterName(app.appEnv(), PARAMETER_SECURITY_GROUP_ID))
        .stringValue(this.securityGroupId)
        .build();
  }

  private static String createParameterName(
      ApplicationEnvironment applicationEnvironment, String parameterName) {
    return applicationEnvironment.getEnvironmentName()
        + "-"
        + applicationEnvironment.getApplicationName()
        + "-ActiveMq-"
        + parameterName;
  }

  public record ActiveMqOutputParameters(
      String activeMqUsername,
      String activeMqPassword,
      String amqpEndpoint,
      String stompEndpoint,
      String activeMqSecurityGroupId) {}

  @SuppressWarnings("unused")
  static class User {

    String username;

    String password;

    public User() {}

    public User(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
