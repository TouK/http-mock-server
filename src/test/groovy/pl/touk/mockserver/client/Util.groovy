package pl.touk.mockserver.client

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.slurpersupport.GPathResult
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.util.EntityUtils

@CompileStatic
@TypeChecked
class Util {
    static GPathResult extractXmlResponse(CloseableHttpResponse response) {
        HttpEntity entity = response.entity

        GPathResult xml = new XmlSlurper().parseText(EntityUtils.toString(entity))
        EntityUtils.consumeQuietly(entity)
        return xml
    }

    static String soap(String request) {
        return """<?xml version='1.0' encoding='UTF-8'?>
            <soap-env:Envelope xmlns:soap-env='http://schemas.xmlsoap.org/soap/envelope/' xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">
                <soap-env:Body>$request</soap-env:Body>
            </soap-env:Envelope>"""
    }

    static Object extractJsonResponse(CloseableHttpResponse response) {
        HttpEntity entity = response.entity
        Object json = new JsonSlurper().parseText(EntityUtils.toString(entity))
        EntityUtils.consumeQuietly(entity)
        return json
    }

    static void consumeResponse(CloseableHttpResponse response) {
        EntityUtils.consumeQuietly(response.entity)
    }
}
