package de.intranda.goobi.plugins.aeon;

import de.intranda.goobi.plugins.AeonProcessCreationWorkflowPlugin;
import de.sub.goobi.helper.Helper;
import io.goobi.vocabulary.exchange.FieldDefinition;
import io.goobi.vocabulary.exchange.Vocabulary;
import io.goobi.vocabulary.exchange.VocabularySchema;
import io.goobi.workflow.api.vocabulary.VocabularyAPIManager;
import io.goobi.workflow.api.vocabulary.jsfwrapper.JSFVocabularyRecord;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private VocabularyAPIManager vocabularyAPI = VocabularyAPIManager.getInstance();

    /** contains the list of selected values in multiselect */
    private List<String> multiselectSelectedValues = new ArrayList<>();
    private List<String> defaultSelectedValues = new ArrayList<>();

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
            if (!overwriteMainField) {
                plugin.updateMultiselectProperties(title, multiselectSelectedValues);
            }
        }
    }

    public void removeSelectedValue(String value) {
        multiselectSelectedValues.remove(value);
        if (!overwriteMainField) {
            plugin.updateMultiselectProperties(title, multiselectSelectedValues);
        }
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
        if ("vocabulary".equals(type)) {
            vocabularyName = config.getString("vocabularyName", "");
            vocabularyField = Arrays.asList(config.getStringArray("vocabularyField"));
            initializeVocabulary();
        }

        shippingOption = config.getString("@type", null);
        this.plugin = plugin;

    }

    private void initializeVocabulary() {
        Vocabulary vocabulary = vocabularyAPI.vocabularies().findByName(vocabularyName);
        if (vocabularyField == null || vocabularyField.isEmpty()) {
            List<JSFVocabularyRecord> recordList = vocabularyAPI.vocabularyRecords().list(vocabulary.getId(), Optional.of(1000), Optional.empty()).getContent();
            selectValues = recordList.stream()
                    .map(JSFVocabularyRecord::getMainValue)
                    .collect(Collectors.toList());
        } else {
            if (vocabularyField.size() > 1) {
                Helper.setFehlerMeldung("vocabularyList with multiple fields is not supported right now");
                return;
            }

            String[] parts = vocabularyField.get(0).trim().split("=");
            if (parts.length != 2) {
                Helper.setFehlerMeldung("Wrong field format");
                return;
            }

            String searchFieldName = parts[0];
            String searchFieldValue = parts[1];

            VocabularySchema schema = vocabularyAPI.vocabularySchemas().get(vocabulary.getSchemaId());
            Optional<FieldDefinition> searchField = schema.getDefinitions().stream()
                    .filter(d -> d.getName().equals(searchFieldName))
                    .findFirst();

            if (searchField.isEmpty()) {
                Helper.setFehlerMeldung("Field " + searchFieldName + " not found in vocabulary " + vocabulary.getName());
                return;
            }

            // Assume there are not than 1000 hits, otherwise it is not useful anyway..
            List<JSFVocabularyRecord> recordList = vocabularyAPI.vocabularyRecords()
                    .search(vocabulary.getId(), searchField.get().getId() + ":" + searchFieldValue)
                    .getContent();
            selectValues = recordList.stream()
                    .map(JSFVocabularyRecord::getMainValue)
                    .collect(Collectors.toList());
        }
    }

    public AeonProperty cloneProperty() {
        return new AeonProperty(config, plugin);
    }

    public boolean isValid() {

        if (StringUtils.isNotBlank(validationExpression)) {
            if (!displayMap.isEmpty() && StringUtils.isNotBlank(value) && displayMap.get(value).booleanValue()) {
                if (!additionalValue.matches(validationExpression)) {
                    return false;
                }
            } else if (StringUtils.isBlank(value)) {
                return false;
            } else if (!value.matches(validationExpression)) {
                return false;
            }
        }
        return true;
    }

    public boolean isDifferFromDefault() {
        // compare multi
        if ("multiselect".equals(type)) {
            // both empty
            if (multiselectSelectedValues.isEmpty() && defaultSelectedValues.isEmpty()) {
                return true;
            }
            // check if both contain the same content
            return !CollectionUtils.isEqualCollection(multiselectSelectedValues, defaultSelectedValues);
        }

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
            plugin.updateProperties(title, value, additionalValue);
        }
        this.value = value;
    }

    public void setAdditionalValue(String additionalValue) {
        if (!overwriteMainField) {
            plugin.updateProperties(title, value, additionalValue);
        }
        this.additionalValue = additionalValue;
    }

    public String getExportValue() {
        if (StringUtils.isNotBlank(additionalValue)) {
            return additionalValue;
        }
        return value;
    }
}
