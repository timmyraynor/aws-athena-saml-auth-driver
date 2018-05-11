package com.amazonaws.athena.jdbc;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithSAMLRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Resolver to pass the SAML Request to AWS and get the temporary sts credentials back
 *
 * This is a profile sensitive resolver and it will try to get the arn details from the
 * properties file in the format of:
 *
 *        <profilename>.account= xxxxx
 *        <profilename>.role= xxxxx
 *        <profilename>.provider= xxxxx
 *
 * Please refer to the sample.mycred.properties for further details
 */
public class AssumeRoleWithSAMLResolver {

  private AWSSecurityTokenServiceClient stsService;
  private static final Logger log = LogManager.getLogger(AssumeRoleWithSAMLResolver.class);

  public AssumeRoleWithSAMLResolver() {
    log.setLevel(Level.DEBUG);
    this.stsService = new AWSSecurityTokenServiceClient();
    log.info("Initialising SAMLResolver......");
  }

  public BasicSessionCredentials getSAMLAuthResult(Properties properties, String profile,
      String SAMLToken) {
    String account = properties.getProperty(profile + "." + "account");
    String role = properties.getProperty(profile + "." + "role");
    String provider = properties.getProperty(profile + "." + "provider");

    String principalARN = SecurityTokenServiceARNConstructor.getPrincipleARN(account, provider);
    String roleARN = SecurityTokenServiceARNConstructor.getRoleARN(account, role);
    log.debug("Sending token:" + SAMLToken);

    AssumeRoleWithSAMLRequest samlRequest = new AssumeRoleWithSAMLRequest()
        .withPrincipalArn(principalARN)
        .withRoleArn(roleARN)
        .withSAMLAssertion(SAMLToken);

    Credentials tempCredentials = stsService.assumeRoleWithSAML(samlRequest).getCredentials();

    return new BasicSessionCredentials(tempCredentials.getAccessKeyId(),
        tempCredentials.getSecretAccessKey(),
        tempCredentials.getSessionToken());
  }

}
