package de.intranda.goobi.plugins.api.aeon;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@XmlRootElement
@AllArgsConstructor
public class User {
    private String username;
    private String password;

}
