package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsServer
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import pl.touk.mockserver.api.common.Https

import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.Executor

@Slf4j
@PackageScope
class HttpServerWrapper {
    private final HttpServer httpServer
    final int port

    private List<ContextExecutor> executors = []

    HttpServerWrapper(int port, Executor executor, Https https = null) {
        this.port = port
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), port)
        httpServer = buildServer(addr, https)
        httpServer.executor = executor
        log.info("Http server starting on port $port...")
        httpServer.start()
        log.info('Http server is started')
    }

    private HttpServer buildServer(InetSocketAddress addr, Https https) {
        if (https) {
            HttpsServer httpsServer = HttpsServer.create(addr, 0)
            httpsServer.httpsConfigurator = new HttpsConfig(buildSslContext(https), https)
            return httpsServer
        } else {
            return HttpServer.create(addr, 0)
        }
    }

    private SSLContext buildSslContext(Https https) {
        KeyManager[] keyManagers = buildKeyManager(https)
        TrustManager[] trustManagers = buildTrustManager(https)

        SSLContext ssl = SSLContext.getInstance('TLSv1')
        ssl.init(keyManagers, trustManagers, new SecureRandom())
        return ssl
    }

    private KeyManager[] buildKeyManager(Https https) {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.defaultType)
        keyStore.load(new FileInputStream(https.keystorePath), https.keystorePassword.toCharArray())
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.defaultAlgorithm)
        kmf.init(keyStore, https.keyPassword.toCharArray())
        return kmf.keyManagers
    }

    private TrustManager[] buildTrustManager(Https https) {
        if (https.requireClientAuth) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.defaultType)
            trustStore.load(new FileInputStream(https.truststorePath), https.truststorePassword.toCharArray())
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.defaultAlgorithm)
            tmf.init(trustStore)
            return tmf.trustManagers
        } else {
            return []
        }
    }

    void createContext(String context, HttpHandler handler) {
        httpServer.createContext(context, handler)
    }

    void addMock(Mock mock) {
        ContextExecutor executor = executors.find { it.path == mock.path }
        if (executor) {
            executor.addMock(mock)
        } else {
            executors << new ContextExecutor(this, mock)
        }
        log.info("Added mock ${mock.name}")
    }

    void stop() {
        executors.each { httpServer.removeContext(it.contextPath) }
        httpServer.stop(0)
    }

    List<MockEvent> removeMock(String name) {
        return executors.collect { it.removeMock(name) }.flatten() as List<MockEvent>
    }

    List<MockEvent> peekMock(String name) {
        return executors.collect { it.peekMock(name) }.flatten() as List<MockEvent>
    }

    List<Mock> getMocks() {
        return executors.collect { it.mocks }.flatten() as List<Mock>
    }
}
