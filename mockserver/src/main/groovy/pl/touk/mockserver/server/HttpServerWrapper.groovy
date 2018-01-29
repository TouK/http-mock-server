package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import pl.touk.mockserver.api.common.Https

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.Executor

@Slf4j
@PackageScope
class HttpServerWrapper {
    private final HttpServer httpServer
    final int port

    private List<ContextExecutor> executors = []

    HttpServerWrapper(int port, Executor executor, Https https) {
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
            httpsServer.httpsConfigurator = new HttpsConfigurator(buildSslContext(https))
            return httpsServer
        } else {
            return HttpServer.create(addr, 0)
        }
    }

    private SSLContext buildSslContext(Https https) {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.defaultType)
        keyStore.load(new FileInputStream(https.keystorePath), https.keystorePassword.toCharArray())
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.defaultAlgorithm)
        kmf.init(keyStore, https.keyPassword.toCharArray())

        SSLContext ssl = SSLContext.getInstance('TLSv1')
        ssl.init(kmf.keyManagers, [] as TrustManager[], new SecureRandom())
        return ssl
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
