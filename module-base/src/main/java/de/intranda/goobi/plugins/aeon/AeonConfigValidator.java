package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;

/**
 * Configuration checks that have to pass before the plugin may create anything.
 *
 * They guard against a configuration that produces no error of its own while doing the wrong thing, or none that
 * names the setting responsible - the hardest kind to notice from the outside.
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

    /**
     * The catalogue the configured opacName names, or null when none of the catalogues offered carries that name.
     *
     * Which catalogues a workflow is offered is decided by three files at once: goobi_opac.xml names them, the opac
     * plugin that serves one may rename it per workflow, and this plugin's opacName picks one of the results. A name
     * matching none of them is therefore an easy mistake to make and impossible to see from any single file, which is
     * why the caller reports the names that do exist alongside the one that does not.
     */
    public static ConfigOpacCatalogue catalogueByName(List<ConfigOpacCatalogue> catalogues, String opacName) {
        // an absent opacName element leaves the name blank, which would otherwise match a catalogue carrying no title
        if (StringUtils.isBlank(opacName)) {
            return null;
        }
        for (ConfigOpacCatalogue catalogue : catalogues) {
            if (opacName.equals(catalogue.getTitle())) {
                return catalogue;
            }
        }
        return null;
    }

    /**
     * The names of the catalogues offered, for the error message that reports a configured name matching none of them.
     */
    public static String catalogueTitles(List<ConfigOpacCatalogue> catalogues) {
        return catalogues.stream().map(ConfigOpacCatalogue::getTitle).collect(Collectors.joining(", "));
    }
}
