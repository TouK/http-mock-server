package pl.touk.mockserver.server

import pl.touk.mockserver.server.HttpMockServer
import groovy.util.slurpersupport.GPathResult
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import pl.touk.mockserver.client.AddMockRequestData
import pl.touk.mockserver.client.ControlServerClient
import pl.touk.mockserver.client.Util
import spock.lang.Shared
import spock.lang.Specification

class MockServerTest extends Specification{

    ControlServerClient controlServerClient

    HttpMockServer httpMockServer

    @Shared
    CloseableHttpClient client = HttpClients.createDefault()

    def setup(){
        httpMockServer = new HttpMockServer(9000)
        controlServerClient = new ControlServerClient('localhost', 9000)
    }

    def cleanup(){
        httpMockServer.stop()
    }

    def "should add working rest mock on endpoint"(){
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
            restPost.entity = new StringEntity('<request/>',ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponseRest-request'
        expect:
            controlServerClient.removeMock('testRest') == 1
    }

    def "should add soap mock on endpoint"(){
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
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity(Util.soap('<request/>'),ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'Envelope'
            restPostResponse.Body.'goodResponseSoap-request'.size() == 1
        expect:
            controlServerClient.removeMock('testSoap') == 1
    }

//TODO    def "should add simultaneously working post and rest mocks with the same predicate and endpoint nad port"(){}"
    //TODO    def "should add mock minimal(){}"
    //TODO    def "should dispatch mocks with the same predicates on another ports"
    //TODO    def "should not add mock with existing name"(){}
    //TODO    def "should not remove the same mock two times"(){}
    //TODO    def "should add mock after deleteing old mock with the same name"(){}
    //TODO    def "should dispatch rest mocks with the another predicates"(){}
    //TODO    def "should dispatch soap mocks with the another predicates"(){}
    //TODO    def "should dispatch rest and soap mocks with the same predicates"(){}
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
