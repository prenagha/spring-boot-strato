package com.renaghan.todo.cdk;

import dev.stratospheric.cdk.DockerRepository;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

/**
 * CDK stack
 *
 * @author padraic
 */
public class InfraStack extends Stack {
  public InfraStack(@Nullable Construct scope, @Nullable String id, @Nullable StackProps props) {
    super(scope, id, props);

    String accountId = "xxx";
    Environment env = Environment.builder().account(accountId).region("us-east-2").build();
    DockerRepository repo =
        new DockerRepository(
            this,
            "repo",
            env,
            new DockerRepository.DockerRepositoryInputParameters(
                "hello-world-repo", accountId, 10));
  }

  public InfraStack(@Nullable Construct scope, @Nullable String id) {
    super(scope, id);
  }
}
