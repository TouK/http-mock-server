package pl.touk.mockserver.api.response;

import lombok.Data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@Data
public class MockResponseReport {
    @XmlElement(required = true)
    private int statusCode;

    private String text;

    @XmlElementWrapper(name = "headers")
    @XmlElement(name = "param")
    private List<Parameter> headers;
}
