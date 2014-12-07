package com.blogspot.przybyszd.mockserver

import com.sun.net.httpserver.HttpExchange
import groovy.util.slurpersupport.GPathResult

import java.util.concurrent.CopyOnWriteArrayList

class ContextExecutor {
    private final HttpServerWraper httpServerWraper
    private final String path
    private final List<Mock> mocks

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
                        println "Mock ${mock.name} invoked"
                        ++mock.counter
                        ex.responseBody << mock.responseOk(xml)
                        ex.responseBody.close()
                        return
                    }
                }
                ex.responseBody << "<invalidInput/>"
                ex.responseBody.close()
        })
    }

    int removeMock(String name) {
        Mock mock = mocks.find {it.name == name}
        if(mock){
            mocks.remove(mock)
        }
        return mock.counter
    }
}
