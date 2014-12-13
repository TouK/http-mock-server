package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange
import groovy.util.slurpersupport.GPathResult

import java.util.concurrent.CopyOnWriteArrayList

class ContextExecutor {
    private final HttpServerWraper httpServerWraper
    final String path
    private final List<Mock> mocks

    ContextExecutor(HttpServerWraper httpServerWraper, String path, Mock initialMock) {
        this.httpServerWraper = httpServerWraper
        this.path = path
        this.mocks = new CopyOnWriteArrayList<>([initialMock])
        httpServerWraper.createContext(path, {
            HttpExchange ex ->
                Request request = new Request(ex.requestBody.text, ex.requestHeaders, ex.requestURI.query)
                println "Mock received input"
                for (Mock mock : mocks) {
                    try {
                        if (ex.requestMethod == mock.method &&
                                mock.predicate(request)) {
                            println "Mock ${mock.name} invoked"
                            ++mock.counter
                            String response = mock.responseOk(request)
                            mock.responseHeaders(request).each {
                                ex.responseHeaders.add(it.key as String, it.value as String)
                            }
                            ex.sendResponseHeaders(mock.statusCode, response ? 0 : -1)
                            if (response) {
                                ex.responseBody << (mock.soap ? wrapSoap(response) : response)
                                ex.responseBody.close()
                            }
                            return
                        }
                    } catch (Exception e) {
                        e.printStackTrace()
                    }
                }
                ex.sendResponseHeaders(404, 0)
                ex.responseBody << request.text
                ex.responseBody.close()
        })
    }

    int removeMock(String name) {
        Mock mock = mocks.find { it.name == name }
        if (mock) {
            mocks.remove(mock)
            return mock.counter
        }
        return 0
    }

    void addMock(Mock mock) {
        mocks << mock
    }

    private static String wrapSoap(String request) {
        """<?xml version='1.0' encoding='UTF-8'?>
            <soap-env:Envelope xmlns:soap-env='http://schemas.xmlsoap.org/soap/envelope/' xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">
                <soap-env:Body>${request}</soap-env:Body>
            </soap-env:Envelope>"""
    }
}
