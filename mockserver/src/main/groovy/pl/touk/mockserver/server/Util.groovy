package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange
import pl.touk.mockserver.api.response.MockAdded

import javax.xml.bind.JAXBContext

class Util {

    private static
    final JAXBContext responseJaxbContext = JAXBContext.newInstance(MockAdded.package.name, MockAdded.classLoader)

    static void createResponse(HttpExchange ex, Object response, int statusCode) {
        String responseString = marshall(response)
        createResponse(ex, responseString, statusCode)
    }

    static void createResponse(HttpExchange ex, String responseString, int statusCode) {
        byte[] responseBytes = responseString ? responseString.getBytes('UTF-8') : new byte[0]
        ex.sendResponseHeaders(statusCode, responseBytes.length ?: -1)
        if (responseString) {
            ex.responseBody << responseBytes
            ex.responseBody.close()
        }
    }

    private static String marshall(Object response) {
        StringWriter sw = new StringWriter()
        responseJaxbContext.createMarshaller().marshal(response, sw)
        return sw.toString()
    }
}
