package com.blogspot.przybyszd.mockserver

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(excludes = ["counter"])
class Mock {
    final String name
    final Closure predicate
    final Closure responseOk
    //TODO add http method
    //TODO add is soap method
    int counter = 0

    Mock(String name, Closure predicate, Closure responseOk) {
        this.name = name
        this.predicate = predicate
        this.responseOk = responseOk
    }
}
