package pl.touk.mockserver.api.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Data
@EqualsAndHashCode(callSuper = false)
public class AddMock extends MockServerRequest {
    @XmlElement(required = true)
    private String name;

    @XmlElement(required = true)
    private String path;

    @XmlElement(required = true)
    private int port;

    private String predicate;

    private String response;

    private Boolean soap;

    private Integer statusCode;

    private Method method;

    private String responseHeaders;
}
