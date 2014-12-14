package pl.touk.mockserver.server

import groovy.transform.PackageScope

@PackageScope
class MockEvent {
    final MockRequest request
    final MockResponse response

    MockEvent(MockRequest request, MockResponse response) {
        this.request = request
        this.response = response
    }
}
