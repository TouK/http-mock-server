package pl.touk.mockserver.server

import com.sun.net.httpserver.Headers
import groovy.json.JsonSlurper
import groovy.util.slurpersupport.GPathResult

class Request {
    final String text
    final Map<String,String> headers
    final Map<String,String> query
    final GPathResult xml
    final GPathResult soap
    final Object json

    Request(String text, Headers headers, String query) {
        this.text = text
        this.headers = headersToMap(headers)
        this.query = queryParamsToMap(query)
        this.xml = inputToXml(text)
        this.soap = inputToSoap(xml)
        this.json= inputToJson(text)
    }

    private static GPathResult inputToXml(String text) {
        try{
            return new XmlSlurper().parseText(text)
        }catch (Exception _){
            return null
        }
    }

    private static GPathResult inputToSoap(GPathResult xml) {
        try{
            if (xml.name() == 'Envelope' && xml.Body.size() > 0) {
                return getSoapBodyContent(xml)
            } else {
                return null
            }
        }catch (Exception _){
            return null
        }
    }

    private static GPathResult getSoapBodyContent(GPathResult xml) {
        return xml.Body.'**'[1]
    }

    private static Object inputToJson(String text) {
        try{
            return new JsonSlurper().parseText(text)
        }catch (Exception _){
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
        }
    }

}
