package pl.touk.mockserver.api.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "mocks")
@Data
@EqualsAndHashCode(callSuper = false)
public class MockListing extends MockServerResponse {
    @XmlElement(name = "mock")
    private List<MockReport> mocks;
}
