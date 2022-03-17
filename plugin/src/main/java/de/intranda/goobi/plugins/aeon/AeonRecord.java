package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;

import lombok.Data;

@Data
public class AeonRecord {

    private List<AeonProperty> properties = new ArrayList<>();

    //
    private boolean accepted = false;

    private boolean showDetails = false;

    private Map<String, String> recordData;

    private String processTitle;

    private boolean duplicate;

    private boolean disabled;

    private List<AeonProperty> processProperties = new ArrayList<>();

    private List<AeonProperty> duplicateProperties = new ArrayList<>();

    private Process existingProcess;

    public String getDuplicationPopup() {
        StringBuilder answer = new StringBuilder();

        answer.append("<ul style=\"list-style-type:none;\">");
        for (AeonProperty property : duplicateProperties) {
            if (StringUtils.isNotBlank(property.getValue())) {
                answer.append("<li class=\"key-value-pair\">");
                answer.append("<b>");
                answer.append(property.getTitle());
                answer.append("</b>: ");
                answer.append(property.getValue());
                answer.append("</li>");
            }
        }
        answer.append("</ul>");

        return answer.toString();
    }

}