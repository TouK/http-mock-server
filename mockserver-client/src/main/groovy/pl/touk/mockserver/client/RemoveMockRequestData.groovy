package pl.touk.mockserver.client

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked

@CompileStatic
@TypeChecked
class RemoveMockRequestData {
    String name
    boolean skipReport = false
}
