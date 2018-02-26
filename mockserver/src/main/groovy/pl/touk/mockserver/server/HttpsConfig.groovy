package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsParameters
import groovy.transform.CompileStatic
import pl.touk.mockserver.api.common.Https

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

@CompileStatic
class HttpsConfig extends HttpsConfigurator {
    private final Https https

    HttpsConfig(SSLContext sslContext, Https https) {
        super(sslContext)
        this.https = https
    }

    @Override
    void configure(HttpsParameters httpsParameters) {
        SSLContext sslContext = getSSLContext()
        SSLParameters sslParameters = sslContext.defaultSSLParameters
        sslParameters.needClientAuth = https.requireClientAuth
        httpsParameters.needClientAuth = https.requireClientAuth
        httpsParameters.SSLParameters = sslParameters
    }
}
