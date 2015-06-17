package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange

class Util {
    static void createResponse(HttpExchange ex, String response, int statusCode) {
        byte[] responseBytes = response ? response.getBytes('UTF-8') : new byte[0]
        ex.sendResponseHeaders(statusCode, responseBytes.length ?: -1)
        if (response) {
            ex.responseBody << responseBytes
            ex.responseBody.close()
        }
    }
}
