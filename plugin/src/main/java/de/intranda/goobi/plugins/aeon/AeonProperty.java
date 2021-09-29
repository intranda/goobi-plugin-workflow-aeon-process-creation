package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.vocabulary.Field;
import org.goobi.vocabulary.VocabRecord;
import org.goobi.vocabulary.Vocabulary;

import de.sub.goobi.persistence.managers.VocabularyManager;
import lombok.Data;

@Data
public class AeonProperty {

    private String title; // /title
    private String type; // /type
    private boolean readonly; // /type/@readonly

    private String propertyName; // /variable
    private String place; // /variable/@place

    private String value; // /value
    private List<String> selectValues; // /select

    private String helpMessage; // /help

    private String validationExpression; // /validation
    private boolean strictValidation; // /validation/@strict
    private String validationErrorMessage; // /message

    private String vocabularyName; // /vocabularyName
    private List<String> vocabularyField; // /vocabularyField

    public AeonProperty(HierarchicalConfiguration config) {
        title = config.getString("title");
        type = config.getString("type", "input");
        readonly = config.getBoolean("type/@readonly", false);

        propertyName = config.getString("variable", title);
        place = config.getString("variable/@place", "process");

        value = config.getString("value", "");

        String[] data = config.getStringArray("select");
        if (data != null) {
            selectValues = Arrays.asList(data);
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

    public void validateProperty(FacesContext context, UIComponent component, Object value) {
        // check if validation expression was configured
        System.out.println("validator");
        if (StringUtils.isNotBlank(validationExpression)) {
            // check if value is empty
            if (value == null) {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "validation error", validationErrorMessage);
                throw new ValidatorException(message);
            }
            String data = (String) value;
            // check if data matches expression
            if (!data.matches(validationExpression)) {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "validation error", validationErrorMessage);
                throw new ValidatorException(message);
            }
        }
    }
}
