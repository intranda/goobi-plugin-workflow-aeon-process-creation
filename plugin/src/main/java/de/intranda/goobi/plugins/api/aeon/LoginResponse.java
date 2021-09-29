package de.intranda.goobi.plugins.api.aeon;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@XmlRootElement
@Data
public class LoginResponse {

    private String accessToken;
    private String expiresIn;

}
