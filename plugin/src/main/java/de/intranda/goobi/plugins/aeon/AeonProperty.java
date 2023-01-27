package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.vocabulary.Field;
import org.goobi.vocabulary.VocabRecord;
import org.goobi.vocabulary.Vocabulary;

import de.intranda.goobi.plugins.AeonProcessCreationWorkflowPlugin;
import de.sub.goobi.persistence.managers.VocabularyManager;
import lombok.Data;

@Data
public class AeonProperty {

    // configuration
    private String aeonField; // @aeon
    private String title; // /title
    private String type; // /type
    private boolean readonly; // /type/@readonly

    private String propertyName; // /variable
    private String place; // /variable/@place

    private String defaultValue;
    private String value; // /value
    private String additionalValue = ""; // second field for select+text fields
    private List<String> selectValues; // /select

    private Map<String, Boolean> displayMap; // store whether additional fields should be displayed for a selected value or not

    private String helpMessage; // /help

    private String validationExpression; // /validation
    private boolean strictValidation; // /validation/@strict
    private String validationErrorMessage; // /message

    private boolean displayAlways = false;

    private String vocabularyName; // /vocabularyName
    private List<String> vocabularyField; // /vocabularyField

    private boolean displayInTitle = false;

    private String shippingOption;

    private HierarchicalConfiguration config;

    private boolean overwriteMainField = false;

    private AeonProcessCreationWorkflowPlugin plugin;

    /** contains the list of selected values in multiselect */
    private List<String> multiselectSelectedValues = new ArrayList<>();

    public List<String> getPossibleValues() {
        List<String> answer = new ArrayList<>();
        for (String possibleValue : selectValues) {
            if (!multiselectSelectedValues.contains(possibleValue)) {
                answer.add(possibleValue);
            }
        }
        return answer;
    }

    public String getMultiselectValue() {
        return "";
    }

    public void setMultiselectValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            multiselectSelectedValues.add(value);
        }
    }

    public void removeSelectedValue(String value) {
        multiselectSelectedValues.remove(value);
    }

    public AeonProperty(HierarchicalConfiguration config, AeonProcessCreationWorkflowPlugin plugin) {
        this.config = config;
        aeonField = config.getString("@aeon");
        title = config.getString("title");
        type = config.getString("type", "input");
        readonly = config.getBoolean("type/@readonly", false);

        displayAlways = config.getBoolean("@displayAlways", false);
        displayInTitle = config.getBoolean("@displayInTitle", false);
        propertyName = config.getString("variable", title);
        place = config.getString("variable/@place", "process");

        value = config.getString("value", "");
        displayMap = new HashMap<>();
        selectValues = new ArrayList<>();
        List<HierarchicalConfiguration> selections = config.configurationsAt("select");
        for (HierarchicalConfiguration hc : selections) {
            boolean showAdditionalField = hc.getBoolean("@text", false);
            String data = hc.getString(".");
            displayMap.put(data, showAdditionalField);
            selectValues.add(data);
        }

        helpMessage = config.getString("help", "");

        validationExpression = config.getString("validation", "");
        strictValidation = config.getBoolean("validation/@strict", false);
        validationErrorMessage = config.getString("message", "");
        if (type.equals("vocabulary")) {
            vocabularyName = config.getString("vocabularyName", "");
            vocabularyField = Arrays.asList(config.getStringArray("vocabularyField"));
            initializeVocabulary();
        }

        shippingOption = config.getString("@type", null);
        this.plugin = plugin;

    }

    private void initializeVocabulary() {

        if (vocabularyField == null || vocabularyField.isEmpty()) {
            Vocabulary currentVocabulary = VocabularyManager.getVocabularyByTitle(vocabularyName);

            if (currentVocabulary != null) {
                VocabularyManager.getAllRecords(currentVocabulary);
                List<VocabRecord> recordList = currentVocabulary.getRecords();
                Collections.sort(recordList);
                selectValues = new ArrayList<>(recordList.size());
                if (currentVocabulary != null && currentVocabulary.getId() != null) {
                    for (VocabRecord vr : recordList) {
                        for (Field f : vr.getFields()) {
                            if (f.getDefinition().isMainEntry()) {
                                selectValues.add(f.getValue());
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            List<StringPair> vocabularySearchFields = new ArrayList<>();
            for (String fieldname : vocabularyField) {
                String[] parts = fieldname.trim().split("=");
                if (parts.length > 1) {
                    String fieldName = parts[0];
                    String value = parts[1];
                    StringPair sp = new StringPair(fieldName, value);
                    vocabularySearchFields.add(sp);
                }
            }
            List<VocabRecord> records = VocabularyManager.findRecords(vocabularyName, vocabularySearchFields);
            if (records != null && records.size() > 0) {
                Collections.sort(records);
                selectValues = new ArrayList<>(records.size());
                for (VocabRecord vr : records) {
                    for (Field f : vr.getFields()) {
                        if (f.getDefinition().isMainEntry()) {
                            selectValues.add(f.getValue());
                            break;
                        }
                    }
                }
            }
        }
    }

    public AeonProperty cloneProperty() {
        AeonProperty property = new AeonProperty(config, plugin);
        return property;
    }

    public boolean isValid() {

        if (StringUtils.isNotBlank(validationExpression)) {
            if (!displayMap.isEmpty() && StringUtils.isNotBlank(value) && displayMap.get(value)) {
                if (!additionalValue.matches(validationExpression)) {
                    return false;
                }
            } else {
                if (StringUtils.isBlank(value)) {
                    return false;
                } else if (!value.matches(validationExpression)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isDifferFromDefault() {
        if (StringUtils.isBlank(defaultValue) && StringUtils.isNotBlank(value)) {
            return true;
        }
        if (StringUtils.isBlank(value) && StringUtils.isNotBlank(defaultValue)) {
            return true;
        }
        if (StringUtils.isBlank(defaultValue) && StringUtils.isBlank(value)) {
            return false;
        }

        return !defaultValue.equals(value);
    }

    public void setValue(String value) {
        if (!overwriteMainField) {
            plugin.updateProperties(title, this.value, value);
        }
        this.value = value;
    }

    public String getExportValue() {
        if (StringUtils.isNotBlank(additionalValue)) {
            return additionalValue;
        }
        return value;
    }
}
