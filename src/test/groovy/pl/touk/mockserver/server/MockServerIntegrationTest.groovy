package pl.touk.mockserver.server

import groovy.util.slurpersupport.GPathResult
import org.apache.http.client.methods.*
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import pl.touk.mockserver.client.*
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class MockServerIntegrationTest extends Specification {

    ControlServerClient controlServerClient

    HttpMockServer httpMockServer

    @Shared
    CloseableHttpClient client = HttpClients.createDefault()

    def setup() {
        httpMockServer = new HttpMockServer(9000)
        controlServerClient = new ControlServerClient('localhost', 9000)
    }

    def cleanup() {
        httpMockServer.stop()
    }

    def "should add working rest mock on endpoint"() {
        expect:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
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
            controlServerClient.removeMock('testRest') == 1
    }

    def "should add soap mock on endpoint"() {
        expect:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
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
            controlServerClient.removeMock('testSoap') == 1
    }

    def "should not remove mock when it does not exist"() {
        when:
            controlServerClient.removeMock('testSoap')
        then:
            thrown(MockDoesNotExist)
        expect:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        and:
            controlServerClient.removeMock('testSoap') == 0
        when:
            controlServerClient.removeMock('testSoap')
        then:
            thrown(MockDoesNotExist)
    }

    def "should not add mock with existing name"() {
        expect:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        when:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint2',
                    port: 9998,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        then:
            thrown(MockAlreadyExists)
    }

    def "should add mock after deleting old mock with the same name"() {
        expect:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseSoap-${req.name()}/>"}''',
                    soap: true
            ))
        and:
            controlServerClient.removeMock('testSoap') == 0
        and:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request2'}''',
                    response: '''{req -> "<goodResponseSoap2-${req.xml.name()}/>"}''',
                    soap: true
            ))
    }

    def "should add simultaneously working post and rest mocks with the same predicate and endpoint nad port"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponseRest-${req.xml.name()}/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
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
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest1',
                    path: '/test1',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request1'}''',
                    response: '''{req -> "<goodResponseRest1/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: secondPath,
                    port: secondPort,
                    predicate: '''{req -> req.xml.name() == 'request2'}''',
                    response: '''{req -> "<goodResponseRest2/>"}'''
            ))
            HttpPost firstRequest = new HttpPost('http://localhost:9999/test1')
            firstRequest.entity = new StringEntity('<request1/>', ContentType.create("text/xml", "UTF-8"))
            HttpPost secondRequest = new HttpPost("http://localhost:${secondPort}${secondPath}")
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
            9999       | '/test1'   | 'the same port and path'
            9998       | '/test1'   | 'the same path and another port'
            9999       | '/test2'   | 'the same port and another path'
            9998       | '/test2'   | 'another port and path'
    }

    @Unroll
    def "should dispatch rest mock with response code"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest1',
                    path: '/test1',
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

    def "should return response code 404 and error body when mocks does not apply"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest1',
                    path: '/test1',
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
            secondXmlResponse.name() == 'invalidInput'
    }

    def "should inform that there was problem during adding mock - invalid port"() {
        when:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint2',
                    port: -1,
                    predicate: '''{_ -> true}''',
                    response: '''{req -> "<goodResponseSoap-${req.xml.name()}/>"}''',
                    soap: true
            ))
        then:
            thrown(InvalidMockDefinitionException)
    }

    def "should dispatch rest mock with get method"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: '/testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<getResponse/>"}''',
                    method: AddMockRequestData.Method.GET
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
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: '/testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<traceResponse/>"}''',
                    method: AddMockRequestData.Method.TRACE
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
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: '/testEndpoint',
                    port: 9999,
                    method: AddMockRequestData.Method.HEAD
            ))
            HttpHead restHead = new HttpHead('http://localhost:9999/testEndpoint')
        when:
            CloseableHttpResponse response = client.execute(restHead)
        then:
            response.statusLine.statusCode == 200
            EntityUtils.consumeQuietly(response.entity)
    }

    def "should dispatch rest mock with options method"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: '/testEndpoint',
                    port: 9999,
                    method: AddMockRequestData.Method.OPTIONS
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
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/test1',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: '/test1',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request1'}''',
                    response: '''{_ -> "<goodResponseRest1/>"}''',
                    method: AddMockRequestData.Method.PUT
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
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/test1',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: '/test1',
                    port: 9999,
                    response: '''{_ -> "<goodResponseRest1/>"}''',
                    method: AddMockRequestData.Method.DELETE
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
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/test1',
                    port: 9999,
                    response: '''{_ -> "<defaultResponse/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: '/test1',
                    port: 9999,
                    predicate: '''{req -> req.xml.name() == 'request1'}''',
                    response: '''{_ -> "<goodResponseRest1/>"}''',
                    method: AddMockRequestData.Method.PATCH
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
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
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
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
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
            GPathResult badRestPostResponse = Util.extractXmlResponse(badResponse)
            badRestPostResponse.name() == 'invalidInput'
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse'
    }

    def "should add mock that accepts only when certain query params exists"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
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
            GPathResult badRestPostResponse = Util.extractXmlResponse(badResponse)
            badRestPostResponse.name() == 'invalidInput'
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse'
    }

    def "should add mock that accepts only when request has specific body"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
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
            GPathResult badRestPostResponse = Util.extractXmlResponse(badResponse)
            badRestPostResponse.name() == 'invalidInput'
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse'
    }

    def "should add mock which response json to json"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
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
            GPathResult badRestPostResponse = Util.extractXmlResponse(badResponse)
            badRestPostResponse.name() == 'invalidInput'
        when:
            CloseableHttpResponse response = client.execute(restPost)
        then:
            Object restPostResponse = Util.extractJsonResponse(response)
            restPostResponse.name == 'goodResponse-1'
    }

    //TODO    def "should get mock report"(){}
    //TODO    def "should get list mocks"(){}
    //TODO    def "should validate mock when creating"
}
