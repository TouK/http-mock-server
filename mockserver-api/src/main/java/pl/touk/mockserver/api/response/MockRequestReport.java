package pl.touk.mockserver.api.response;

import lombok.Data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@Data
public class MockRequestReport {
    private String text;

    @XmlElementWrapper(name = "headers")
    @XmlElement(name = "param")
    private List<Parameter> headers;

    @XmlElementWrapper(name = "query")
    @XmlElement(name = "param")
    private List<Parameter> queryParams;

    @XmlElementWrapper(name = "path")
    @XmlElement(name = "elem")
    private List<String> paths;
}
