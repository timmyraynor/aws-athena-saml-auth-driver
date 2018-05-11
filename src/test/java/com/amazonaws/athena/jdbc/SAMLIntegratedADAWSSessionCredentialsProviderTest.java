package com.amazonaws.athena.jdbc;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class SAMLIntegratedADAWSSessionCredentialsProviderTest {


  @Test
  public void prepareSAMLAWSSTS() {
    SAMLIntegratedADAWSSessionCredentialsProvider credentialsProvider = new SAMLIntegratedADAWSSessionCredentialsProvider(
         "/tmp/mycred.properties");


  }
}