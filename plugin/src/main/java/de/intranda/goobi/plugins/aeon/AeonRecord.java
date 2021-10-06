package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class AeonRecord {

    private List<AeonProperty> properties = new ArrayList<>();

    //
    private boolean accepted = false;

    private boolean showDetails = false;

    private Map<String, String> recordData;
}