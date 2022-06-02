package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.goobi.beans.Process;

import lombok.Data;

@Data
public class AeonRecord {

    private List<AeonProperty> properties = new ArrayList<>();

    private long id = System.currentTimeMillis();

    private boolean accepted = false;

    private boolean showDetails = false;

    private Map<String, String> recordData;

    private String processTitle;

    private String duplicateTitle;

    private boolean duplicate;

    private boolean disabled;

    private boolean deletable = false;

    private List<AeonProperty> processProperties = new ArrayList<>();

    private Process existingProcess;

    private List<AeonExistingProcess> existingProcesses = new ArrayList<>();


}

