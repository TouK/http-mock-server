package pl.touk.mockserver.api.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Data
@EqualsAndHashCode(callSuper = false)
public class MockAdded extends MockServerResponse {
}
