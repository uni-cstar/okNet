package unics.oknet.okhttp.tls

import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object SystemDefaultTrustManager {

    @JvmStatic
    fun systemDefaultTrustManager(): X509TrustManager {
        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers

            if (trustManagers.size != 1 || (trustManagers[0] !is X509TrustManager)) {
                throw  IllegalStateException(
                    "Unexpected default trust managers:"
                            + trustManagers.contentToString()
                )
            }
            return trustManagers[0] as X509TrustManager
        } catch (e: GeneralSecurityException) {
            throw AssertionError("No System TLS", e); // The system has no TLS. Just give up.
        }
    }

    @JvmStatic
    fun systemDefaultSslSocketFactory(trustManager: X509TrustManager): SSLSocketFactory {
        try {
            val sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
            return sslContext.socketFactory
        } catch (e: GeneralSecurityException) {
            throw AssertionError("No System TLS", e); // The system has no TLS. Just give up.
        }
    }
}