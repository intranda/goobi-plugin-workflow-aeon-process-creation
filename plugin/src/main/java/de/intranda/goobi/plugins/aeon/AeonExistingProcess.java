package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class AeonExistingProcess {
    private String title = "";
    private String date = "";
    private List<AeonProperty> duplicateProperties = new ArrayList<>();
}
