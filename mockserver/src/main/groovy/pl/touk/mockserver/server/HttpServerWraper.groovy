package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j

import java.util.concurrent.Executors

@Slf4j
@PackageScope
class HttpServerWraper {
    private final HttpServer httpServer
    final int port

    private List<ContextExecutor> executors = []

    HttpServerWraper(int port) {
        this.port = port
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), port)
        httpServer = HttpServer.create(addr, 0)
        httpServer.executor = Executors.newWorkStealingPool()
        log.info("Http server starting on port $port...")
        httpServer.start()
        log.info('Http server is started')
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
