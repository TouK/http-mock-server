package pl.touk.mockserver.client

import org.apache.commons.lang3.StringEscapeUtils

class AddMockRequestData {
    String name
    String path
    Integer port
    String predicate
    String response
    Boolean soap

    void setPredicate(String predicate){
        this.predicate = StringEscapeUtils.escapeXml11(predicate)
    }

    void setResponse(String response){
        this.response = StringEscapeUtils.escapeXml11(response)
    }


}
