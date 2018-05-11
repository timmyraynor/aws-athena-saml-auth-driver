package com.amazonaws.athena.jdbc;

import com.amazonaws.athena.jdbc.shaded.org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;


/**
 * SAML Auth client we used for NBN AWS SAML Auth, where we need to:
 *
 *   - initiate using /adfs/ls/idpinitiatedsignon
 *   - NTLM Auth using adusername adpw addomain to /adfs/ls/wia using Windows Integration Auth (WIA)
 *   - the previous step will return with the SAML Auth
 */
public class SAMLTempAuthWIAAuthClient implements SAMLAuthClient {

  private static final Logger log = LogManager
      .getLogger(SAMLIntegratedADAWSSessionCredentialsProvider.class);
  public static final String USER_AGENT_STRING = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36";

  public String getSAMLTokenViaAD(String adUserName, String adDomain, String adPassword,
      String baseUrl, String relyingParty)
      throws IOException {
    Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
        .register(AuthSchemes.NTLM, new NTLMSchemeFactory())
        .register(AuthSchemes.BASIC, new BasicSchemeFactory())
        .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
        .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
        .register(AuthSchemes.KERBEROS, new KerberosSchemeFactory())
        .build();

    CloseableHttpClient client = HttpClientBuilder.create()
        .setDefaultAuthSchemeRegistry(authSchemeRegistry)
        .build();
    HttpPost post = new HttpPost(baseUrl + "/adfs/ls/idpinitiatedsignon");

    List<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair("SignInOtherSite", "SignInOtherSite"));
    params.add(new BasicNameValuePair("RelyingParty", relyingParty));
    params.add(new BasicNameValuePair("SignInGo", "Sign in"));
    params.add(new BasicNameValuePair("SingleSignOut", "SingleSignOut"));

    HttpClientContext ctx = HttpClientContext.create();
    Credentials credentials = new NTCredentials(adUserName, adPassword, "workstation", adDomain);
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, credentials);
    ctx.setCredentialsProvider(credentialsProvider);

    post.setHeader("User-Agent", USER_AGENT_STRING);
    post.setEntity(new UrlEncodedFormEntity(params));
    client.execute(post, ctx);

    HttpGet samlGet = new HttpGet(baseUrl + "/adfs/ls/wia");
    samlGet.setHeader("User-Agent", USER_AGENT_STRING);

    String xmlString = EntityUtils.toString(client.execute(samlGet, ctx).getEntity());

    return parseHttpResponse(xmlString);
  }

  private String parseHttpResponse(String xmlString)
      throws IOException {
    // TODO: Implement solutions for parsing the httpResponse body and get the actual SAML token out
    System.out.println(xmlString);
    XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();
    try {
      XPathExpression expr = xpath.compile("/html/body/form/input[@name=\"SAMLResponse\"]/@value");
      String result = expr.evaluate(new InputSource(new StringReader(xmlString)));
      return result;
    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }
    return StringUtils.EMPTY;
  }
}
