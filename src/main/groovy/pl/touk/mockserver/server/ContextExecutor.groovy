package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange
import groovy.transform.PackageScope

import java.util.concurrent.CopyOnWriteArrayList

@PackageScope
class ContextExecutor {
    private final HttpServerWraper httpServerWraper
    final String path
    private final List<Mock> mocks

    ContextExecutor(HttpServerWraper httpServerWraper, Mock initialMock) {
        this.httpServerWraper = httpServerWraper
        this.path = '/' + initialMock.path
        this.mocks = new CopyOnWriteArrayList<>([initialMock])
        httpServerWraper.createContext(path, {
            HttpExchange ex ->
                MockRequest request = new MockRequest(ex.requestBody.text, ex.requestHeaders, ex.requestURI.query)
                println "Mock received input"
                for (Mock mock : mocks) {
                    try {
                        if (mock.match(ex.requestMethod, request)) {
                            MockResponse httpResponse = mock.apply(request)
                            fillExchange(ex, httpResponse)
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

    String getPath() {
        return path.substring(1)
    }

    String getContextPath() {
        return path
    }

    private static void fillExchange(HttpExchange httpExchange, MockResponse response) {
        response.headers.each {
            httpExchange.responseHeaders.add(it.key, it.value)
        }
        String responseText = response.response
        httpExchange.sendResponseHeaders(response.statusCode, responseText ? responseText.length() : -1)
        if (responseText) {
            httpExchange.responseBody << responseText
            httpExchange.responseBody.close()
        }
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

    List<Mock> getMocks(){
        return mocks
    }
}
