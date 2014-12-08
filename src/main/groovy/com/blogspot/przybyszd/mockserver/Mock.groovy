package com.blogspot.przybyszd.mockserver

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(excludes = ["counter"])
class Mock {
    final String name
    final Closure predicate
    final Closure responseOk
    final boolean soap
    //TODO add http method
    int counter = 0

    Mock(String name, Closure predicate, Closure responseOk, boolean soap) {
        this.name = name
        this.predicate = predicate
        this.responseOk = responseOk
        this.soap = soap
    }
}
