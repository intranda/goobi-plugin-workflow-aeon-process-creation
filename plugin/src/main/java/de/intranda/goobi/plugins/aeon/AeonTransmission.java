package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/*
 * The main Object from the Transmission
 * (properties still have to be specifically designed for aeon JSON response)
 */
@Data
public class AeonTransmission {
    private String id;
    private String creationDate;
    private String title;
    private AeonUser user;
    List<AeonItem> items = new ArrayList<>();

    //sets all items to accepted / declined (for buttons under list)
    public void setAllItemsAccepted(boolean status) {
        for (AeonItem item : this.items) {
            item.setAccepted(status);
        }
    }
}
