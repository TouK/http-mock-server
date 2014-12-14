package pl.touk.mockserver.client

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TypeChecked

@EqualsAndHashCode
@CompileStatic
@TypeChecked
class MockEvent {
    final MockRequest request
    final MockResponse response

    MockEvent(MockRequest request, MockResponse response) {
        this.request = request
        this.response = response
    }
}
