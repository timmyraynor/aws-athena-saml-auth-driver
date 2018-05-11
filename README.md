# aws-athena-saml-auth-driver
This is a project which wraps up the AWS Athena driver and provide extra layer of SAML auth to get the connection rather than using AccessID and Secret Key.
The original requirement for this project is to provide a Athena Driver for Tableau Server to connect to Athena with SAML auth-ed AD credentials.

## Concept and dependencies
The core idea is to utilise the Athena driver options to point to a custom credential provider which could resolve SAML Auth issues and get a STS token to authenticate user to AWS using SAML auth.

Please refer to the release note for AWS Athena driver options.

From the version 1.X documentation (https://docs.aws.amazon.com/athena/latest/ug/connect-with-previous-jdbc.html), we have:

| Property Name | Description| Default Value| Is Required |
|---------------|------------|--------------|-------------|
| s3_staging_dir | The S3 location to which your query output is written, for example s3://query-results-bucket/folder/, which is established under Settings in the Athena Console, https://console.aws.amazon.com/athena/. The JDBC driver then asks Athena to read the results and provide rows of data back to the user. | N/A | Yes |
| query_results_encryption_option | The encryption method to use for the directory specified by s3_staging_dir. If not specified, the location is not encrypted. Valid values are SSE_S3, SSE_KMS, and CSE_KMS. | N/A | No |
| query_results_aws_kms_key | The Key ID of the AWS customer master key (CMK) to use if query_results_encryption_option specifies SSE-KMS or CSE-KMS. For example, 123abcde-4e56-56f7-g890-1234h5678i9j. | N/A | No |
| aws_credentials_provider_class | The credentials provider class name, which implements the AWSCredentialsProvider interface. | N/A | No |
| aws_credentials_provider_arguments | Arguments for the credentials provider constructor as comma-separated values. | N/A | No |
| max_error_retries | The maximum number of retries that the JDBC client attempts to make a request to Athena. | 10 | No |
| connection_timeout |  The maximum amount of time, in milliseconds, to make a successful connection to Athena before an attempt is terminated. | 10,000 | No |
| socket_timeout | The maximum amount of time, in milliseconds, to wait for a socket in order to send data to Athena. | 10,000 | No |
| retry_base_delay | Minimum delay amount, in milliseconds, between retrying attempts to connect Athena. | 100 | No |
| retry_max_backoff_time | Maximum delay amount, in milliseconds, between retrying attempts to connect to Athena. | 1000 | No|
| log_path | Local path of the Athena JDBC driver logs. If no log path is provided, then no log files are created. | N/A | No|
| log_level | Log level of the Athena JDBC driver logs. Valid values: INFO, DEBUG, WARN, ERROR, ALL, OFF, FATAL, TRACE. | N/A |No|


the `aws_credentials_provider_class` and `aws_credentials_provider_arguments` could be utilised to point to a custom credential provider, this is also where the `SAMLIntegratedADAWSSessionCredentialsProvider` could be plugged in.

From the version 2.X Release notes(https://s3.amazonaws.com/athena-downloads/drivers/JDBC/SimbaAthenaJDBC_2.0.2/docs/release-notes.txt), we have the following options available as well:

- s3_staging_dir: alias for S3OutputLocation
- query_results_encryption_option: alias for S3OutputEncOption
- query_results_aws_kms_key: alias for S3OutputEncKMSKey
- aws_credentials_provider_class: alias for AwsCredentialsProviderClass
- aws_credentials_provider_arguments: alias for AwsCredentialsProviderArguments
- max_error_retries: alias for MaxErrorRetry
- connection_timeout (time in milliseconds): alias for ConnectTimeout (time in seconds)
- socket_timeout (time in milliseconds): alias for SocketTimeout (time in seconds)

The provider class will point to the SAML Auth credential provider, and the `aws_credentials_provider_arguments` will be used to feed in all required parameters.

## Build and wrap up
As Athena driver is still required and not open source, the way to build this is to build this with a manual downloaded Athena Driver.

Here I used a maven trick to put custom driver into `.m2/repositories` corresponding folders base on the artifact details:

    <dependency>
      <groupId>com.amazonaws.athena.jdbc</groupId>
      <artifactId>AthenaJDBC41</artifactId>
      <version>1.1.0</version>
    </dependency>

I could then download the jar and put them into `.m2/repositories/com/amazonaws/athena/jdbc/AthenaJDBC41/1.1.0`. If you are using the 2.X athena driver, please fix the path and maven identifier accordingly.
If you are using Artifactory or some other local maven repositories, you could create the server path for this driver instead.

Once built, the target jar could serve as a individual driver for uses such as Tableau required JDBC Driver with some extra properties. This will be discussed in the next section.

* Note: On the other hand if we have the flexibility of accessing the existing classpath, please remove the athena driver classpath and build this jar, put the jar together with the Athena driver into the correponding classpath.

## Provide the options and setup
As the options only allow string to be the `aws_credentials_provider_arguments`, we could only pass the path to the configuration file as a string for easier config value management, e.g.

    aws_credentials_provider_class=com.amazonaws.athena.jdbc.SAMLAuthADIntegratedAWSSessionCredentialsProvider
    aws_credentials_provider_arguments="mytest,/tmp/mycred.properties"

The arguments consist of 2 parts:

- profile prefix (which is `mytest` in this case)
- credentials.properties file (which stores the credentials for SAML Auth)

A sample credentials file will look like below:

    username=
    password=
    domain=
    baseurl=https://adfs.mydomain.net

    mytest.account=XXXXX1231221
    mytest.role=cloud-admin
    mytest.provider=adfs
    mytest.relyingparty=XXXXXXXX-XXXX-XXXX-XXXX-XXXX

Where the `baseurl` is the internal ADFS link. `mytest` is the profile prefix which allows user to put multiple SAML profiles into the credentials file and choose the right one in the `aws_credentials_provider_arguments` options.

## Where to specify the options - Tableau Specific
This is really flexible considering different ways of specifying the `aws_credentials_provider_class` and `aws_credentials_provider_arguments` options. But for Tableau, there's a specific `athena.properties` file which you could use to put in those attributes. An example of the file would look like:

    log_path=/tmp/athena.log
    log_level=DEBUG
    aws_credentials_provider_class=com.amazonaws.athena.jdbc.SAMLAuthADIntegratedAWSSessionCredentialsProvider
    aws_credentials_provider_arguments="mytest,/tmp/mycred.properties"

Please refer to (http://kb.tableau.com/articles/howto/Customizing-JDBC-Connections) about customizing Tableau Athena JDBC Connector driver.