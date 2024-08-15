package com.renaghan.todo.cdk;

/** CDK App */
public class DomainApp {
  public static void main(String[] args) {
    CDKApp app = new CDKApp();
    new DomainStack(app, "domain");
    app.synth();
  }
}
