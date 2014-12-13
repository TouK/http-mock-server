package pl.touk.mockserver.server

import groovy.util.slurpersupport.GPathResult
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import pl.touk.mockserver.client.AddMockRequestData
import pl.touk.mockserver.client.ControlServerClient
import pl.touk.mockserver.client.Util
import spock.lang.Specification

class ServerMockPT extends Specification {

    def "should handle many request simultaneously"() {
        given:
            HttpMockServer httpMockServer = new HttpMockServer()
            ControlServerClient controlServerClient = new ControlServerClient("localhost", 9999)
            HttpClient client = HttpClients.createDefault()
            int requestAmount = 1000
            GPathResult[] responses = new GPathResult[requestAmount]
            Thread[] threads = new Thread[requestAmount]
            for (int i = 0; i < requestAmount; ++i) {
                int current = i
                threads[i] = new Thread({
                    int endpointNumber = current % 10
                    int port = 9000 + (current % 7)
                    controlServerClient.addMock(new AddMockRequestData(
                            name: "testRest$current",
                            path: "testEndpoint$endpointNumber",
                            port: port,
                            predicate: """{req -> req.xml.name() == 'request$current'}""",
                            response: """{req -> "<goodResponse$current/>"}"""
                    ))
                    HttpPost restPost = new HttpPost("http://localhost:$port/testEndpoint$endpointNumber")
                    restPost.entity = new StringEntity("<request$current/>", ContentType.create("text/xml", "UTF-8"))
                    CloseableHttpResponse response = client.execute(restPost)
                    responses[current] = Util.extractXmlResponse(response)
                    assert controlServerClient.removeMock("testRest$current") == 1
                })
            }
        when:
            threads*.start()
            Thread.sleep(60000)
        then:
            responses.eachWithIndex { res, i -> println "Checking $i"; assert res.name() == "goodResponse$i" }
        cleanup:
            httpMockServer.stop()
    }
}
