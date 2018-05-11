package com.amazonaws.athena.jdbc;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 *  This is the main provider class we could use to get the AD->SAMLAuth token sorted out
 *
 *  We are trying to stay away from changing the actual AthenaJDBC jars, instead we are using the
 *  parameters provided from AthenaJDBC jar as follow:
 *
 *       -  aws_credentials_provider_class
 *       -  aws_credentials_provider_arguments
 *
 *  The aws_credentials_provider_class will point to this class
 *  The aws_credentials_provider_arguments will have 2 parameters:
 *       - Profile name: as they are required to select the right profile
 *       - Path to the credentials properties file
 *
 *  An example of the above arguments will be
 *         "itc-test,/tmp/mycred.properties"
 *
 *  You should be able to use the sample.mycred.properties in this project as an example
 */
public class SAMLIntegratedADAWSSessionCredentialsProvider implements AWSCredentialsProvider {

  private final AWSCredentials credentials;

  private AssumeRoleWithSAMLResolver samlResolver;

  private SAMLAuthClient samlTempAuthHttpClient;

  private static final Logger log = LogManager
      .getLogger(SAMLIntegratedADAWSSessionCredentialsProvider.class);

  public SAMLIntegratedADAWSSessionCredentialsProvider(String filePath) {
    log.setLevel(Level.DEBUG);

    this.samlResolver = new AssumeRoleWithSAMLResolver();
    this.samlTempAuthHttpClient = new SAMLTempAuthPostAuthClient();

    Properties props = new Properties();
    try {
      InputStream inputStream = new FileInputStream(filePath);
      props.load(inputStream);
    } catch (IOException e) {
      log.debug(e.getMessage());
      throw new RuntimeException(e);
    }

    log.info("Loading configurations from server.....");
    String baseUrl = props.getProperty("baseurl");
    String profileName = props.getProperty("profile");
    String relyingParty = props.getProperty(profileName + "." + "relyingparty");
    BasicSessionCredentials samlAuthResult = null;
    try {
      String samlTokenViaAD = samlTempAuthHttpClient
          .getSAMLTokenViaAD(props.getProperty("username"), props.getProperty("domain"),
              props.getProperty("password"), baseUrl, relyingParty);
      samlAuthResult = samlResolver
          .getSAMLAuthResult(props, profileName, samlTokenViaAD);
    } catch (IOException e) {
      log.error(e.getMessage());
      e.printStackTrace();
    }
    this.credentials = samlAuthResult;
  }

  public AWSCredentials getCredentials() {
    return this.credentials;
  }

  public void refresh() {

  }
}
