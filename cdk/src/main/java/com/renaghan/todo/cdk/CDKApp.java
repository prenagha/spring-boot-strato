package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

/** CDK App */
class CDKApp extends App {

  protected String getApplicationDomain() {
    String val = (String) getNode().tryGetContext("applicationDomain");
    //noinspection ConstantValue
    if (val == null || val.isBlank())
      throw new IllegalStateException("applicationDomain is required in cdk.json context");
    ApplicationEnvironment appEnv = appEnv();
    if ("prod".equals(appEnv.getEnvironmentName())) return val;
    return appEnv.getEnvironmentName() + "." + val;
  }

  protected String getApplicationURL() {
    return "https://" + getApplicationDomain();
  }

  protected String getContext(String name) {
    if ("applicationDomain".equals(name))
      throw new IllegalStateException("Use getApplicationDomain() method");

    String val = (String) getNode().tryGetContext(name);
    //noinspection ConstantValue
    if (val == null || val.isBlank())
      throw new IllegalStateException(name + " is required in cdk.json context");
    return val;
  }

  protected ApplicationEnvironment appEnv() {
    return new ApplicationEnvironment(getContext("applicationName"), getContext("environmentName"));
  }

  protected Environment awsEnv() {
    return Environment.builder()
        .account(getContext("accountId"))
        .region(getContext("region"))
        .build();
  }
}
