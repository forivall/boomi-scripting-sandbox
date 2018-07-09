import com.boomi.component.CertificateFactory as BoomiCertificateFactory
import com.boomi.document.scripting.DataContextImpl
import com.boomi.execution.ExecutionManager
import com.boomi.execution.ExecutionTask
import com.boomi.execution.ExecutionUtil

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate

def logger = ExecutionUtil.getBaseLogger()

// set up constants
int BUFFER_SIZE = 1024 * 4 // 4 KB

def INHEADER_PREFIX = 'document.dynamic.userdefined.inheader_'
int INHEADER_PREFIX_LEN = INHEADER_PREFIX.length()

def QUERY_PREFIX = 'query_'
int QUERY_PREFIX_LEN = QUERY_PREFIX.length()

CookieHandler.setDefault(null);

// gather scripting environment variables
def task = ExecutionManager.current

Properties execProps = task.properties
DataContextImpl context = dataContext

String HTTP_METHOD = execProps.getProperty('inmethod')

// load user configurable variables
String HTTP_PROXY_URL_BASE = execProps.getProperty('http_proxy_url_base')
String CERT_ID = execProps.getProperty('http_proxy_trusted_certificate_id')
CERT_ID = null

logger.info("HTTP_METHOD: " + HTTP_METHOD)
logger.info("HTTP_PROXY_URL_BASE: " + HTTP_PROXY_URL_BASE)
logger.info("CERT_ID: " + CERT_ID)

// reassemble the URL from the process properties
pathParts = []

1.upto(9) { n ->
  pathPart = execProps.getProperty("param_path$n")
  if (pathPart != null && pathPart.size() > 0) {
    pathParts << pathPart
  }
}

String path = pathParts.join('/')
logger.info("path: " + path)

queryParams = []

for (entry in execProps.entrySet()) {
  String k = entry.key
  String v = entry.value as String
  if (k.startsWith(QUERY_PREFIX)) {
    if (v == null || v.size() == 0) {
      queryParams << k.substring(QUERY_PREFIX_LEN)
    } else {
      queryParams << k.substring(QUERY_PREFIX_LEN) + '=' + URLEncoder.encode(v, 'UTF-8')
    }
  }
}

if (queryParams.size() > 0) {
  path += '?' + queryParams.join('&')
}

def urlString = HTTP_PROXY_URL_BASE + '/' + path
logger.info("urlString: " + urlString)

URL url = new URL(urlString)

logger.info("loading certificate...")
// setup use of the trusted SSL certificate
SSLContext sc = SSLContext.getInstance("TLS")
boolean HAS_TRUSTED_CERT = CERT_ID != null && CERT_ID.size()
boolean IS_HTTPS = url.protocol == 'https'
boolean USE_TRUSTED_CERT = IS_HTTPS && HAS_TRUSTED_CERT

class TrustAllHostnameVerifier implements HostnameVerifier {
  static logger = ExecutionUtil.getBaseLogger()
  public boolean verify(String hostname, SSLSession session) {
    logger.info("loading certificate...")
    return true;
  }
}

class TrustAllTrustManager implements X509TrustManager {
  static logger = ExecutionUtil.getBaseLogger()
  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    logger.info("getAcceptedIssuers")
    return null;
  }
  public void checkClientTrusted(X509Certificate[] certs, String authType) {
    logger.info("checkClientTrusted")
  }
  public void checkServerTrusted(X509Certificate[] certs, String authType) {
    logger.info("checkServerTrusted")
  }
}

HostnameVerifier hv = HAS_TRUSTED_CERT ? null : new TrustAllHostnameVerifier()
if (IS_HTTPS) {
  KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType())
  ks.load(null, null)

  TrustManager[] trustManagers
  if (HAS_TRUSTED_CERT) {
    Certificate cert = BoomiCertificateFactory.getInstance(task.accountConfig)
      .getPublicCertificate(CERT_ID, task.directory)
    ks.setCertificateEntry(url.host, cert)

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    tmf.init(ks)
    trustManagers = tmf.getTrustManagers()
  } else {
    trustManagers = [new TrustAllTrustManager()];
  }
  sc.init(null, trustManagers, null)
}

logger.info("loaded certificate.")

// perform the http request for each document
context.getDataCount().times { i ->
  InputStream docData = context.getStream(i)
  Properties docProps = context.getProperties(i)

  HttpURLConnection conn = url.openConnection() as HttpURLConnection

  // set custom certificate
  if (IS_HTTPS) {
    HttpsURLConnection sconn = conn as HttpsURLConnection
    sconn.SSLSocketFactory = sc.socketFactory
    if (hv != null) sconn.hostnameVerifier = hv
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

  // logger.info("skipping request...")
  //
  // context.storeStream(docData, docProps)
  // return

  logger.info("making request...")

  // retrieve response, set response code
  def responseCode = conn.responseCode

  logger.info("completed request")
  docProps.setProperty('document.dynamic.userdefined.outstatuscode', "${responseCode}")

  // set response headers
  Map<String, List<String>> headerFields = conn.headerFields
  for (entry in headerFields.entrySet()) {
    String k = entry.key
    List<String> v = entry.value
    if (k != null && v.size() > 0) {
      docProps.setProperty("document.dynamic.userdefined.outheader_$k", v.get(0))
      if (v.size() > 1) {
        for (int j in 0..<v.size()) {
          docProps.setProperty("document.dynamic.userdefined.outheader_${k}_${j}", v.get(j))
        }
      }
    }
  }
  def setCookieHeaders = headerFields.get('Set-Cookie')
  if (setCookieHeaders != null) {
    for (int ci in 0..<setCookieHeaders.size()) {
      def setCookieHeader = setCookieHeaders.get(ci)
      execProps.setProperty('setCookieHeader_' + ci, setCookieHeader)
    }
  }


  logger.info("reading response request...")
  // set response body
  InputStream respStream
  try {
    respStream = conn.getInputStream()
  } catch (IOException ignored) {
    respStream = conn.getErrorStream()
  }
  context.storeStream(respStream, docProps)
}
