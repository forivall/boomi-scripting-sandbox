import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


// URL url = new URL("https://integration-dv910.ledsyn.local:7594/jderest/defaultconfig")
httpMethod = 'GET'
URL url = new URL("https://loajwl-lb:8688/jderest/defaultconfig")

SSLContext.getDefault()
SSLContext sc = SSLContext.getInstance("SSLv3")

// Create all-trusting host name verifier
class TrustAllHostnameVerifierReq implements HostnameVerifier {
  public boolean verify(String hostname, SSLSession session) {
    return true;
  }
}

class TrustAllTrustManagerReq implements X509TrustManager {
  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    return null;
  }
  public void checkClientTrusted(X509Certificate[] certs, String authType) {
  }
  public void checkServerTrusted(X509Certificate[] certs, String authType) {
  }
}

HostnameVerifier hv = new TrustAllHostnameVerifierReq()
if (url.protocol == 'https') {
  KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType())
  ks.load(null, null)
  // Create a trust manager that does not validate certificate chains
  TrustManager[] trustAllCerts = [new TrustAllTrustManagerReq()];

  sc.init(null, trustAllCerts, null)
}

HttpURLConnection conn = url.openConnection() as HttpURLConnection

if (url.protocol == 'https') {
  HttpsURLConnection sconn = conn as HttpsURLConnection

  sconn.SSLSocketFactory = sc.socketFactory
  sconn.hostnameVerifier = hv
}

conn.requestMethod = httpMethod

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

InputStream connIs
try {
  connIs = conn.getInputStream()
} catch (IOException err) {
  connIs = conn.getErrorStream()
}

inStr = conn.getInputStream()
byte[] buffer = new byte[1024]
int len
while ((len = inStr.read(buffer)) != -1) {
  System.out.write(buffer, 0, len)
}
