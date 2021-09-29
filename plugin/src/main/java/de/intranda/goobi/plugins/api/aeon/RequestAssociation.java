package de.intranda.goobi.plugins.api.aeon;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement
public class RequestAssociation {

    private String type;
    private String reference;
}
