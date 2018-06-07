import com.boomi.component.CertificateFactory as BoomiCertificateFactory
import com.boomi.document.scripting.DataContextImpl
import com.boomi.execution.ExecutionManager
import com.boomi.execution.ExecutionTask

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory

String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n" +
  "MIIFkDCCBHigAwIBAgITGAAAAKsM3MVd6IgoBQAAAAAAqzANBgkqhkiG9w0BAQsF\n" +
  "ADBJMRMwEQYKCZImiZPyLGQBGRYDbmV0MRYwFAYKCZImiZPyLGQBGRYGbGVkY29y\n" +
  "MRowGAYDVQQDExFMZWRjb3ItSXNzdWluZy1DQTAeFw0xNjA3MTIyMzE1MTBaFw0x\n" +
  "ODA3MTIyMzE1MTBaMGExCzAJBgNVBAYTAkNBMQswCQYDVQQIEwJCQzESMBAGA1UE\n" +
  "BxMJVmFuY291dmVyMQ8wDQYDVQQKEwZMZWRjb3IxDDAKBgNVBAsTA09IUzESMBAG\n" +
  "A1UEAxMJbG9handsLWxiMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
  "mJSbcQx60a0qV96L6+FOWv/Enb0TcruglpMP48ePC+SlVMv7uCe4B4OUeNcKx6hs\n" +
  "djbTv9hPc0WdlWtQbAStg7W5gU1r0wqpSirbwtOLKOV5k0gzCoExnS7dVzxSjVou\n" +
  "+YQeWYtmUxEBibiO/fr1JfDaVASU3UrFkLNUz3Ws0Z/XHllWAKD63QOjqBJnekWY\n" +
  "dO1SxK/vtith41e0/SFJgDwWT1bigQdxrWhrP7UUSP35CstwbyyJ7OUeVQczQj2c\n" +
  "TmSm+ef284fOCQDPYC7r6qxx3hfMY3b6DPISC7CBCW0lDyMxv0M3wgsvMDtGT/Ei\n" +
  "HxG3xp515oFYzJnXSzk9KQIDAQABo4ICVzCCAlMwHQYDVR0OBBYEFB4GgphXXODH\n" +
  "4cnEVi2CfZA7qwWQMB8GA1UdIwQYMBaAFCxzoT8HnyhROc/Z6Oldzjw3cSA4MIHO\n" +
  "BgNVHR8EgcYwgcMwgcCggb2ggbqGgbdsZGFwOi8vL0NOPUxlZGNvci1Jc3N1aW5n\n" +
  "LUNBLENOPUxDQVZTRTEsQ049Q0RQLENOPVB1YmxpYyUyMEtleSUyMFNlcnZpY2Vz\n" +
  "LENOPVNlcnZpY2VzLENOPUNvbmZpZ3VyYXRpb24sREM9bGVkY29yLERDPW5ldD9j\n" +
  "ZXJ0aWZpY2F0ZVJldm9jYXRpb25MaXN0P2Jhc2U/b2JqZWN0Q2xhc3M9Y1JMRGlz\n" +
  "dHJpYnV0aW9uUG9pbnQwgcIGCCsGAQUFBwEBBIG1MIGyMIGvBggrBgEFBQcwAoaB\n" +
  "omxkYXA6Ly8vQ049TGVkY29yLUlzc3VpbmctQ0EsQ049QUlBLENOPVB1YmxpYyUy\n" +
  "MEtleSUyMFNlcnZpY2VzLENOPVNlcnZpY2VzLENOPUNvbmZpZ3VyYXRpb24sREM9\n" +
  "bGVkY29yLERDPW5ldD9jQUNlcnRpZmljYXRlP2Jhc2U/b2JqZWN0Q2xhc3M9Y2Vy\n" +
  "dGlmaWNhdGlvbkF1dGhvcml0eTALBgNVHQ8EBAMCBaAwPAYJKwYBBAGCNxUHBC8w\n" +
  "LQYlKwYBBAGCNxUI7+AmhPDEcoetnxeC+5JKgqThGk2Em4wnhbPsJQIBZAIBDDAT\n" +
  "BgNVHSUEDDAKBggrBgEFBQcDATAbBgkrBgEEAYI3FQoEDjAMMAoGCCsGAQUFBwMB\n" +
  "MA0GCSqGSIb3DQEBCwUAA4IBAQBIzM4bjclb1DfEQas6fEidK71e167mdFv3G4zm\n" +
  "C8WZr2LnHVyqRajVzgmHKEeY7LWHoJDQOkBl+06wVTxHlWAq/6EXTQ2gBjKLjlQt\n" +
  "aiDqKJjf/lqOwdVl8HUq5Gn8tDvWYYWBC4Du8GwtsVklkKAPLFlMCgskl28Ch+/d\n" +
  "VxHOsGKaKh8oX1OlTv8+IXRJUaH6msnkYdoKvce2ot6J3JYIrsY7nsOi4hZrqOzh\n" +
  "W+feeWZWqIFyyj7xCwjEqC+lIXd1AozF/qwXhyB2LCKescP4/lOtM2o3DcQuPom7\n" +
  "oXh0xLbRv8RDuiFO4BNyvTYnD3pu2ixXleeXH6TA0lsdFuIv\n" +
  "-----END CERTIFICATE-----"

String CERT_ID = '14840107-c77b-4c79-8a01-81df9659d796'
String CERT_FOLDER = '#Common Components'

ExecutionTask task = ExecutionManager.current
Properties execProps = task.properties
DataContextImpl context = dataContext

def INHEADER_PREFIX = 'document.dynamic.userdefined.inheader_'
int INHEADER_PREFIX_LEN = INHEADER_PREFIX.length()

def QUERY_PREFIX = 'query_'
int QUERY_PREFIX_LEN = QUERY_PREFIX.length()

// reassemble the URL
pathParts = []

1.upto(9) { n ->
  pathPart = execProps.getProperty("param_path$n")
  if (pathPart != null && pathPart.size() > 0) {
    pathParts << pathPart
  }
}

String path = pathParts.join('/')

queryParams = []

for (entry in execProps.entrySet()) {
  String k = entry.key
  String v = entry.value as String
  if (k.startsWith(QUERY_PREFIX)) {
    queryParams << k.substring(QUERY_PREFIX_LEN) + '=' + URLEncoder.encode(v, 'UTF-8')
  }
}

if (queryParams.size() > 0) {
  path += '?' + queryParams.join('&')
}

String fullPath = (execProps.getProperty('http_proxy_url_base') as String) + '/' + path
URL url = new URL(fullPath)
String httpMethod = execProps.getProperty('inmethod')

SSLContext sc = SSLContext.getInstance("SSLv3")
if (url.protocol == 'https') {
  KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType())
  ks.load(null, null)

  CertificateFactory cf = CertificateFactory.getInstance('X.509')
  Certificate cert = cf.generateCertificate(new ByteArrayInputStream(CERTIFICATE.bytes))

  // BoomiCertificateFactory bcf = BoomiCertificateFactory.getInstance(task.accountConfig)
  // Certificate cert = bcf.getPublicCertificate(CERT_ID, CERT_FOLDER)

  ks.setCertificateEntry(url.host, cert)

  TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
  tmf.init(ks)

  sc.init(null, tmf.getTrustManagers(), null)
}

context.getDataCount().times { i ->
  InputStream is = dataContext.getStream(i)
  Properties docProps = dataContext.getProperties(i)

  HttpURLConnection conn = url.openConnection() as HttpURLConnection

  if (url.protocol == 'https') {
    HttpsURLConnection sconn = conn as HttpsURLConnection

    sconn.SSLSocketFactory = sc.socketFactory
  }

  conn.requestMethod = httpMethod


  for (entry in docProps.entrySet()) {
    String k = entry.key
    if (k.startsWith(INHEADER_PREFIX)) {
      conn.setRequestProperty(k.substring(INHEADER_PREFIX_LEN), entry.value as String)
    }
  }

  if (conn.requestMethod != 'GET' && conn.requestMethod != 'HEAD') {
    conn.doOutput = true
    OutputStream os = conn.getOutputStream()
    // pipe the inputStream to the outputStream
    byte[] buffer = new byte[1024]
    int len
    while ((len = is.read(buffer)) != -1) {
      os.write(buffer, 0, len)
    }
  }

  // done request
  docProps.setProperty('document.dynamic.userdefined.outstatuscode', "${conn.responseCode}")


  Map<String, List<String>> headerFields = conn.headerFields
  for (entry in headerFields.entrySet()) {
    String k = entry.key
    List<String> v = entry.value
    if (k != null && v.size() > 0) {
      docProps.setProperty("document.dynamic.userdefined.outheader_$k", v.get(0))
    }
  }

  InputStream connIs
  try {
    connIs = conn.getInputStream()
  } catch (IOException err) {
    connIs = conn.getErrorStream()
  }
  dataContext.storeStream(connIs, docProps)
}
