package pl.touk.mockserver.api.response;

import lombok.Data;

import javax.xml.bind.annotation.XmlElement;

@Data
public class MockEventReport {
    @XmlElement(required = true)
    private MockRequestReport request;

    @XmlElement(required = true)
    private MockResponseReport response;
}
