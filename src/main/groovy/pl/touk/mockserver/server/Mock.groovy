package pl.touk.mockserver.server

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(excludes = ["counter"])
class Mock {
    final String name
    final String path
    final int port
    Closure predicate = { xml -> true }
    Closure responseOk = { xml -> '' }
    boolean soap = false
    int statusCode = 200
    String method = 'POST'
    Closure requestHeaders = {hs -> true}
    Closure responseHeaders = {xml -> [:]}
    Closure queryParams = {qs -> true}
    int counter = 0

    Mock(String name, String path, int port) {
        this.name = name
        this.path = path
        this.port = port
    }
}
