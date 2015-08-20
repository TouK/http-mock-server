package pl.touk.mockserver.client

import groovy.util.slurpersupport.GPathResult
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients

class RemoteMockServer {
    private final String address
    private final CloseableHttpClient client = HttpClients.createDefault()

    RemoteMockServer(String host, int port) {
        address = "http://$host:$port/serverControl"
    }

    void addMock(AddMockRequestData addMockRequestData) {
        HttpPost addMockPost = new HttpPost(address)
        addMockPost.entity = buildAddMockRequest(addMockRequestData)
        CloseableHttpResponse response = client.execute(addMockPost)
        GPathResult responseXml = Util.extractXmlResponse(response)
        if (responseXml.name() != 'mockAdded') {
            if (responseXml.text() == 'mock already registered') {
                throw new MockAlreadyExists()

            }
            throw new InvalidMockDefinition(responseXml.text())
        }
    }

    List<MockEvent> removeMock(String name, boolean skipReport = false) {
        HttpPost removeMockPost = new HttpPost(address)
        removeMockPost.entity = buildRemoveMockRequest(new RemoveMockRequestData(name: name, skipReport: skipReport))
        CloseableHttpResponse response = client.execute(removeMockPost)
        GPathResult responseXml = Util.extractXmlResponse(response)
        if (responseXml.name() == 'mockRemoved') {
            return responseXml.'mockEvent'.collect {
                new MockEvent(mockRequestFromXml(it.request), mockResponseFromXml(it.response))
            }
        }
        throw new MockDoesNotExist()
    }

    List<MockEvent> peekMock(String name) {
        HttpPost removeMockPost = new HttpPost(address)
        removeMockPost.entity = buildPeekMockRequest(new PeekMockRequestData(name: name))
        CloseableHttpResponse response = client.execute(removeMockPost)
        GPathResult responseXml = Util.extractXmlResponse(response)
        if (responseXml.name() == 'mockPeeked') {
            return responseXml.'mockEvent'.collect {
                new MockEvent(mockRequestFromXml(it.request), mockResponseFromXml(it.response))
            }
        }
        throw new MockDoesNotExist()
    }

    private static MockResponse mockResponseFromXml(GPathResult xml) {
        return new MockResponse(xml.statusCode.text() as int, xml.text.text(), xml.headers.param.collectEntries {
            [(it.@name.text()): it.text()]
        })
    }

    private static MockRequest mockRequestFromXml(GPathResult xml) {
        return new MockRequest(
                xml.text.text(),
                xml.headers.param.collectEntries { [(it.@name.text()): it.text()] },
                xml.query.param.collectEntries { [(it.@name.text()): it.text()] },
                xml.path.elem*.text()
        )
    }

    private static StringEntity buildRemoveMockRequest(RemoveMockRequestData data) {
        return new StringEntity("""\
            <removeMock>
                <name>${data.name}</name>
                <skipReport>${data.skipReport}</skipReport>
            </removeMock>
        """, ContentType.create("text/xml", "UTF-8"))
    }

    private static StringEntity buildPeekMockRequest(PeekMockRequestData data) {
        return new StringEntity("""\
            <peekMock>
                <name>${data.name}</name>
            </peekMock>
        """, ContentType.create("text/xml", "UTF-8"))
    }

    private static StringEntity buildAddMockRequest(AddMockRequestData data) {
        return new StringEntity("""\
            <addMock>
                <name>${data.name}</name>
                <path>${data.path}</path>
                <port>${data.port}</port>
                ${data.predicate ? "<predicate>${data.predicate}</predicate>" : ''}
                ${data.response ? "<response>${data.response}</response>" : ''}
                ${data.soap != null ? "<soap>${data.soap}</soap>" : ''}
                ${data.statusCode ? "<statusCode>${data.statusCode}</statusCode>" : ''}
                ${data.method ? "<method>${data.method}</method>" : ''}
                ${data.responseHeaders ? "<responseHeaders>${data.responseHeaders}</responseHeaders>" : ''}
            </addMock>
        """, ContentType.create("text/xml", "UTF-8"))
    }

    List<RegisteredMock> listMocks() {
        HttpGet get = new HttpGet(address)
        CloseableHttpResponse response = client.execute(get)
        GPathResult xml = Util.extractXmlResponse(response)
        if (xml.name() == 'mocks') {
            return xml.mock.collect {
                new RegisteredMock(it.name.text(), it.path.text(), it.port.text() as int, it.predicate.text(), it.response.text(), it.responseHeaders.text())
            }
        }
        return []
    }
}
