package pl.touk.mockserver.api.response;

import lombok.Data;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@Data
public class Parameter {
    @XmlAttribute(required = true)
    private String name;

    @XmlValue
    private String value;
}
