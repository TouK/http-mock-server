package pl.touk.mockserver.client

import groovy.util.slurpersupport.GPathResult
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients

class ControlServerClient {
    private final String address
    private final CloseableHttpClient client = HttpClients.createDefault()

    ControlServerClient(String host, int port) {
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
            throw new InvalidMockDefinitionException(responseXml.text())
        }
    }

    int removeMock(String name) {
        HttpPost removeMockPost = new HttpPost(address)
        removeMockPost.entity = buildRemoveMockRequest(new RemoveMockRequestData(name: name))
        CloseableHttpResponse response = client.execute(removeMockPost)
        GPathResult responseXml = Util.extractXmlResponse(response)
        if (responseXml.name() == 'mockRemoved') {
            return responseXml.text() as int
        }
        throw new MockDoesNotExist()
    }

    private static StringEntity buildRemoveMockRequest(RemoveMockRequestData data) {
        return new StringEntity("""\
            <removeMock>
                <name>${data.name}</name>
            </removeMock>
        """, ContentType.create("text/xml", "UTF-8"))
    }

    private static StringEntity buildAddMockRequest(AddMockRequestData data) {
        return new StringEntity("""\
            <addMock>
                <name>${data.name}</name>
                <path>${data.path}</path>
                <port>${data.port}</port>
                ${data.predicate != null ? "<predicate>${data.predicate}</predicate>" : ''}
                ${data.response != null ? "<response>${data.response}</response>" : ''}
                ${data.soap != null ? "<soap>${data.soap}</soap>" : ''}
                ${data.statusCode != null ? "<statusCode>${data.statusCode}</statusCode>" : ''}
            </addMock>
        """, ContentType.create("text/xml", "UTF-8"))
    }
}
