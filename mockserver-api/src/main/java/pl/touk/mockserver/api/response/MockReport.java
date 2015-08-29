package pl.touk.mockserver.api.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlElement;

@Data
public class MockReport {
    @XmlElement(required = true)
    private String name;

    @XmlElement(required = true)
    private String path;

    @XmlElement(required = true)
    private int port;

    @XmlElement(required = true)
    private String predicate;

    @XmlElement(required = true)
    private String response;

    @XmlElement(required = true)
    private String responseHeaders;
}
