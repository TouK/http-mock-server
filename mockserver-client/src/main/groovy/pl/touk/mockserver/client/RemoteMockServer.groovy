package pl.touk.mockserver.client

import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import pl.touk.mockserver.api.request.AddMock
import pl.touk.mockserver.api.request.MockServerRequest
import pl.touk.mockserver.api.request.PeekMock
import pl.touk.mockserver.api.request.RemoveMock
import pl.touk.mockserver.api.response.*

import javax.xml.bind.JAXBContext

class RemoteMockServer {
    private final String address
    private final CloseableHttpClient client = HttpClients.createDefault()
    private static final JAXBContext requestContext = JAXBContext.newInstance(AddMock, PeekMock, RemoveMock)

    RemoteMockServer(String host, int port) {
        address = "http://$host:$port/serverControl"
    }

    void addMock(AddMock addMockData) {
        HttpPost addMockPost = new HttpPost(address)
        addMockPost.entity = buildAddMockRequest(addMockData)
        CloseableHttpResponse response = client.execute(addMockPost)
        Util.extractResponse(response)
    }

    List<MockEventReport> removeMock(String name, boolean skipReport = false) {
        HttpPost removeMockPost = new HttpPost(address)
        removeMockPost.entity = buildRemoveMockRequest(new RemoveMock(name: name, skipReport: skipReport))
        CloseableHttpResponse response = client.execute(removeMockPost)
        MockRemoved mockRemoved = Util.extractResponse(response) as MockRemoved
        return mockRemoved.mockEvents ?: []
    }

    List<MockEventReport> peekMock(String name) {
        HttpPost removeMockPost = new HttpPost(address)
        removeMockPost.entity = buildPeekMockRequest(new PeekMock(name: name))
        CloseableHttpResponse response = client.execute(removeMockPost)
        MockPeeked mockPeeked = Util.extractResponse(response) as MockPeeked
        return mockPeeked.mockEvents ?: []
    }

    private static StringEntity buildRemoveMockRequest(RemoveMock data) {
        return new StringEntity(marshallRequest(data), ContentType.create("text/xml", "UTF-8"))
    }

    private static String marshallRequest(MockServerRequest data) {
        StringWriter sw = new StringWriter()
        requestContext.createMarshaller().marshal(data, sw)
        return sw.toString()
    }

    private static StringEntity buildPeekMockRequest(PeekMock peekMock) {
        return new StringEntity(marshallRequest(peekMock), ContentType.create("text/xml", "UTF-8"))
    }

    private static StringEntity buildAddMockRequest(AddMock data) {
        return new StringEntity(marshallRequest(data), ContentType.create("text/xml", "UTF-8"))
    }

    List<MockReport> listMocks() {
        HttpGet get = new HttpGet(address)
        CloseableHttpResponse response = client.execute(get)
        Mocks mocks = Util.extractResponse(response) as Mocks
        return mocks.mocks
    }
}
