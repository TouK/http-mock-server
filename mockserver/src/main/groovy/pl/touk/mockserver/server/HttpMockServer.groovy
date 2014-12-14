package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

import static pl.touk.mockserver.server.Util.createResponse

@Slf4j
class HttpMockServer {

    private final HttpServerWraper httpServerWraper
    private final List<HttpServerWraper> childServers = new CopyOnWriteArrayList<>()
    private final Set<String> mockNames = new CopyOnWriteArraySet<>()

    HttpMockServer(int port = 9999) {
        httpServerWraper = new HttpServerWraper(port)

        httpServerWraper.createContext('/serverControl', {
            HttpExchange ex ->
                try {
                    if (ex.requestMethod == 'GET') {
                        listMocks(ex)
                    } else if (ex.requestMethod == 'POST') {
                        GPathResult request = new XmlSlurper().parse(ex.requestBody)
                        if (request.name() == 'addMock') {
                            addMock(request, ex)
                        } else if (request.name() == 'removeMock') {
                            removeMock(request, ex)
                        } else {
                            throw new RuntimeException('Unknown request')
                        }
                    } else {
                        throw new RuntimeException('Unknown request')
                    }
                } catch (Exception e) {
                    createErrorResponse(ex, e)
                }
        })
    }

    void listMocks(HttpExchange ex) {
        StringWriter sw = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(sw)
        builder.mocks {
            listMocks().each {
                Mock mock ->
                    builder.mock {
                        name mock.name
                        path mock.path
                        port mock.port
                    }
            }
        }
        createResponse(ex, sw.toString(), 200)
    }

    Set<Mock> listMocks() {
        return childServers.collect { it.mocks }.flatten() as TreeSet
    }

    private void addMock(GPathResult request, HttpExchange ex) {
        String name = request.name
        if (name in mockNames) {
            throw new RuntimeException('mock already registered')
        }
        Mock mock = mockFromRequest(request)
        HttpServerWraper child = getOrCreateChildServer(mock.port)
        child.addMock(mock)
        mockNames << name
        createResponse(ex, '<mockAdded/>', 200)
    }

    private static Mock mockFromRequest(GPathResult request) {
        String name = request.name
        String mockPath = request.path
        int mockPort = Integer.valueOf(request.port as String)
        Mock mock = new Mock(name, mockPath, mockPort)
        mock.predicate = request.predicate
        mock.response = request.response
        mock.soap = request.soap
        mock.statusCode = request.statusCode
        mock.method = request.method
        mock.responseHeaders = request.responseHeaders
        return mock
    }

    private HttpServerWraper getOrCreateChildServer(int mockPort) {
        HttpServerWraper child = childServers.find { it.port == mockPort }
        if (!child) {
            child = new HttpServerWraper(mockPort)
            childServers << child
        }
        return child
    }

    private void removeMock(GPathResult request, HttpExchange ex) {
        String name = request.name
        if (!(name in mockNames)) {
            throw new RuntimeException('mock not registered')
        }
        log.info("Removing mock $name")
        List<MockEvent> mockEvents = childServers.collect { it.removeMock(name) }.flatten()
        mockNames.remove(name)
        StringWriter sw = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(sw)
        builder.mockRemoved {
            mockEvents.each { MockEvent m ->
                builder.mockEvent {
                    builder.request {
                        text m.request.text
                        headers {
                            m.request.headers.each {
                                builder.param(name: it.key, it.value)
                            }
                        }
                        query {
                            m.request.query.each {
                                builder.param(name: it.key, it.value)
                            }
                        }
                        path {
                            m.request.path.each {
                                builder.elem it
                            }
                        }
                    }
                    builder.response {
                        text m.response.text
                        headers {
                            m.response.headers.each {
                                builder.param(name: it.key, it.value)
                            }
                        }
                        statusCode m.response.statusCode
                    }
                }
            }
        }
        createResponse(ex, sw.toString(), 200)
    }

    private static void createErrorResponse(HttpExchange ex, Exception e) {
        StringWriter sw = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(sw)
        builder.exceptionOccured e.message
        createResponse(ex, sw.toString(), 400)
    }

    void stop() {
        childServers.each { it.stop() }
        httpServerWraper.stop()
    }
}
