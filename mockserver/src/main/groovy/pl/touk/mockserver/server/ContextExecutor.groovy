package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j

import java.util.concurrent.CopyOnWriteArrayList

@Slf4j
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
                MockRequest request = new MockRequest(ex.requestBody.text, ex.requestHeaders, ex.requestURI)
                log.info("Mock received input")
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
                Util.createResponse(ex, request.text, 404)
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
        Util.createResponse(httpExchange, response.text, response.statusCode)
    }

    List<MockEvent> removeMock(String name) {
        Mock mock = mocks.find { it.name == name }
        if (mock) {
            mocks.remove(mock)
            return mock.history
        }
        return []
    }

    List<MockEvent> peekMock(String name) {
        Mock mock = mocks.find { it.name == name }
        if (mock) {
            return mock.history
        }
        return []
    }

    void addMock(Mock mock) {
        mocks << mock
    }

    List<Mock> getMocks() {
        return mocks
    }
}
