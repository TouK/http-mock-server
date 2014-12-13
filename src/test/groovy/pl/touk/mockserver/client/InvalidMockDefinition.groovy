package pl.touk.mockserver.client

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked

@CompileStatic
@TypeChecked
class InvalidMockDefinition extends RuntimeException {
    InvalidMockDefinition(String s) {
        super(s)
    }
}
