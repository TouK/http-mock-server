package pl.touk.mockserver.client

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TypeChecked

@CompileStatic
@TypeChecked
@EqualsAndHashCode
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
