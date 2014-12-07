package com.blogspot.przybyszd.mockserver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import groovy.util.slurpersupport.GPathResult

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

class SoapMockServer {

    HttpServerWraper httpServerWraper
    List<HttpServerWraper> childServers = new CopyOnWriteArrayList<>()
    Set<String> actionsNames = new CopyOnWriteArraySet<>()

    SoapMockServer(int port = 9999){
        httpServerWraper= new HttpServerWraper(port)

        httpServerWraper.createContext('/serverControl', {
            HttpExchange ex ->
                ex.sendResponseHeaders(200, 0)
                try{
                GPathResult request = new XmlSlurper().parse(ex.requestBody)
                if(ex.requestMethod== 'POST' && request.name() == 'addMock'){
                    String name = request.name
                    if(name in actionsNames){
                        throw new RuntimeException('action already registered')
                    }
                    println "Adding $name"
                    String mockPath = request.path
                    int mockPort = Integer.valueOf(request.port as String)
                    Closure predicate = Eval.me(request.predicate as String) as Closure
                    Closure okResponse = Eval.me(request.response as String) as Closure
                    Action action = new Action(name, predicate, okResponse)
                    HttpServerWraper child = childServers.find {it.port == mockPort}
                    if(!child){
                        child = new HttpServerWraper(mockPort)
                        childServers << child
                    }
                    child.addAction(mockPath, action)
                    actionsNames << name
                    ex.responseBody << '<addedAction/>'
                    ex.responseBody.close()
                }
                }catch(Exception e){
                    ex.responseBody << """<exceptionOccured>${e.message}</exceptionOccured>"""
                    ex.responseBody.close()
                }
                //TODO add delete mock
                //TODO add list mock
        })
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

        void addAction(String path, Action action){
            ContextExecutor executor = executors.find {it.path == path}
            if(executor){
                executor.actions.add(action)
            }else {
                executors.add(new ContextExecutor(this, path, action))
            }
        }

        void removeAction(String name){
            //TODO delete action by name
        }

        void stop(){
            executors.each {httpServer.removeContext(it.path)}
            httpServer.stop(0)
        }
    }

    private static final class ContextExecutor{
        final HttpServerWraper httpServerWraper
        final String path
        List<Action> actions

        ContextExecutor(HttpServerWraper httpServerWraper, String path, Action initialAction) {
            this.httpServerWraper = httpServerWraper
            this.path = path
            this.actions = new CopyOnWriteArrayList<>([initialAction])
            httpServerWraper.createContext(path,{
                HttpExchange ex ->
                    ex.sendResponseHeaders(200, 0)
                    String input = ex.requestBody.text
                    println "Mock received input"
                    GPathResult xml = new XmlSlurper().parseText(input)
                    for (Action action : actions){
                        if(action.predicate(xml)){
                            ex.responseBody << action.responseOk(xml)
                            ex.responseBody.close()
                            return
                        }
                    }
                    ex.responseBody << "<invalidInput/>"
                    ex.responseBody.close()
            })
        }
    }

    private static final class Action{
        final String name
        final Closure predicate
        final Closure responseOk

        Action(String name, Closure predicate, Closure responseOk) {
            this.name = name
            this.predicate = predicate
            this.responseOk = responseOk
        }
    }

    static void main(String [] args) {
        SoapMockServer soapMockServer = new SoapMockServer()

        Runtime.runtime.addShutdownHook(new Thread({
            println 'Http server is stopping...'
            soapMockServer.stop()
            println 'Http server is stopped'
        } as Runnable))

        while(true){
            Thread.sleep(10000)
        }
    }
}
