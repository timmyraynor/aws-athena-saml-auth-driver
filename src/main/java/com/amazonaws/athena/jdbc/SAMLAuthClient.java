package com.amazonaws.athena.jdbc;

import java.io.IOException;

public interface SAMLAuthClient {

  String getSAMLTokenViaAD(String adUserName, String adDomain, String adPassword,
      String baseUrl, String relyingParty) throws IOException;
}
