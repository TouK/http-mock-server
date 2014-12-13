package pl.touk.mockserver.server

import groovy.transform.PackageScope

@PackageScope
class MockResponse {
    final int statusCode
    final String response
    final Map<String, String> headers

    MockResponse(int statusCode, String response, Map<String, String> headers) {
        this.statusCode = statusCode
        this.response = response
        this.headers = headers
    }
}
