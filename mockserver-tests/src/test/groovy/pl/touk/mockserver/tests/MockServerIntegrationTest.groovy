package pl.touk.mockserver.tests

import groovy.util.slurpersupport.GPathResult
import org.apache.http.client.methods.*
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import pl.touk.mockserver.api.request.AddMock
import pl.touk.mockserver.api.common.Method
import pl.touk.mockserver.api.response.MockEventReport
import pl.touk.mockserver.api.response.MockReport
import pl.touk.mockserver.api.response.Parameter
import pl.touk.mockserver.client.*
import pl.touk.mockserver.server.HttpMockServer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class MockServerIntegrationTest extends Specification {

    RemoteMockServer remoteMockServer

    HttpMockServer httpMockServer

    @Shared
    CloseableHttpClient client = HttpClients.createDefault()

    def setup() {
        httpMockServer = new HttpMockServer(9000)
        remoteMockServer = new RemoteMockServer('localhost', 9000)
    }

    def cleanup() {
        httpMockServer.stop()
    }

    def "should add working rest mock on endpoint"() {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseRest-${req.xml.name()}/>"}''',
                    soap: false
            ))
        when:
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponseRest-request'
        expect:
            remoteMockServer.removeMock('testRest')?.size() == 1
    }

    def "should add working rest mock on endpoint with utf"() {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRestUtf',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request' && req.xml.@test == 'łżźćąś'}''',
                    response: '''{req -> "<goodResponseRest-${req.xml.name()} ans='łżźćąś'/>"}''',
                    soap: false
            ))
        when:
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request test="łżźćąś"/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponseRest-request'
            restPostResponse.@ans == 'łżźćąś'
        expect:
            remoteMockServer.removeMock('testRestUtf')?.size() == 1
    }

    def "should add soap mock on endpoint"() {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testSoap',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.soap.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.soap.name()}/>"}''',
                    soap: true
            ))
        when:
            HttpPost soapPost = new HttpPost('http://localhost:9999/testEndpoint')
            soapPost.entity = new StringEntity(Util.soap('<request/>'), ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(soapPost)
        then:
            GPathResult soapPostResponse = Util.extractXmlResponse(response)
            soapPostResponse.name() == 'Envelope'
            soapPostResponse.Body.'goodResponseSoap-request'.size() == 1
        expect:
            remoteMockServer.removeMock('testSoap')?.size() == 1
    }

    def "should throw exception when try to remove mock when it does not exist"() {
        when:
            remoteMockServer.removeMock('testSoap')
        then:
            thrown(MockDoesNotExist)
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testSoap',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        and:
            remoteMockServer.removeMock('testSoap') == []
        when:
            remoteMockServer.removeMock('testSoap')
        then:
            thrown(MockDoesNotExist)
    }

    def "should not add mock with existing name"() {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testSoap',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        when:
            remoteMockServer.addMock(new AddMock(
                    name: 'testSoap',
                    path: 'testEndpoint2',
                    port: 9998,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        then:
            thrown(MockAlreadyExists)
    }

    def "should not add mock with empty name"() {
        when:
            remoteMockServer.addMock(new AddMock(
                    name: '',
                    path: 'testEndpoint2',
                    port: 9998,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        then:
            thrown(InvalidMockDefinition)
    }

    def "should add mock after deleting old mock with the same name"() {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testSoap',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.name()}/>"}''',
                    soap: true
            ))
        and:
            remoteMockServer.removeMock('testSoap') == []
        and:
            remoteMockServer.addMock(new AddMock(
                    name: 'testSoap',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request2'}''',
                    response: '''{req -> "<goodResponseSoap2-${req.xml.name()}/>"}''',
                    soap: true
            ))
    }

    def "should add simultaneously working post and rest mocks with the same predicate and endpoint nad port"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseRest-${req.xml.name()}/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testSoap',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.soap.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.soap.name()}/>"}''',
                    soap: true
            ))
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            HttpPost soapPost = new HttpPost('http://localhost:9999/testEndpoint')
            soapPost.entity = new StringEntity(Util.soap('<request/>'), ContentType.create("text/xml", "UTF-8"))
        when:
            CloseableHttpResponse restResponse = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(restResponse)
            restPostResponse.name() == 'goodResponseRest-request'
        when:
            CloseableHttpResponse soapResponse = client.execute(soapPost)
        then:
            GPathResult soapPostResponse = Util.extractXmlResponse(soapResponse)
            soapPostResponse.name() == 'Envelope'
            soapPostResponse.Body.'goodResponseSoap-request'.size() == 1
    }

    @Unroll
    def "should dispatch rest mocks when second on #name"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest1',
                    path: 'test1',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request1'}''',
                    response: '''{req -> "<goodResponseRest1/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: secondPath,
                    port: secondPort,
                    predicate: '''{req -> req.xml.name() == 'request2'}''',
                    response: '''{req -> "<goodResponseRest2/>"}'''
            ))
            HttpPost firstRequest = new HttpPost('http://localhost:9999/test1')
            firstRequest.entity = new StringEntity('<request1/>', ContentType.create("text/xml", "UTF-8"))
            HttpPost secondRequest = new HttpPost("http://localhost:${secondPort}/${secondPath}")
            secondRequest.entity = new StringEntity('<request2/>', ContentType.create("text/xml", "UTF-8"))
        when:
            CloseableHttpResponse firstResponse = client.execute(firstRequest)
        then:
            GPathResult firstXmlResponse = Util.extractXmlResponse(firstResponse)
            firstXmlResponse.name() == 'goodResponseRest1'
        when:
            CloseableHttpResponse secondResponse = client.execute(secondRequest)
        then:
            GPathResult secondXmlResponse = Util.extractXmlResponse(secondResponse)
            secondXmlResponse.name() == 'goodResponseRest2'
        where:
            secondPort | secondPath | name
            9999       | 'test1'    | 'the same port and path'
            9998       | 'test1'    | 'the same path and another port'
            9999       | 'test2'    | 'the same port and another path'
            9998       | 'test2'    | 'another port and path'
    }

    @Unroll
    def "should dispatch rest mock with response code"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest1',
                    path: 'test1',
                    port: 9999,
                    statusCode: statusCode
            ))
            HttpPost request = new HttpPost('http://localhost:9999/test1')
            request.entity = new StringEntity('<request1/>', ContentType.create("text/xml", "UTF-8"))
        when:
            CloseableHttpResponse response = client.execute(request)
        then:
            response.statusLine.statusCode == expectedStatusCode
            EntityUtils.consumeQuietly(response.entity)
        where:
            statusCode | expectedStatusCode
            null       | 200
            300        | 300
            204        | 204
    }

    def "should return response code 404 and error body the same as request body when mocks does not apply"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest1',
                    path: 'test1',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request2'}''',
                    response: '''{req -> "<goodResponseRest2/>"}'''
            ))
            HttpPost request = new HttpPost('http://localhost:9999/test1')
            request.entity = new StringEntity('<request1/>', ContentType.create("text/xml", "UTF-8"))
        when:
            CloseableHttpResponse response = client.execute(request)
        then:
            response.statusLine.statusCode == 404
            GPathResult secondXmlResponse = Util.extractXmlResponse(response)
            secondXmlResponse.name() == 'request1'
    }

    def "should inform that there was problem during adding mock - invalid port"() {
        when:
            remoteMockServer.addMock(new AddMock(
                    name: 'testSoap',
                    path: 'testEndpoint2',
                    port: -1,
                    predicate: '''{_ -> true}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        then:
            thrown(InvalidMockDefinition)
    }

    def "should dispatch rest mock with get method"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<getResponse/>"}''',
                    method: Method.GET
            ))
            HttpGet restGet = new HttpGet('http://localhost:9999/testEndpoint')
        when:
            CloseableHttpResponse response = client.execute(restGet)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'getResponse'
    }

    def "should dispatch rest mock with trace method"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<traceResponse/>"}''',
                    method: Method.TRACE
            ))
            HttpTrace restTrace = new HttpTrace('http://localhost:9999/testEndpoint')
        when:
            CloseableHttpResponse response = client.execute(restTrace)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'traceResponse'
    }

    def "should dispatch rest mock with head method"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'testEndpoint',
                    port: 9999,
                    method: Method.HEAD
            ))
            HttpHead restHead = new HttpHead('http://localhost:9999/testEndpoint')
        when:
            CloseableHttpResponse response = client.execute(restHead)
        then:
            EntityUtils.consumeQuietly(response.entity)
            response.statusLine.statusCode == 200
    }

    def "should dispatch rest mock with options method"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'testEndpoint',
                    port: 9999,
                    method: Method.OPTIONS
            ))
            HttpOptions restOptions = new HttpOptions('http://localhost:9999/testEndpoint')
        when:
            CloseableHttpResponse response = client.execute(restOptions)
        then:
            response.statusLine.statusCode == 200
            EntityUtils.consumeQuietly(response.entity)
    }

    def "should dispatch rest mock with put method"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'test1',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'test1',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request1'}''',
                    response: '''{_ -> "<goodResponseRest1/>"}''',
                    method: Method.PUT
            ))
            HttpPut request = new HttpPut('http://localhost:9999/test1')
            request.entity = new StringEntity('<request1/>', ContentType.create("text/xml", "UTF-8"))
        when:
            CloseableHttpResponse response = client.execute(request)
        then:
            GPathResult secondXmlResponse = Util.extractXmlResponse(response)
            secondXmlResponse.name() == 'goodResponseRest1'
    }

    def "should dispatch rest mock with delete method"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'test1',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'test1',
                    port: 9999,
                    response: '''{_ -> "<goodResponseRest1/>"}''',
                    method: Method.DELETE
            ))
            HttpDelete request = new HttpDelete('http://localhost:9999/test1')
        when:
            CloseableHttpResponse response = client.execute(request)
        then:
            GPathResult secondXmlResponse = Util.extractXmlResponse(response)
            secondXmlResponse.name() == 'goodResponseRest1'
    }

    def "should dispatch rest mock with patch method"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'test1',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'test1',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request1'}''',
                    response: '''{_ -> "<goodResponseRest1/>"}''',
                    method: Method.PATCH
            ))
            HttpPatch request = new HttpPatch('http://localhost:9999/test1')
            request.entity = new StringEntity('<request1/>', ContentType.create("text/xml", "UTF-8"))
        when:
            CloseableHttpResponse response = client.execute(request)
        then:
            GPathResult secondXmlResponse = Util.extractXmlResponse(response)
            secondXmlResponse.name() == 'goodResponseRest1'
    }

    def "should add mock that return headers"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{_ -> "<goodResponse/>"}''',
                    responseHeaders: '''{ req -> ['Input-Name':"${req.xml.name()}"]}'''
            ))
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            response.allHeaders.findAll { it.name.toLowerCase() == 'input-name' && it.value == 'request' }
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse'
    }

    def "should add mock that accepts only when certain request headers exists"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{ req -> req.headers['user-agent']?.startsWith('Mozilla') &&
                                            req.headers.pragma == 'no-cache'}''',
                    response: '''{_ -> "<goodResponse/>"}'''
            ))
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            restPost.addHeader('User-Agent', 'Mozilla/5.0')
            restPost.addHeader('Pragma', 'no-cache')
            HttpPost badRestPost = new HttpPost('http://localhost:9999/testEndpoint')
            badRestPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            badRestPost.addHeader('Pragma', 'no-cache')
        when:
            CloseableHttpResponse badResponse = client.execute(badRestPost)
        then:
            badResponse.statusLine.statusCode == 404
            Util.consumeResponse(badResponse)
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse'
    }

    def "should add mock that accepts only when certain query params exists"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{ req -> req.query['q'] == '15' &&
                                            req.query.id == '1'}''',
                    response: '''{_ -> "<goodResponse/>"}'''
            ))
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint?q=15&id=1')
            HttpPost badRestPost = new HttpPost('http://localhost:9999/testEndpoint?q=15&id=2')
        when:
            CloseableHttpResponse badResponse = client.execute(badRestPost)
        then:
            badResponse.statusLine.statusCode == 404
            Util.consumeResponse(badResponse)
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse'
    }

    def "should add mock that accepts only when request has specific body"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.text == 'hello=world&id=3'}''',
                    response: '''{_ -> "<goodResponse/>"}'''
            ))
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('hello=world&id=3', ContentType.create("text/plain", "UTF-8"))
            HttpPost badRestPost = new HttpPost('http://localhost:9999/testEndpoint')
            badRestPost.entity = new StringEntity('hello=world&id=2', ContentType.create("text/plain", "UTF-8"))
        when:
            CloseableHttpResponse badResponse = client.execute(badRestPost)
        then:
            badResponse.statusLine.statusCode == 404
            Util.consumeResponse(badResponse)
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse'
    }

    def "should add mock which response json to json"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.json.id == 1 && req.json.ar == ["a", true]}''',
                    response: '''{req -> """{"name":"goodResponse-${req.json.id}"}"""}'''
            ))
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('{"id":1, "ar":["a", true]}', ContentType.create("text/json", "UTF-8"))
            HttpPost badRestPost = new HttpPost('http://localhost:9999/testEndpoint')
            badRestPost.entity = new StringEntity('{"id":1, "ar":["a", false]}', ContentType.create("text/json", "UTF-8"))
        when:
            CloseableHttpResponse badResponse = client.execute(badRestPost)
        then:
            badResponse.statusLine.statusCode == 404
            Util.consumeResponse(badResponse)
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            Object restPostResponse = Util.extractJsonResponse(response)
            restPostResponse.name == 'goodResponse-1'
    }

    def "should get list mocks"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'testEndpoint',
                    port: 9998,
                    predicate: '''{ req -> req.xml.name() == 'request1'}''',
                    response: '''{ req -> '<response/>' }''',
                    responseHeaders: '{ _ -> [a: "b"] }'
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest4',
                    path: 'testEndpoint',
                    port: 9999,
                    soap: true,
                    statusCode: 204,
                    method: Method.PUT
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest3',
                    path: 'testEndpoint2',
                    port: 9999
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest5',
                    path: 'testEndpoint',
                    port: 9999
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest6',
                    path: 'testEndpoint2',
                    port: 9999
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999
            ))
            remoteMockServer.removeMock('testRest5')
        when:
            List<MockReport> mockReport = remoteMockServer.listMocks()
        then:
            mockReport.size() == 5
            assertMockReport(mockReport[0], [name:'testRest', path: 'testEndpoint', port: 9999, predicate: '{ _ -> true }', response: '''{ _ -> '' }''', responseHeaders: '{ _ -> [:] }', soap: false, statusCode: 200, method: Method.POST])
            assertMockReport(mockReport[1], [name: 'testRest2', path: 'testEndpoint', port: 9998, predicate: '''{ req -> req.xml.name() == 'request1'}''', response: '''{ req -> '<response/>' }''', responseHeaders: '{ _ -> [a: "b"] }', soap: false, statusCode: 200, method: Method.POST])
            assertMockReport(mockReport[2], [name: 'testRest3', path: 'testEndpoint2', port: 9999, predicate: '{ _ -> true }', response: '''{ _ -> '' }''', responseHeaders: '{ _ -> [:] }', soap: false, statusCode: 200, method: Method.POST])
            assertMockReport(mockReport[3], [name: 'testRest4', path: 'testEndpoint', port: 9999, predicate: '{ _ -> true }', response: '''{ _ -> '' }''', responseHeaders: '{ _ -> [:] }', soap: true, statusCode: 204, method: Method.PUT])
            assertMockReport(mockReport[4], [name: 'testRest6', path: 'testEndpoint2', port: 9999, predicate: '{ _ -> true }', response: '''{ _ -> '' }''', responseHeaders: '{ _ -> [:] }', soap: false, statusCode: 200, method: Method.POST])
    }

    private void assertMockReport( MockReport mockReport, Map<String, Object> props) {
        props.each {
            assert mockReport."${it.key}" == it.value
        }
    }

    def "should add mock accepts path certain path params"() {
        given:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.path[1] == '15' && req.path[2] == 'comments'}''',
                    response: '''{req -> """{"name":"goodResponse-${req.path[1]}"}"""}'''
            ))
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint/15/comments')
            HttpPost badRestPost = new HttpPost('http://localhost:9999/testEndpoint/test/comments')
        when:
            CloseableHttpResponse badResponse = client.execute(badRestPost)
        then:
            badResponse.statusLine.statusCode == 404
            Util.consumeResponse(badResponse)
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            Object restPostResponse = Util.extractJsonResponse(response)
            restPostResponse.name == 'goodResponse-15'
    }

    def "should get mock report when deleting mock"() {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name()[0..6] == 'request' }''',
                    response: '''{req -> "<goodResponseRest-${req.xml.name()}/>"}''',
                    statusCode: 201,
                    responseHeaders: '''{req -> ['aaa':'14']}''',
                    soap: false
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'reqXYZ' }''',
                    response: '''{req -> "<goodResponseRest/>"}''',
                    statusCode: 202,
                    responseHeaders: '''{req -> ['aaa':'15']}''',
                    soap: false
            ))
        when:
            HttpPost post1 = new HttpPost('http://localhost:9999/testEndpoint')
            post1.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response1 = client.execute(post1)
        then:
            GPathResult restPostResponse1 = Util.extractXmlResponse(response1)
            restPostResponse1.name() == 'goodResponseRest-request'
        when:
            HttpPost post2 = new HttpPost('http://localhost:9999/testEndpoint/hello')
            post2.entity = new StringEntity('<request15/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response2 = client.execute(post2)
        then:
            GPathResult restPostResponse2 = Util.extractXmlResponse(response2)
            restPostResponse2.name() == 'goodResponseRest-request15'
        when:
            HttpPost post3 = new HttpPost('http://localhost:9999/testEndpoint?id=123')
            post3.entity = new StringEntity('<reqXYZ/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response3 = client.execute(post3)
        then:
            GPathResult restPostResponse3 = Util.extractXmlResponse(response3)
            restPostResponse3.name() == 'goodResponseRest'
        when:
            List<MockEventReport> mockEvents1 = remoteMockServer.removeMock('testRest')
        then:
            mockEvents1.size() == 2
            mockEvents1[0].request.text == '<request/>'
            !mockEvents1[0].request.headers?.headers?.empty
            mockEvents1[0].request.queryParams.queryParams == []
            mockEvents1[0].request.path.pathParts == ['testEndpoint']
            !mockEvents1[0].response.headers?.headers?.empty
            mockEvents1[0].response.text == '<goodResponseRest-request/>'
            mockEvents1[0].response.statusCode == 201

            mockEvents1[1].request.text == '<request15/>'
            !mockEvents1[1].request.headers?.headers?.empty
            mockEvents1[1].request.queryParams.queryParams == []
            mockEvents1[1].request.path.pathParts == ['testEndpoint', 'hello']
            !mockEvents1[1].response.headers?.headers?.empty
            mockEvents1[1].response.text == '<goodResponseRest-request15/>'
            mockEvents1[1].response.statusCode == 201
        when:
            List<MockEventReport> mockEvents2 = remoteMockServer.removeMock('testRest2')
        then:
            mockEvents2.size() == 1
            mockEvents2[0].request.text == '<reqXYZ/>'
            !mockEvents2[0].request.headers?.headers?.empty
            mockEvents2[0].request.queryParams.queryParams.find { it.name == 'id' }?.value == '123'
            mockEvents2[0].request.path.pathParts == ['testEndpoint']
            mockEvents2[0].response.headers.headers.find { it.name == 'aaa' }?.value == '15'
            mockEvents2[0].response.text == '<goodResponseRest/>'
            mockEvents2[0].response.statusCode == 202
    }

    def "should get mock report when peeking mock"() {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name()[0..6] == 'request' }''',
                    response: '''{req -> "<goodResponseRest-${req.xml.name()}/>"}''',
                    statusCode: 201,
                    responseHeaders: '''{req -> ['aaa':'14']}''',
                    soap: false
            ))
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest2',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'reqXYZ' }''',
                    response: '''{req -> "<goodResponseRest/>"}''',
                    statusCode: 202,
                    responseHeaders: '''{req -> ['aaa':'15']}''',
                    soap: false
            ))
        when:
            HttpPost post1 = new HttpPost('http://localhost:9999/testEndpoint')
            post1.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response1 = client.execute(post1)
        then:
            GPathResult restPostResponse1 = Util.extractXmlResponse(response1)
            restPostResponse1.name() == 'goodResponseRest-request'
        when:
            HttpPost post2 = new HttpPost('http://localhost:9999/testEndpoint/hello')
            post2.entity = new StringEntity('<request15/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response2 = client.execute(post2)
        then:
            GPathResult restPostResponse2 = Util.extractXmlResponse(response2)
            restPostResponse2.name() == 'goodResponseRest-request15'
        when:
            HttpPost post3 = new HttpPost('http://localhost:9999/testEndpoint?id=123')
            post3.entity = new StringEntity('<reqXYZ/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response3 = client.execute(post3)
        then:
            GPathResult restPostResponse3 = Util.extractXmlResponse(response3)
            restPostResponse3.name() == 'goodResponseRest'
        when:
            List<MockEventReport> mockEvents1 = remoteMockServer.peekMock('testRest')
        then:
            mockEvents1.size() == 2
            mockEvents1[0].request.text == '<request/>'
            !mockEvents1[0].request.headers?.headers?.empty
            mockEvents1[0].request.queryParams.queryParams == []
            mockEvents1[0].request.path.pathParts == ['testEndpoint']
            !mockEvents1[0].response.headers?.headers?.empty
            mockEvents1[0].response.text == '<goodResponseRest-request/>'
            mockEvents1[0].response.statusCode == 201

            mockEvents1[1].request.text == '<request15/>'
            !mockEvents1[1].request.headers?.headers?.empty
            mockEvents1[1].request.queryParams.queryParams == []
            mockEvents1[1].request.path.pathParts == ['testEndpoint', 'hello']
            !mockEvents1[1].response.headers?.headers?.empty
            mockEvents1[1].response.text == '<goodResponseRest-request15/>'
            mockEvents1[1].response.statusCode == 201
        when:
            List<MockEventReport> mockEvents2 = remoteMockServer.peekMock('testRest2')
        then:
            mockEvents2.size() == 1
            mockEvents2[0].request.text == '<reqXYZ/>'
            !mockEvents2[0].request.headers?.headers?.empty
            mockEvents2[0].request.queryParams.queryParams.find{it.name == 'id'}?.value == '123'
            mockEvents2[0].request.path.pathParts == ['testEndpoint']
            mockEvents2[0].response.headers.headers.find {it.name == 'aaa'}?.value == '15'
            mockEvents2[0].response.text == '<goodResponseRest/>'
            mockEvents2[0].response.statusCode == 202
    }

    @Unroll
    def "should return mock report with #mockEvents events when deleting mock with flag skip mock = #skipReport"() {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name()[0..6] == 'request' }''',
                    response: '''{req -> "<goodResponseRest-${req.xml.name()}/>"}''',
                    statusCode: 201,
                    responseHeaders: '''{req -> ['aaa':'14']}''',
                    soap: false
            ))
        when:
            HttpPost post1 = new HttpPost('http://localhost:9999/testEndpoint')
            post1.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response1 = client.execute(post1)
        then:
            GPathResult restPostResponse1 = Util.extractXmlResponse(response1)
            restPostResponse1.name() == 'goodResponseRest-request'
        expect:
            remoteMockServer.removeMock('testRest', skipReport).size() == mockEvents
        where:
            skipReport | mockEvents
            false      | 1
            true       | 0
    }

    @Unroll
    def "should reject mock when it has System.exit in closure"() {
        when:
            remoteMockServer.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: 'testEndpoint',
                    port: 9999,
                    predicate: predicate,
                    response: '''{req -> "<goodResponseRest-${req.xml.name()}/>"}''',
                    soap: false
            ))
        then:
            thrown(InvalidMockDefinition)
        expect:
            remoteMockServer.listMocks() == []
        where:
            predicate << [
                    '''{req -> System.exit(-1); req.xml.name() == 'request'}''',
                    '''{req -> System     .exit(-1); req.xml.name() == 'request'}''',
                    '''{req -> System

                        .exit(-1); req.xml.name() == 'request'}''',
                    '''{req -> System.    exit(-1); req.xml.name() == 'request'}''',
                    '''{req -> System.exit   (-1); req.xml.name() == 'request'}'''
            ]
    }
}
