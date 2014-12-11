package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange
import groovy.util.slurpersupport.GPathResult

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

class HttpMockServer {

    private final HttpServerWraper httpServerWraper
    private final List<HttpServerWraper> childServers = new CopyOnWriteArrayList<>()
    private final Set<String> mockNames = new CopyOnWriteArraySet<>()

    HttpMockServer(int port = 9999){
        httpServerWraper= new HttpServerWraper(port)

        httpServerWraper.createContext('/serverControl', {
            HttpExchange ex ->
                try{
                    GPathResult request = new XmlSlurper().parse(ex.requestBody)
                    if(ex.requestMethod== 'POST' && request.name() == 'addMock'){
                        addMock(request, ex)
                    }else if(ex.requestMethod == 'POST' && request.name() == 'removeMock'){
                        removeMock(request, ex)
                    }
                    //TODO add get mock report
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
        Mock mock = new Mock(name, mockPath, mockPort)
        String predicate = request.predicate
        if(predicate){
            mock.predicate = Eval.me(predicate) as Closure
        }
        String okResponse = request.response
        if(okResponse){
            mock.responseOk = Eval.me(okResponse) as Closure
        }
        String soap = request.soap
        if(soap){
            mock.soap = Boolean.valueOf(soap)
        }
        String statusCode = request.statusCode
        if(statusCode){
            mock.statusCode = Integer.valueOf(statusCode)
        }
        String method = request.method
        if(method){
            mock.method = method
        }
        String responseHeaders = request.responseHeaders
        if(responseHeaders){
            mock.responseHeaders = Eval.me(responseHeaders) as Closure
        }
        HttpServerWraper child = childServers.find { it.port == mockPort }
        if (!child) {
            child = new HttpServerWraper(mockPort)
            childServers << child
        }
        child.addMock(mockPath, mock)
        mockNames << name
        ex.sendResponseHeaders(200, 0)
        ex.responseBody << '<mockAdded/>'
        ex.responseBody.close()
    }

    private void removeMock(GPathResult request, HttpExchange ex) {
        String name = request.name
        if (! (name in mockNames)) {
            throw new RuntimeException('mock not registered')
        }
        println "Removing $name"
        int used = childServers.inject(0) { int res, HttpServerWraper server-> server.removeMock(name) + res}
        mockNames.remove(name)
        ex.sendResponseHeaders(200, 0)
        ex.responseBody << "<mockRemoved>$used</mockRemoved>"
        ex.responseBody.close()
    }

    private static void createErrorResponse(HttpExchange ex, Exception e) {
        ex.sendResponseHeaders(400, 0)
        ex.responseBody << """<exceptionOccured>${e.message}</exceptionOccured>"""
        ex.responseBody.close()
    }

    void stop(){
        childServers.each {it.stop()}
        httpServerWraper.stop()
    }
}
