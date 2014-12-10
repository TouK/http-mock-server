package pl.touk.mockserver.server

import groovy.util.slurpersupport.GPathResult
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
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
                    predicate: '''{xml -> xml.name() == 'request'}''',
                    response: '''{xml -> "<goodResponseRest-${xml.name()}/>"}''',
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
                    predicate: '''{xml -> xml.name() == 'request'}''',
                    response: '''{xml -> "<goodResponseSoap-${xml.name()}/>"}''',
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

    def "should not remove when it does not exist"() {
        when:
            controlServerClient.removeMock('testSoap')
        then:
            thrown(MockDoesNotExist)
        expect:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{xml -> xml.name() == 'request'}''',
                    response: '''{xml -> "<goodResponseSoap-${xml.name()}/>"}''',
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
                    predicate: '''{xml -> xml.name() == 'request'}''',
                    response: '''{xml -> "<goodResponseSoap-${xml.name()}/>"}''',
                    soap: true
            ))
        when:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint2',
                    port: 9998,
                    predicate: '''{xml -> xml.name() == 'request'}''',
                    response: '''{xml -> "<goodResponseSoap-${xml.name()}/>"}''',
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
                    predicate: '''{xml -> xml.name() == 'request'}''',
                    response: '''{xml -> "<goodResponseSoap-${xml.name()}/>"}''',
                    soap: true
            ))
        and:
            controlServerClient.removeMock('testSoap') == 0
        and:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{xml -> xml.name() == 'request2'}''',
                    response: '''{xml -> "<goodResponseSoap2-${xml.name()}/>"}''',
                    soap: true
            ))
    }

    def "should add simultaneously working post and rest mocks with the same predicate and endpoint nad port"() {
        given:
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{xml -> xml.name() == 'request'}''',
                    response: '''{xml -> "<goodResponseRest-${xml.name()}/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testSoap',
                    path: '/testEndpoint',
                    port: 9999,
                    predicate: '''{xml -> xml.name() == 'request'}''',
                    response: '''{xml -> "<goodResponseSoap-${xml.name()}/>"}''',
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
                    predicate: '''{xml -> xml.name() == 'request1'}''',
                    response: '''{xml -> "<goodResponseRest1/>"}'''
            ))
            controlServerClient.addMock(new AddMockRequestData(
                    name: 'testRest2',
                    path: secondPath,
                    port: secondPort,
                    predicate: '''{xml -> xml.name() == 'request2'}''',
                    response: '''{xml -> "<goodResponseRest2/>"}'''
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

    //TODO    def "should get mock report"(){}
    //TODO    def "should get list mocks"(){}
    //TODO    def "should dispatch rest mock with response code"(){}
    //TODO    def "should dispatch rest mock with post method"(){}
    //TODO    def "should dispatch rest mock with post method and request headers"(){}
    //TODO    def "should dispatch rest mock with post method and response headers"(){}
    //TODO    def "should dispatch rest mock with get method"(){}
    //TODO    def "should dispatch rest mock with get method and request headers"(){}
    //TODO    def "should dispatch rest mock with get method and response headers"(){}
    //TODO    def "should dispatch rest mock with put method"(){}
    //TODO    def "should dispatch rest mock with put method and request headers"(){}
    //TODO    def "should dispatch rest mock with put method and response headers"(){}
    //TODO    def "should dispatch rest mock with delete method"(){}
    //TODO    def "should dispatch rest mock with delete method and request headers"(){}
    //TODO    def "should dispatch rest mock with delete method and response headers"(){}
    //TODO    def "should dispatch rest mocks with all methods"(){}
}
