package pl.touk.mockserver.server

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(excludes = ["counter"])
class Mock {
    final String name
    final String path
    final int port
    Closure predicate = { _ -> true }
    Closure responseOk = { _ -> '' }
    boolean soap = false
    int statusCode = 200
    String method = 'POST'
    Closure responseHeaders = {_ -> [:]}
    int counter = 0

    Mock(String name, String path, int port) {
        this.name = name
        this.path = path
        this.port = port
    }
}
