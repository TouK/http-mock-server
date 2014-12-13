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
                String input = ex.requestBody.text
                Map<String, String> queryParams = ex.requestURI.query?.split('&')?.collectEntries {
                    String[] keyValue = it.split('='); [(keyValue[0]): keyValue[1]]
                } ?: [:]
                Map<String, String> headers = ex.requestHeaders.collectEntries {
                    [it.key.toLowerCase(), it.value.join(',')]
                }
                println "Mock received input"
                for (Mock mock : mocks) {
                    try {
                        GPathResult xml = input ? new XmlSlurper().parseText(input) : null
                        if (mock.soap) {
                            if (xml.name() == 'Envelope' && xml.Body.size() > 0) {
                                xml = getSoapBodyContent(xml)
                            } else {
                                continue
                            }
                        }
                        if (ex.requestMethod == mock.method &&
                                mock.predicate(xml) &&
                                mock.requestHeaders(headers) &&
                                mock.queryParams(queryParams)) {
                            println "Mock ${mock.name} invoked"
                            ++mock.counter
                            String response = mock.responseOk(xml)
                            mock.responseHeaders(xml).each {
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
                ex.responseBody << "<invalidInput/>"
                ex.responseBody.close()
        })
    }

    private static GPathResult getSoapBodyContent(GPathResult xml) {
        return xml.Body.'**'[1]
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
