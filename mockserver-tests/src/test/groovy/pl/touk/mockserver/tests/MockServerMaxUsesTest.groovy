package pl.touk.mockserver.tests


import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import pl.touk.mockserver.api.request.AddMock
import pl.touk.mockserver.client.RemoteMockServer
import pl.touk.mockserver.server.HttpMockServer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class MockServerMaxUsesTest extends Specification {

    RemoteMockServer remoteMockServer

    @AutoCleanup('stop')
    HttpMockServer httpMockServer

    @Shared
    CloseableHttpClient client = HttpClients.createDefault()

    def setup() {
        httpMockServer = new HttpMockServer(9000)
        remoteMockServer = new RemoteMockServer('localhost', 9000)
    }

    def 'should return two mocks in order'() {
        given:'mock with predicate is given but for only one use'
            remoteMockServer.addMock(new AddMock(
                name: 'mock1',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock1'}''',
                maxUses: 1
            ))
        and:'mock with the same predicate is given'
            remoteMockServer.addMock(new AddMock(
                name: 'mock2',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock2'}''',
            ))
        when:'we call the first time'
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:'first mock should be returned and expired'
            response.entity.content.text == 'mock1'
        when:'we call the second time using the same request'
            CloseableHttpResponse response2 = client.execute(restPost)
        then:'second mock should be returned'
            response2.entity.content.text == 'mock2'
        when:'we call the third time using the same request'
            CloseableHttpResponse response3 = client.execute(restPost)
        then:'second mock should be returned, because it has unlimited uses'
            response3.entity.content.text == 'mock2'
    }

    def 'should return two mocks in order but only once'() {
        given:'mock with predicate is given but for only one use'
            remoteMockServer.addMock(new AddMock(
                name: 'mock1',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock1'}''',
                maxUses: 1
            ))
        and:'mock with the same predicate is given'
            remoteMockServer.addMock(new AddMock(
                name: 'mock2',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock2'}''',
                maxUses: 1,
            ))
        when:'we call the first time'
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:'first mock should be returned and expired'
            response.entity.content.text == 'mock1'
        when:'we call the second time using the same request'
            CloseableHttpResponse response2 = client.execute(restPost)
        then:'second mock should be returned'
            response2.entity.content.text == 'mock2'
        when:'we call the third time using the same request'
            CloseableHttpResponse response3 = client.execute(restPost)
        then:'no mock should be found'
            response3.statusLine.statusCode == 404
        and:'mock should exist'
            remoteMockServer.listMocks().find { it.name == 'mock1' } != null
    }

    def 'should return two mocks in cyclic order'() {
        given:'mock with predicate is given but for only one use'
            remoteMockServer.addMock(new AddMock(
                name: 'mock1',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock1'}''',
                maxUses: 1,
                cyclic: true,
                preserveHistory: true
            ))
        and:'mock with the same predicate is given'
            remoteMockServer.addMock(new AddMock(
                name: 'mock2',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock2'}''',
                maxUses: 1,
                cyclic: true
            ))
        when:'we call the first time'
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:'first mock should be returned and expired'
            response.entity.content.text == 'mock1'
        when:'we call the second time using the same request'
            CloseableHttpResponse response2 = client.execute(restPost)
        then:'second mock should be returned and expired'
            response2.entity.content.text == 'mock2'
        when:'we call the third time using the same request'
            CloseableHttpResponse response3 = client.execute(restPost)
        then:'first mock should be returned, because these mocks are cyclic'
            response3.entity.content.text == 'mock1'
        when:'we call the fourth time using the same request'
            CloseableHttpResponse response4 = client.execute(restPost)
        then:'second mock should be returned, because these mocks are cyclic'
            response4.entity.content.text == 'mock2'
        and:
            remoteMockServer.peekMock('mock1').size() == 2
    }

    def 'should return two mocks with the same request interjected by another'() {
        given:'mock with predicate is given but for only one use'
            remoteMockServer.addMock(new AddMock(
                name: 'mock1',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock1'}''',
                maxUses: 1,
                cyclic: true
            ))
        and:'mock with the same predicate is given'
            remoteMockServer.addMock(new AddMock(
                name: 'mock2',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock2'}''',
                maxUses: 1,
                cyclic: true
            ))
        and:'mock with other predicate is given'
            remoteMockServer.addMock(new AddMock(
                name: 'otherMock',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'otherRequest'}''',
                response: '''{req -> 'otherMock'}'''
            ))
        when:'we call the first time'
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:'first mock should be returned and expired'
            response.entity.content.text == 'mock1'
        when:'we call other request'
            HttpPost otherRestPost = new HttpPost('http://localhost:9999/testEndpoint')
            otherRestPost.entity = new StringEntity('<otherRequest/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse otherResponse = client.execute(otherRestPost)
        then:'other mock should be called'
            otherResponse.entity.content.text == 'otherMock'
        when:'we call the second time using the same request'
            CloseableHttpResponse response2 = client.execute(restPost)
        then:'second mock should be returned and expired'
            response2.entity.content.text == 'mock2'
        when:'we call the third time using the same request'
            CloseableHttpResponse response3 = client.execute(restPost)
        then:'first mock should be returned, because these mocks are cyclic'
            response3.entity.content.text == 'mock1'
    }

    def 'should return first mock twice'() {
        given:'mock with predicate is given but for only one use'
            remoteMockServer.addMock(new AddMock(
                name: 'mock1',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock1'}''',
                maxUses: 2
            ))
        and:'mock with the same predicate is given'
            remoteMockServer.addMock(new AddMock(
                name: 'mock2',
                path: 'testEndpoint',
                port: 9999,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> 'mock2'}''',
            ))
        when:'we call the first time'
            HttpPost restPost = new HttpPost('http://localhost:9999/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:'first mock should be returned and expired'
            response.entity.content.text == 'mock1'
        when:'we call the second time using the same request'
            CloseableHttpResponse response2 = client.execute(restPost)
        then:'again first mock should be returned'
            response2.entity.content.text == 'mock1'
        when:'we call the third time using the same request'
            CloseableHttpResponse response3 = client.execute(restPost)
        then:'second mock should be returned'
            response3.entity.content.text == 'mock2'
    }

    def 'should throw exception if adding mock with incorrect maxUses'() {
        when:
            remoteMockServer.addMock(new AddMock(
                name: 'mock1',
                maxUses: 0
            ))
        then:
            thrown(RuntimeException)
    }
}
