package pl.touk.mockserver.server

import com.sun.net.httpserver.HttpExchange

class Util {
    static void createResponse(HttpExchange ex, String response, int statusCode) {
        ex.sendResponseHeaders(statusCode, response ? response.length() : -1)
        if (response) {
            ex.responseBody << response
            ex.responseBody.close()
        }
    }
}
