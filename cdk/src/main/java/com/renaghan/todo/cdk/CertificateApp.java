package com.renaghan.todo.cdk;

/** CDK App */
public class CertificateApp {
  public static void main(String[] args) {
    CDKApp app = new CDKApp();
    new CertificateStack(app, "certificate");
    app.synth();
  }
}
