package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.goobi.beans.GoobiProperty;
import org.goobi.production.cli.helper.StringPair;

/**
 * The values one existing process carries, answered per field from the store that field is configured for.
 *
 * Both stores are handed in already loaded rather than fetched here, so that this class stays free of the database and
 * so that a caller reading several fields of the same process pays for one lookup instead of one per field.
 */
public class AeonProcessValues {

    private final List<StringPair> metadata;
    private final List<GoobiProperty> properties;

    public AeonProcessValues(List<StringPair> metadata, List<GoobiProperty> properties) {
        this.metadata = metadata == null ? List.of() : metadata;
        this.properties = properties == null ? List.of() : properties;
    }

    /** first value of the given field, or an empty string when the process carries none. Never null. */
    public String first(AeonProperty field) {
        List<String> values = all(field);
        return values.isEmpty() ? "" : values.get(0);
    }

    /**
     * Every value of the given field, in the order the process carries them. A multiselect field leaves one entry per
     * selected value, so more than one is normal rather than a sign of duplicated data.
     *
     * A stored value may be null, which is coalesced to an empty string here rather than at each call site: a null
     * would otherwise make a caller's equals comparison throw, make a contains check report a match between two
     * absent values, and end up written into a displayed field.
     */
    public List<String> all(AeonProperty field) {
        List<String> values = new ArrayList<>();
        if (field.isMetadata()) {
            for (StringPair pair : metadata) {
                if (field.getVariableName().equals(pair.getOne())) {
                    values.add(Objects.toString(pair.getTwo(), ""));
                }
            }
        } else {
            for (GoobiProperty property : properties) {
                if (field.getVariableName().equals(property.getPropertyName())) {
                    values.add(Objects.toString(property.getPropertyValue(), ""));
                }
            }
        }
        return values;
    }
}
