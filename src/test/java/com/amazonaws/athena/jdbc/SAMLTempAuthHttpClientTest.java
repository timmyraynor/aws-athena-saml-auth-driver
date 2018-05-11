package com.amazonaws.athena.jdbc;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

@Ignore
public class SAMLTempAuthHttpClientTest {


  private SAMLTempAuthPostAuthClient client = new SAMLTempAuthPostAuthClient();


  @Test
  public void testClientPerformSAMLTokenRetrieval()
      throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
    String samlTokenViaAD = client.getSAMLTokenViaAD("yourusername", "SERVIAN", "xxxxxx",
        "https://fs.servian.com/adfs/ls/idpinitiatedsignon",
        "c08f8391-9898-e511-80cf-06d610463ba8");
    assertNotNull(samlTokenViaAD);
  }
}