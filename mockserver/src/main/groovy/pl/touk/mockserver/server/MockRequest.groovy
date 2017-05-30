package pl.touk.mockserver.server

import com.sun.net.httpserver.Headers
import groovy.json.JsonSlurper
import groovy.transform.PackageScope
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil

@PackageScope
class MockRequest {
    final String text
    final Map<String, String> headers
    final Map<String, String> query
    final GPathResult xml
    final GPathResult soap
    final Object json
    final List<String> path

    MockRequest(String text, Headers headers, URI uri) {
        this.text = text
        this.headers = headersToMap(headers)
        this.query = queryParamsToMap(uri.query)
        this.xml = inputToXml(text)
        this.soap = inputToSoap(xml)
        this.json = inputToJson(text)
        this.path = uri.path.split('/').findAll()
    }

    private static GPathResult inputToXml(String text) {
        if (!text.startsWith('<')) {
            return null
        }
        try {
            return new XmlSlurper().parseText(text)
        } catch (Exception _) {
            return null
        }
    }

    private static GPathResult inputToSoap(GPathResult xml) {
        try {
            if (xml != null && xml.name() == 'Envelope' && xml.Body.size() > 0) {
                return getSoapBodyContent(xml)
            } else {
                return null
            }
        } catch (Exception _) {
            return null
        }
    }

    private static GPathResult getSoapBodyContent(GPathResult xml) {
        return xml.Body.'**'[1] as GPathResult
    }

    private static Object inputToJson(String text) {
        if (!text.startsWith('[') &&  !text.startsWith('{')) {
            return null
        }
        try {
            return new JsonSlurper().parseText(text)
        } catch (Exception _) {
            return null
        }
    }

    private static Map<String, String> queryParamsToMap(String query) {
        return query?.split('&')?.collectEntries {
            String[] keyValue = it.split('='); [(keyValue[0]): keyValue[1]]
        } ?: [:]
    }

    private static Map<String, String> headersToMap(Headers headers) {
        return headers.collectEntries {
            [it.key.toLowerCase(), it.value.join(',')]
        } as Map<String, String>
    }

    String getTextWithoutSoap() {
        return XmlUtil.serialize(soap)
    }
}
