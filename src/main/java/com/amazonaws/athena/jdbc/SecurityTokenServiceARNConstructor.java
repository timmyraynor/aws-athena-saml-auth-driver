package com.amazonaws.athena.jdbc;

public class SecurityTokenServiceARNConstructor {

  public static String getRoleARN(String account, String role) {
    return "arn:aws:iam::" + account + ":role/" + role;
  }

  public static String getPrincipleARN(String account, String provider) {
    return "arn:aws:iam::" + account + ":saml-provider/" + provider;
  }
}
