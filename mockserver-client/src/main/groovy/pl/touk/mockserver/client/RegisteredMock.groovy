package pl.touk.mockserver.client

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TypeChecked

@CompileStatic
@TypeChecked
@EqualsAndHashCode
@ToString
class RegisteredMock {
    final String name
    final String path
    final int port
    final String predicate
    final String response
    final String responseHeaders

    RegisteredMock(String name, String path, int port, String predicate, String response, String responseHeaders) {
        this.name = name
        this.path = path
        this.port = port
        this.predicate = predicate
        this.response = response
        this.responseHeaders = responseHeaders
    }
}
