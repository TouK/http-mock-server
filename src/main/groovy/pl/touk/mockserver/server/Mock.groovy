package pl.touk.mockserver.server

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(excludes = ["counter"])
class Mock {
    final String name
    final String path
    final int port
    final Closure predicate
    final Closure responseOk
    final boolean soap
    //TODO add http method
    //TODO add http code
    //TODO add request headers
    //TODO add response headers
    int counter = 0
    //TODO add historical invocations

    Mock(String name, String path, int port, Closure predicate, Closure responseOk, boolean soap) {
        this.name = name
        this.predicate = predicate
        this.responseOk = responseOk
        this.soap = soap
        this.path = path
        this.port = port
    }
}
