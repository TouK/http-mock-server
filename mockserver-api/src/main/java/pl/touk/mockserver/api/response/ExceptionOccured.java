package pl.touk.mockserver.api.response;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement
@Data
public class ExceptionOccured {
    @XmlValue
    private String message;
}
