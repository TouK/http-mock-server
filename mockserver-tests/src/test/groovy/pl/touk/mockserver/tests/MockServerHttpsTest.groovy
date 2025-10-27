package pl.touk.mockserver.tests

import groovy.xml.slurpersupport.GPathResult
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
import spock.lang.AutoCleanup
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import java.security.KeyStore

@Ignore('Upgrade of Java needed')
class MockServerHttpsTest extends Specification {

    RemoteMockServer remoteMockServer = new RemoteMockServer('localhost', 19000)

    @AutoCleanup('stop')
    HttpMockServer httpMockServer = new HttpMockServer(19000)

    @Shared
    SSLContext noClientAuthSslContext = SSLContexts.custom()
        .loadTrustMaterial(trustStore())
        .build()

    @Shared
    SSLContext trustedCertificateSslContext = SSLContexts.custom()
        .loadKeyMaterial(trustedCertificateKeystore(), 'changeit'.toCharArray())
        .loadTrustMaterial(trustStore())
        .build()

    @Shared
    SSLContext untrustedCertificateSslContext = SSLContexts.custom()
        .loadKeyMaterial(untrustedCertificateKeystore(), 'changeit'.toCharArray())
        .loadTrustMaterial(trustStore())
        .build()

    def 'should handle HTTPS server' () {
        given:
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
            CloseableHttpResponse response = client(noClientAuthSslContext).execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse-request'
    }

    def 'should handle HTTPS server with client auth' () {
        given:
            remoteMockServer.addMock(new AddMock(
                name: 'testHttps',
                path: 'testEndpoint',
                port: 10443,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> "<goodResponse-${req.xml.name()}/>"}''',
                https: new Https(
                    keyPassword: 'changeit',
                    keystorePassword: 'changeit',
                    keystorePath: MockServerHttpsTest.classLoader.getResource('keystore.jks').path,
                    truststorePath: MockServerHttpsTest.classLoader.getResource('truststore.jks').path,
                    truststorePassword: 'changeit',
                    requireClientAuth: true
                ),
                soap: false
            ))
        when:
            HttpPost restPost = new HttpPost('https://localhost:10443/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            CloseableHttpResponse response = client(trustedCertificateSslContext).execute(restPost)
        then:
            GPathResult restPostResponse = Util.extractXmlResponse(response)
            restPostResponse.name() == 'goodResponse-request'
    }

    def 'should handle HTTPS server with wrong client auth' () {
        given:
            remoteMockServer.addMock(new AddMock(
                name: 'testHttps',
                path: 'testEndpoint',
                port: 10443,
                predicate: '''{req -> req.xml.name() == 'request'}''',
                response: '''{req -> "<goodResponse-${req.xml.name()}/>"}''',
                https: new Https(
                    keyPassword: 'changeit',
                    keystorePassword: 'changeit',
                    keystorePath: MockServerHttpsTest.classLoader.getResource('keystore.jks').path,
                    truststorePath: MockServerHttpsTest.classLoader.getResource('truststore.jks').path,
                    truststorePassword: 'changeit',
                    requireClientAuth: true
                ),
                soap: false
            ))
        when:
            HttpPost restPost = new HttpPost('https://localhost:10443/testEndpoint')
            restPost.entity = new StringEntity('<request/>', ContentType.create("text/xml", "UTF-8"))
            client(sslContext).execute(restPost)
        then:
            thrown(SSLHandshakeException)
        where:
            sslContext << [noClientAuthSslContext, untrustedCertificateSslContext]
    }

    private CloseableHttpClient client(SSLContext sslContext) {
        return HttpClients.custom()
            .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
            .setSslcontext(sslContext)
            .build()
    }

    private KeyStore trustedCertificateKeystore() {
        return loadKeystore('trusted.jks')
    }

    private KeyStore untrustedCertificateKeystore() {
        return loadKeystore('untrusted.jks')
    }

    private KeyStore trustStore() {
        return loadKeystore('truststore.jks')
    }

    private KeyStore loadKeystore(String fileName) {
        KeyStore truststore = KeyStore.getInstance(KeyStore.defaultType)
        truststore.load(new FileInputStream(MockServerHttpsTest.classLoader.getResource(fileName).path), "changeit".toCharArray());
        return truststore
    }
}
