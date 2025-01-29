package org.goobi.aeon;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@XmlRootElement
@Data
public class LoginResponse {

    private String accessToken;
    private String expiresIn;

}
