package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange
import groovy.util.logging.Slf4j
import pl.touk.mockserver.api.common.Https
import pl.touk.mockserver.api.common.ImportAlias
import pl.touk.mockserver.api.common.Method
import pl.touk.mockserver.api.request.AddMock
import pl.touk.mockserver.api.request.MockServerRequest
import pl.touk.mockserver.api.request.PeekMock
import pl.touk.mockserver.api.request.RemoveMock
import pl.touk.mockserver.api.response.ExceptionOccured
import pl.touk.mockserver.api.response.MockAdded
import pl.touk.mockserver.api.response.MockEventReport
import pl.touk.mockserver.api.response.MockPeeked
import pl.touk.mockserver.api.response.MockRemoved
import pl.touk.mockserver.api.response.MockReport
import pl.touk.mockserver.api.response.MockRequestReport
import pl.touk.mockserver.api.response.MockResponseReport
import pl.touk.mockserver.api.response.Mocks
import pl.touk.mockserver.api.response.Parameter

import javax.xml.bind.JAXBContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor
import java.util.concurrent.Executors

import static pl.touk.mockserver.server.Util.createResponse

@Slf4j
class HttpMockServer {

    private final HttpServerWrapper httpServerWrapper
    private final Map<Integer, HttpServerWrapper> childServers = new ConcurrentHashMap<>()
    private final Set<String> mockNames = new CopyOnWriteArraySet<>()
    private final ConfigObject configuration = new ConfigObject()
    private final Executor executor

    private static
    final JAXBContext requestJaxbContext = JAXBContext.newInstance(AddMock.package.name, AddMock.classLoader)

    HttpMockServer(int port = 9999, ConfigObject initialConfiguration = new ConfigObject(), int threads = 10) {
        executor = Executors.newFixedThreadPool(threads)
        httpServerWrapper = new HttpServerWrapper(port, executor)

        initialConfiguration.values()?.each { ConfigObject co ->
            addMock(co)
        }

        httpServerWrapper.createContext('/serverControl', {
            HttpExchange ex ->
                try {
                    if (ex.requestMethod == 'GET') {
                        if (ex.requestURI.path == '/serverControl/configuration') {
                            createResponse(ex, configuration.prettyPrint(), 200)
                        } else {
                            listMocks(ex)
                        }
                    } else if (ex.requestMethod == 'POST') {
                        MockServerRequest request = requestJaxbContext.createUnmarshaller().unmarshal(ex.requestBody) as MockServerRequest
                        if (request instanceof AddMock) {
                            addMock(request, ex)
                        } else if (request instanceof RemoveMock) {
                            removeMock(request, ex)
                        } else if (request instanceof PeekMock) {
                            peekMock(request, ex)
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
        Mocks mockListing = new Mocks(
            mocks: listMocks().collect {
                new MockReport(
                    name: it.name,
                    path: it.path,
                    port: it.port,
                    predicate: it.predicateClosureText,
                    response: it.responseClosureText,
                    responseHeaders: it.responseHeadersClosureText,
                    soap: it.soap,
                    method: it.method,
                    statusCode: it.statusCode as int,
                    schema: it.schema,
                    imports: it.imports.collect { new ImportAlias(alias: it.key, fullClassName: it.value) },
                    preserveHistory: it.preserveHistory
                )
            }
        )
        createResponse(ex, mockListing, 200)
    }

    Set<Mock> listMocks() {
        return childServers.values().collect { it.mocks }.flatten() as TreeSet<Mock>
    }

    private void addMock(AddMock request, HttpExchange ex) {
        String name = request.name
        if (name in mockNames) {
            throw new RuntimeException('mock already registered')
        }
        Mock mock = mockFromRequest(request)
        HttpServerWrapper child = getOrCreateChildServer(mock.port, mock.https)
        child.addMock(mock)
        saveConfiguration(request)
        mockNames << name
        createResponse(ex, new MockAdded(), 200)
    }

    private void addMock(ConfigObject co) {
        String name = co.name
        if (name in mockNames) {
            throw new RuntimeException('mock already registered')
        }
        Mock mock = mockFromConfig(co)
        HttpServerWrapper child = getOrCreateChildServer(mock.port, null)
        child.addMock(mock)
        configuration.put(name, co)
        mockNames << name
    }

    private void saveConfiguration(AddMock request) {
        ConfigObject mockDefinition = new ConfigObject()
        request.metaPropertyValues.findAll { it.name != 'class' && it.value }.each {
            if (it.name == 'imports') {
                ConfigObject configObject = new ConfigObject()
                it.value.each { ImportAlias imp ->
                    configObject.put(imp.alias, imp.fullClassName)
                }
                mockDefinition.put(it.name, configObject)
            } else if (it.name == 'method') {
                mockDefinition.put(it.name, it.value.name())
            } else {
                mockDefinition.put(it.name, it.value)
            }
        }
        configuration.put(request.name, mockDefinition)
    }

    private static Mock mockFromRequest(AddMock request) {
        Mock mock = new Mock(request.name, request.path, request.port)
        mock.imports = request.imports?.collectEntries { [(it.alias): it.fullClassName] } ?: [:]
        mock.predicate = request.predicate
        mock.response = request.response
        mock.soap = request.soap
        mock.statusCode = request.statusCode
        mock.method = request.method
        mock.responseHeaders = request.responseHeaders
        mock.schema = request.schema
        mock.preserveHistory = request.preserveHistory != false
        mock.https = request.https
        return mock
    }

    private static Mock mockFromConfig(ConfigObject co) {
        Mock mock = new Mock(co.name, co.path, co.port)
        mock.imports = co.imports
        mock.predicate = co.predicate ?: null
        mock.response = co.response ?: null
        mock.soap = co.soap ?: null
        mock.statusCode = co.statusCode ?: null
        mock.method = co.method ? Method.valueOf(co.method) : null
        mock.responseHeaders = co.responseHeaders ?: null
        mock.schema = co.schema ?: null
        mock.preserveHistory = co.preserveHistory != false
        return mock
    }

    private HttpServerWrapper getOrCreateChildServer(int mockPort, Https https) {
        HttpServerWrapper child = childServers[mockPort]
        if (!child) {
            child = new HttpServerWrapper(mockPort, executor, https)
            childServers.put(mockPort, child)
        }
        return child
    }

    private void removeMock(RemoveMock request, HttpExchange ex) {
        String name = request.name
        boolean skipReport = request.skipReport ?: false
        if (!(name in mockNames)) {
            throw new RuntimeException('mock not registered')
        }
        log.info("Removing mock $name")
        List<MockEvent> mockEvents = skipReport ? [] : childServers.values().collect {
            it.removeMock(name)
        }.flatten() as List<MockEvent>
        mockNames.remove(name)
        configuration.remove(name)
        MockRemoved mockRemoved = new MockRemoved(
            mockEvents: createMockEventReports(mockEvents)
        )
        createResponse(ex, mockRemoved, 200)
    }

    private static List<MockEventReport> createMockEventReports(List<MockEvent> mockEvents) {
        return mockEvents.collect {
            new MockEventReport(
                request: new MockRequestReport(
                    text: it.request.text,
                    headers: new MockRequestReport.Headers(headers: it.request.headers.collect {
                        new Parameter(name: it.key, value: it.value)
                    }),
                    queryParams: new MockRequestReport.QueryParams(queryParams: it.request.query.collect {
                        new Parameter(name: it.key, value: it.value)
                    }),
                    path: new MockRequestReport.Path(pathParts: it.request.path)
                ),
                response: new MockResponseReport(
                    statusCode: it.response.statusCode,
                    text: it.response.text,
                    headers: new MockResponseReport.Headers(headers: it.response.headers.collect {
                        new Parameter(name: it.key, value: it.value)
                    })
                )
            )
        }
    }

    private void peekMock(PeekMock request, HttpExchange ex) {
        String name = request.name
        if (!(name in mockNames)) {
            throw new RuntimeException('mock not registered')
        }
        log.trace("Peeking mock $name")
        List<MockEvent> mockEvents = childServers.values().collect { it.peekMock(name) }.flatten() as List<MockEvent>
        MockPeeked mockPeeked = new MockPeeked(
            mockEvents: createMockEventReports(mockEvents)
        )
        createResponse(ex, mockPeeked, 200)
    }

    private static void createErrorResponse(HttpExchange ex, Exception e) {
        log.warn('Exception occured', e)
        createResponse(ex, new ExceptionOccured(value: e.message), 400)
    }

    void stop() {
        childServers.values().each { it.stop() }
        httpServerWrapper.stop()
    }
}
