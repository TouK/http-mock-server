package pl.touk.mockserver.client

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked

@CompileStatic
@TypeChecked
class InvalidMockDefinitionException extends RuntimeException {
    InvalidMockDefinitionException(String s) {
        super(s)
    }
}
