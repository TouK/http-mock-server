package pl.touk.mockserver.api.request;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum Method {
    POST,
    GET,
    DELETE,
    PUT,
    TRACE,
    HEAD,
    OPTIONS,
    PATCH;
}
