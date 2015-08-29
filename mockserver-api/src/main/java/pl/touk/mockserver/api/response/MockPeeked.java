package pl.touk.mockserver.api.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@Data
@EqualsAndHashCode(callSuper = false)
public class MockPeeked extends MockServerResponse {
    @XmlElement(name = "mockEvent")
    private List<MockEventReport> mockEvents;
}
