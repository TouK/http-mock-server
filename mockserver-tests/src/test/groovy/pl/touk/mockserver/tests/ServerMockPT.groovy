package pl.touk.mockserver.tests

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import pl.touk.mockserver.api.request.AddMock
import pl.touk.mockserver.client.RemoteMockServer
import pl.touk.mockserver.client.Util
import pl.touk.mockserver.server.HttpMockServer
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ServerMockPT extends Specification {


    @Timeout(value = 60)
    def "should handle many request simultaneously"() {
        given:
            HttpClient client = HttpClients.createDefault()
            HttpMockServer httpMockServer = new HttpMockServer()
            RemoteMockServer controlServerClient = new RemoteMockServer("localhost", 9999)
            int requestAmount = 1000
            String[] responses = new String[requestAmount]
            ExecutorService executorService = Executors.newFixedThreadPool(20)
            for (int i = 0; i < requestAmount; ++i) {
                int current = i
                executorService.submit {
                    int endpointNumber = current % 10
                    int port = 9000 + (current % 7)
                    controlServerClient.addMock(new AddMock(
                            name: "testRest$current",
                            path: "testEndpoint$endpointNumber",
                            port: port,
                            predicate: """{req -> req.xml.name() == 'request$current'}""",
                            response: """{req -> "<goodResponse$current/>"}"""
                    ))
                    HttpPost restPost = new HttpPost("http://localhost:$port/testEndpoint$endpointNumber")
                    restPost.entity = new StringEntity("<request$current/>", ContentType.create("text/xml", "UTF-8"))
                    CloseableHttpResponse response = client.execute(restPost)
                    responses[current] = Util.extractStringResponse(response)
                    assert controlServerClient.removeMock("testRest$current", false).size() == 1
                }
            }
        when:
            executorService.awaitTermination(60, TimeUnit.SECONDS)
        then:
            responses.eachWithIndex { res, i -> assert new XmlSlurper().parseText(res).name() == "goodResponse$i" as String }
        cleanup:
            httpMockServer.stop()
    }
}
