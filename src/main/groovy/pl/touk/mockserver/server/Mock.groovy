package pl.touk.mockserver.server

import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope

@PackageScope
@EqualsAndHashCode(excludes = ["counter"])
class Mock {
    final String name
    final String path
    final int port
    Closure predicate = { _ -> true }
    Closure response = { _ -> '' }
    Closure responseHeaders = { _ -> [:] }
    boolean soap = false
    int statusCode = 200
    String method = 'POST'
    int counter = 0

    Mock(String name, String path, int port) {
        if (!(name)) {
            throw new RuntimeException("Mock name must be given")
        }
        this.name = name
        this.path = path
        this.port = port
    }

    boolean match(String method, MockRequest request) {
        return this.method == method && predicate(request)
    }

    MockResponse apply(MockRequest request) {
        println "Mock $name invoked"
        ++counter
        String responseText = response(request)
        String response = soap ? wrapSoap(responseText) : responseText
        Map<String, String> headers = responseHeaders(request)
        return new MockResponse(statusCode, response, headers)
    }

    private static String wrapSoap(String request) {
        """<?xml version='1.0' encoding='UTF-8'?>
            <soap-env:Envelope xmlns:soap-env='http://schemas.xmlsoap.org/soap/envelope/' xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">
                <soap-env:Body>${request}</soap-env:Body>
            </soap-env:Envelope>"""
    }

    void setPredicate(String predicate) {
        if (predicate) {
            this.predicate = Eval.me(predicate) as Closure
        }
    }

    void setResponse(String response) {
        if (response) {
            this.response = Eval.me(response) as Closure
        }
    }

    void setSoap(String soap) {
        if (soap) {
            this.soap = Boolean.valueOf(soap)
        }
    }

    void setStatusCode(String statusCode) {
        if (statusCode) {
            this.statusCode = Integer.valueOf(statusCode)
        }
    }

    void setMethod(String method) {
        if (method) {
            this.method = method
        }
    }

    void setResponseHeaders(String responseHeaders) {
        if (responseHeaders) {
            this.responseHeaders = Eval.me(responseHeaders) as Closure
        }
    }
}
