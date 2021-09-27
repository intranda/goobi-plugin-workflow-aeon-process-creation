package de.intranda.goobi.plugins.aeon;

import lombok.Data;

/*
 * Just a class to hold JSON information from transmission
 * (will have to be designed specifically for aeon)
 */
@Data
public class AeonUser {
    private String firstName;
    private String lastName;
    private String mailAddress;
}
