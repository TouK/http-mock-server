package pl.touk.mockserver.tests

import groovy.util.slurpersupport.GPathResult
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.SSLContexts
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import pl.touk.mockserver.api.common.Https
import pl.touk.mockserver.api.request.AddMock
import pl.touk.mockserver.client.RemoteMockServer
import pl.touk.mockserver.client.Util
import pl.touk.mockserver.server.HttpMockServer
import spock.lang.Shared
import spock.lang.Specification

import javax.net.ssl.SSLContext
import java.security.KeyStore

class MockServerHttpsTest extends Specification {

    RemoteMockServer remoteMockServer

    HttpMockServer httpMockServer

    @Shared
    SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(trustStore())
        .build()

    @Shared
    CloseableHttpClient client = HttpClients.custom()
        .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
        .setSslcontext(sslContext)
        .build()

    def setup() {
        httpMockServer = new HttpMockServer(19000)
        remoteMockServer = new RemoteMockServer('localhost', 19000)
    }

    def cleanup() {
        httpMockServer.stop()
    }

    def 'should handle HTTPS server' () {
        expect:
            remoteMockServer.addMock(new AddMock(
                    name: 'testHttps',
                    path: 'testEndpoint',
                    port: 10443,
                    predicate: '''{req -> req.xml.name() == 'request'}''',
                    response: '''{req -> "<goodResponse-${req.xml.name()}/>"}''',
                    https: new Https(
                        keyPassword: 'changeit',
                        keystorePassword: 'changeit',
                        keystorePath: MockServerHttpsTest.classLoader.getResource('keystore.jks').path
                    ),
                    soap: false
            ))
        when:
            HttpPost restPost = new HttpPost('https://localhost:10443/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client.execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse-request'
        and:
            remoteMockServer.removeMock('testHttps')?.size() == 1
    }

    private KeyStore trustStore() {
        KeyStore truststore  = KeyStore.getInstance(KeyStore.defaultType)
        truststore.load(new FileInputStream(MockServerHttpsTest.classLoader.getResource('truststore.jks').path), "changeit".toCharArray());
        return truststore
    }
}
