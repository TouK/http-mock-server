package pl.touk.mockserver.client

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.slurpersupport.GPathResult
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.util.EntityUtils
import pl.touk.mockserver.api.response.ExceptionOccured
import pl.touk.mockserver.api.response.MockAdded
import pl.touk.mockserver.api.response.MockServerResponse

import javax.xml.bind.JAXBContext

@CompileStatic
@TypeChecked
class Util {
    private static
    final JAXBContext responseContext = JAXBContext.newInstance(MockAdded.package.name, MockAdded.classLoader)

    static GPathResult extractXmlResponse(CloseableHttpResponse response) {
        return new XmlSlurper().parseText(extractStringResponse(response))
    }
    static String extractStringResponse(CloseableHttpResponse response) {
        HttpEntity entity = response.entity
        String responseString = EntityUtils.toString(entity, 'UTF-8')
        EntityUtils.consumeQuietly(entity)
        return responseString
    }

    static MockServerResponse extractResponse(CloseableHttpResponse response) {
        String responseString = extractStringResponse(response)
        if (response.statusLine.statusCode == 200) {
            return responseContext.createUnmarshaller().unmarshal(new StringReader(responseString)) as MockServerResponse
        }
        ExceptionOccured exceptionOccured = responseContext.createUnmarshaller().unmarshal(new StringReader(responseString)) as ExceptionOccured
        String message = exceptionOccured.value
        if (message == 'mock already registered') {
            throw new MockAlreadyExists()
        }
        if (message == 'mock not registered') {
            throw new MockDoesNotExist()
        }
        if (message == 'mock request schema is invalid schema') {
            throw new InvalidMockRequestSchema()
        }
        throw new InvalidMockDefinition(message)
    }

    static String soap(String request) {
        return """<?xml version='1.0' encoding='UTF-8'?>
            <soap-env:Envelope xmlns:soap-env='http://schemas.xmlsoap.org/soap/envelope/' xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">
                <soap-env:Body>$request</soap-env:Body>
            </soap-env:Envelope>"""
    }

    static Object extractJsonResponse(CloseableHttpResponse response) {
        return new JsonSlurper().parseText(extractStringResponse(response))
    }

    static void consumeResponse(CloseableHttpResponse response) {
        EntityUtils.consumeQuietly(response.entity)
    }

}
