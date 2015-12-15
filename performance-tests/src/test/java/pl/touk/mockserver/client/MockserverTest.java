package pl.touk.mockserver.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.ThreadParams;
import pl.touk.mockserver.api.request.AddMock;
import pl.touk.mockserver.server.HttpMockServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MockserverTest {
    HttpMockServer httpMockServer;

    @Setup
    public void prepareMockServer(BenchmarkParams params) {
        try {
            httpMockServer = new HttpMockServer(9999);
        } catch (Exception e) {
            //OK
        }
    }

    @TearDown
    public void stopMockServer() {
        try {
            httpMockServer.stop();
        } catch (Exception e) {
        }
    }

    @State(Scope.Thread)
    public static class TestState {
        RemoteMockServer remoteMockServer;
        HttpClient httpClient;
        int current;

        @Setup
        public void prepareMockServer(ThreadParams params) {
            remoteMockServer = new RemoteMockServer("localhost", 9999);
            httpClient = HttpClients.createDefault();
            current = params.getThreadIndex();
        }
    }


    @Benchmark
    @Measurement(iterations = 60)
    @Fork(warmups = 1, value = 1)
    @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
    @Warmup(iterations = 10)
    @Threads(4)
    public void shouldHandleManyRequestsSimultaneously(TestState testState) throws IOException {
        int current = testState.current;
        int endpointNumber = current % 10;
        int port = 9000 + (current % 7);
        AddMock addMock = new AddMock();
        addMock.setName("testRest" + current);
        addMock.setPath("testEndpoint" + endpointNumber);
        addMock.setPort(port);
        addMock.setPredicate("{req -> req.xml.name() == 'request" + current + "' }");
        addMock.setResponse("{req -> '<goodResponse" + current + "/>'}");
        testState.remoteMockServer.addMock(addMock);
        HttpPost restPost = new HttpPost("http://localhost:" + port + "/testEndpoint" + endpointNumber);
        restPost.setEntity(new StringEntity("<request" + current + "/>", ContentType.create("text/xml", "UTF-8")));
        CloseableHttpResponse response = (CloseableHttpResponse) testState.httpClient.execute(restPost);
        String stringResponse = Util.extractStringResponse(response);
        testState.remoteMockServer.removeMock("testRest" + current, true);
        assert stringResponse.equals("<goodResponse" + current + "/>");
    }

}
