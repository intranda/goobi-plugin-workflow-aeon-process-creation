package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data

public class AeonRecord {

    private List<AeonProperty> properties = new ArrayList<>();

}
