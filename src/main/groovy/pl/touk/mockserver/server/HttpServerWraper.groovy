package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import groovy.transform.PackageScope

import java.util.concurrent.Executors

@PackageScope
class HttpServerWraper {
    private final HttpServer httpServer
    final int port

    private List<ContextExecutor> executors = []

    HttpServerWraper(int port) {
        this.port = port
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), port)
        httpServer = HttpServer.create(addr, 0)
        httpServer.executor = Executors.newCachedThreadPool()
        println("Http server statrting on port $port...")
        httpServer.start()
        println('Http server is started')
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
        println "Added mock ${mock.name}"
    }

    void stop() {
        executors.each { httpServer.removeContext(it.path) }
        httpServer.stop(0)
    }

    int removeMock(String name) {
        executors.inject(0) { int res, ContextExecutor e -> e.removeMock(name) + res }
    }
}
