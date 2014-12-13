package pl.touk.mockserver.client

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TypeChecked

@CompileStatic
@TypeChecked
@EqualsAndHashCode
class RegisteredMock {
    final String name
    final String path
    final int port

    RegisteredMock(String name, String path, int port) {
        this.name = name
        this.path = path
        this.port = port
    }
}
