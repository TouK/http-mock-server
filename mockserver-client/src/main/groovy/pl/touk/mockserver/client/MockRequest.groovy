package pl.touk.mockserver.client

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TypeChecked

@CompileStatic
@TypeChecked
@EqualsAndHashCode
class MockRequest {
    final String text
    final Map<String, String> headers
    final Map<String, String> query
    final List<String> path

    MockRequest(String text, Map<String, String> headers, Map<String, String> query, List<String> path) {
        this.text = text
        this.headers = headers
        this.query = query
        this.path = path
    }
}
