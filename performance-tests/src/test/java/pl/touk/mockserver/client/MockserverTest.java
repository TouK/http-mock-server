package pl.touk.mockserver.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.ThreadParams;
import pl.touk.mockserver.api.request.AddMock;
import pl.touk.mockserver.server.HttpMockServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MockserverTest {
    HttpMockServer httpMockServer;

    @Param({"9000", "8000"})
    int initialPort;

    @Setup
    public void prepareMockServer(BenchmarkParams params) {
        httpMockServer = new HttpMockServer(9999);
    }

    @TearDown
    public void stopMockServer() {
        httpMockServer.stop();
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
    @Measurement(iterations = 10)
    @Fork(value = 2)
    @BenchmarkMode({Mode.AverageTime, Mode.Throughput, Mode.SampleTime})
    @Warmup(iterations = 5)
    @Threads(4)
    public void shouldHandleManyRequestsSimultaneously(TestState testState, Blackhole bh) throws IOException {
        int current = testState.current;
        int endpointNumber = current % 10;
        int port = initialPort + (current % 7);
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
        bh.consume(stringResponse);
    }

}
