package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration checks that have to pass before the plugin may create anything.
 *
 * Both of them guard against a configuration that produces no error at all while doing the wrong thing, which is the
 * hardest kind to notice from the outside.
 */
public class AeonConfigValidator {

    private AeonConfigValidator() {
        // hide implicit public constructor
    }

    /**
     * Titles of the fields whose place attribute names neither store. Such a field would fall through to writing a
     * process property, so its value would be stored somewhere nothing ever reads it back from.
     */
    public static List<String> invalidPlaces(List<AeonProperty> fields) {
        List<String> broken = new ArrayList<>();
        for (AeonProperty field : fields) {
            if (!field.isPlaceValid()) {
                broken.add(field.getTitle());
            }
        }
        return broken;
    }

    /**
     * The transaction field that carries the transaction number, or null when the configuration declares none.
     *
     * The duplicate check and the cancellation search look processes up by the name configured in
     * processCancellation/transactionFieldName. That name has to be the variable of this field, otherwise both
     * lookups search for something no process was ever written with and quietly return nothing.
     */
    public static AeonProperty transactionNumberField(List<AeonProperty> transactionFields, String transactionNumberAeonField) {
        for (AeonProperty field : transactionFields) {
            if (transactionNumberAeonField.equals(field.getAeonField())) {
                return field;
            }
        }
        return null;
    }
}
