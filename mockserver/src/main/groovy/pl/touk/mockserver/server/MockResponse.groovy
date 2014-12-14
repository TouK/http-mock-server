package pl.touk.mockserver.server

import groovy.transform.PackageScope

@PackageScope
class MockResponse {
    final int statusCode
    final String text
    final Map<String, String> headers

    MockResponse(int statusCode, String text, Map<String, String> headers) {
        this.statusCode = statusCode
        this.text = text
        this.headers = headers
    }
}
