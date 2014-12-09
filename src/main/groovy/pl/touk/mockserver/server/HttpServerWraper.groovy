package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer

import java.util.concurrent.Executors

class HttpServerWraper {
    private final HttpServer httpServer
    private final int port

    private List<ContextExecutor> executors = []

    HttpServerWraper(int port) {
        this.port = port
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), port)
        httpServer = HttpServer.create(addr, 0)
        httpServer.executor = Executors.newCachedThreadPool()
        println "Http server statrting on port $port..."
        httpServer.start()
        println 'Http server is started'
    }

    void createContext(String context, HttpHandler handler) {
        httpServer.createContext(context, handler)
    }

    void addMock(String path, Mock mock) {
        ContextExecutor executor = executors.find { it.path == path }
        if (executor) {
            executor.addMock(mock)
        } else {
            executors << new ContextExecutor(this, path, mock)
        }
    }

    void stop() {
        executors.each { httpServer.removeContext(it.path) }
        httpServer.stop(0)
    }

    int removeMock(String name) {
        executors.inject(0) { int res, ContextExecutor e -> e.removeMock(name) + res}
    }
}
