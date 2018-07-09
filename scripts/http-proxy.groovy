import com.boomi.component.CertificateFactory as BoomiCertificateFactory
import com.boomi.component.ComponentFactory as BoomiComponentFactory
import com.boomi.model.platform.Component as BoomiComponent
import com.boomi.document.scripting.DataContextImpl
import com.boomi.execution.ExecutionManager
import com.boomi.execution.ExecutionTask
import com.boomi.function.lookup.CrossRefLookup
import com.boomi.model.platform.CrossRefExtensionUtil

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore
import java.security.cert.Certificate

// set up constants
int BUFFER_SIZE = 1024 * 4 // 4 KB

def INHEADER_PREFIX = 'document.dynamic.userdefined.inheader_'
int INHEADER_PREFIX_LEN = INHEADER_PREFIX.length()

def QUERY_PREFIX = 'query_'
int QUERY_PREFIX_LEN = QUERY_PREFIX.length()

// clear the cookie handler. Otherwise, the Set-Cookie header will be blanked out
CookieHandler.setDefault(null);

// gather scripting environment variables
ExecutionTask task = ExecutionManager.current

Properties execProps = task.properties
DataContextImpl context = dataContext

String HTTP_METHOD = execProps.getProperty('inmethod')

// load user configurable variables
String HTTP_PROXY_URL_BASE = execProps.getProperty('http_proxy_url_base')
String CERT_ID = execProps.getProperty('http_proxy_trusted_certificate_id')
String SSL_PROTOCOL = execProps.getProperty('http_proxy_ssl_protocol')

// reassemble the URL from the process properties
pathParts = []

1.upto(9) { n ->
  def pathPart = execProps.getProperty("param_path$n")
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

URL url = new URL(HTTP_PROXY_URL_BASE + '/' + path)

// setup use of the trusted SSL certificate
boolean USE_TRUSTED_CERT = url.protocol == 'https' && CERT_ID != null && CERT_ID.size()
//SSLContext sc = SSLContext.getInstance(SSL_PROTOCOL)
//if (USE_TRUSTED_CERT) {
//  KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType())
//  ks.load(null, null)
//
//  Certificate cert = BoomiCertificateFactory.getInstance(task.accountConfig)
//    .getPublicCertificate(CERT_ID, task.directory)
//  ks.setCertificateEntry(url.host, cert)
//
//  TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
//  tmf.init(ks)
//
//  sc.init(null, tmf.getTrustManagers(), null)
//}
SSLContext sc = BoomiCertificateFactory
  .getInstance(task.accountConfig)
  .createSSLContext(null, USE_TRUSTED_CERT ? CERT_ID : null, task.directory)

// perform the http request for each document
context.getDataCount().times { i ->
  InputStream docData = context.getStream(i)
  Properties docProps = context.getProperties(i)

  HttpURLConnection conn = url.openConnection() as HttpURLConnection

  // set custom certificate
  if (USE_TRUSTED_CERT) {
    HttpsURLConnection sconn = conn as HttpsURLConnection
    sconn.SSLSocketFactory = sc.socketFactory
  }

  conn.requestMethod = HTTP_METHOD

  // copy request headers from properties
  for (entry in docProps.entrySet()) {
    String k = entry.key
    if (k.startsWith(INHEADER_PREFIX)) {
      conn.setRequestProperty(k.substring(INHEADER_PREFIX_LEN), entry.value as String)
    }
  }

  // set request body
  if (conn.requestMethod != 'GET' && conn.requestMethod != 'HEAD') {
    conn.doOutput = true
    OutputStream requestBody = conn.outputStream
    // copy the document's data into the http request's outputStream
    byte[] buffer = new byte[BUFFER_SIZE]
    int len
    while ((len = docData.read(buffer)) != -1) {
      requestBody.write(buffer, 0, len)
    }
  }

  // retrieve response, set response code
  docProps.setProperty('document.dynamic.userdefined.outstatuscode', "${conn.responseCode}")

  // set response headers
  Map<String, List<String>> headerFields = conn.headerFields
  for (entry in headerFields.entrySet()) {
    String k = entry.key
    List<String> v = entry.value
    if (k != null && v.size() > 0) {
      docProps.setProperty("document.dynamic.userdefined.outheader_$k", v.get(0))
    }
  }

  // set response body
  InputStream respStream
  try {
    respStream = conn.getInputStream()
  } catch (IOException ignored) {
    respStream = conn.getErrorStream()
  }
  dataContext.storeStream(respStream, docProps)
}
