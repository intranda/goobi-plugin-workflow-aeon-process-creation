package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;

import lombok.Data;

@Data
public class AeonRecord implements Comparable<AeonRecord> {

    private static final String ORDER_PROPERTY = "containerGrouping";

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

    @Override
    public int compareTo(AeonRecord other) {

        // comparing cancel records
        if (recordData == null && other.getRecordData() == null) {
            return processTitle.compareTo(other.getProcessTitle());
        }

        String orderField = "";
        String otherOrderField = "";
        if (recordData != null) {
            orderField = recordData.get(ORDER_PROPERTY);
        }
        if (other.getRecordData() != null) {
            otherOrderField = other.getRecordData().get(ORDER_PROPERTY);
        }

        // if both fields are empty, the order is the same
        if (StringUtils.isBlank(orderField) && StringUtils.isBlank(otherOrderField)) {
            return 0;
        }
        // if one field is empty, the other record has higher order
        if (StringUtils.isNotBlank(orderField) && StringUtils.isBlank(otherOrderField)) {
            return -1;
        }
        if (StringUtils.isBlank(orderField) && StringUtils.isNotBlank(otherOrderField)) {
            return 1;
        }

        // compare values
        return orderField.compareTo(otherOrderField);
    }

}
