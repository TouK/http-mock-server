package com.blogspot.przybyszd.mockserver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import groovy.transform.EqualsAndHashCode
import groovy.util.slurpersupport.GPathResult

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

class HttpMockServer {

    HttpServerWraper httpServerWraper
    List<HttpServerWraper> childServers = new CopyOnWriteArrayList<>()
    Set<String> mockNames = new CopyOnWriteArraySet<>()

    HttpMockServer(int port = 9999){
        httpServerWraper= new HttpServerWraper(port)

        httpServerWraper.createContext('/serverControl', {
            HttpExchange ex ->
                ex.sendResponseHeaders(200, 0)
                try{
                    GPathResult request = new XmlSlurper().parse(ex.requestBody)
                    if(ex.requestMethod== 'POST' && request.name() == 'addMock'){
                        addMock(request, ex)
                    }else if(ex.requestMethod == 'POST' && request.name() == 'removeMock'){
                        removeMock(request, ex)
                    }
                    //TODO add list mock
                }catch(Exception e){
                    createErrorResponse(ex, e)
                }
        })
    }

    private void addMock(GPathResult request, HttpExchange ex) {
        String name = request.name
        if (name in mockNames) {
            throw new RuntimeException('mock already registered')
        }
        println "Adding $name"
        String mockPath = request.path
        int mockPort = Integer.valueOf(request.port as String)
        Closure predicate = Eval.me(request.predicate as String) as Closure
        Closure okResponse = Eval.me(request.response as String) as Closure
        Mock mock = new Mock(name, predicate, okResponse)
        HttpServerWraper child = childServers.find { it.port == mockPort }
        if (!child) {
            child = new HttpServerWraper(mockPort)
            childServers << child
        }
        child.addMock(mockPath, mock)
        mockNames << name
        ex.responseBody << '<mockAdded/>'
        ex.responseBody.close()
    }

    private void removeMock(GPathResult request, HttpExchange ex) {
        String name = request.name
        if (! (name in mockNames)) {
            throw new RuntimeException('mock not registered')
        }
        childServers.each {it.removeMock(name)}
        mockNames.remove(name)
        ex.responseBody << '<mockRemoved/>'
        ex.responseBody.close()
    }

    private static void createErrorResponse(HttpExchange ex, Exception e) {
        ex.responseBody << """<exceptionOccured>${e.message}</exceptionOccured>"""
        ex.responseBody.close()
    }

    void stop(){
        childServers.each {it.stop()}
        httpServerWraper.stop()
    }

    private static final class HttpServerWraper{
        final HttpServer httpServer
        final int port

        List<ContextExecutor> executors = []

        HttpServerWraper(int port){
            this.port = port
            InetSocketAddress addr = new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), port)
            httpServer = HttpServer.create(addr, 0)
            httpServer.executor = Executors.newCachedThreadPool()
            println "Http server statrting on port $port..."
            httpServer.start()
            println 'Http server is started'
        }

        void createContext(String context, HttpHandler handler){
            httpServer.createContext(context, handler)
        }

        void addMock(String path, Mock mock){
            ContextExecutor executor = executors.find {it.path == path}
            if(executor){
                executor.mocks << mock
            }else {
                executors << new ContextExecutor(this, path, mock)
            }
        }

        void stop(){
            executors.each {httpServer.removeContext(it.path)}
            httpServer.stop(0)
        }

        void removeMock(String name) {
            executors.each {it.removeMock(name)}
        }
    }

    private static final class ContextExecutor{
        final HttpServerWraper httpServerWraper
        final String path
        List<Mock> mocks

        ContextExecutor(HttpServerWraper httpServerWraper, String path, Mock initialMock) {
            this.httpServerWraper = httpServerWraper
            this.path = path
            this.mocks = new CopyOnWriteArrayList<>([initialMock])
            httpServerWraper.createContext(path,{
                HttpExchange ex ->
                    ex.sendResponseHeaders(200, 0)
                    String input = ex.requestBody.text
                    println "Mock received input"
                    GPathResult xml = new XmlSlurper().parseText(input)
                    for (Mock mock : mocks){
                        if(mock.predicate(xml)){
                            ex.responseBody << mock.responseOk(xml)
                            ex.responseBody.close()
                            return
                        }
                    }
                    ex.responseBody << "<invalidInput/>"
                    ex.responseBody.close()
            })
        }

        void removeMock(String name) {
            Mock mock = mocks.find {it.name == name}
            if(mock){
                mocks.remove(mock)
            }
        }
    }

    @EqualsAndHashCode
    private static final class Mock {
        final String name
        final Closure predicate
        final Closure responseOk
        //TODO add http method
        //TODO add is soap method

        Mock(String name, Closure predicate, Closure responseOk) {
            this.name = name
            this.predicate = predicate
            this.responseOk = responseOk
        }
    }

    static void main(String [] args) {
        HttpMockServer httpMockServer = new HttpMockServer()

        Runtime.runtime.addShutdownHook(new Thread({
            println 'Http server is stopping...'
            httpMockServer.stop()
            println 'Http server is stopped'
        } as Runnable))

        while(true){
            Thread.sleep(10000)
        }
    }
}
