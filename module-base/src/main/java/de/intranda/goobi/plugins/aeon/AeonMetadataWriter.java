package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;

import ugh.dl.DocStruct;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;

/**
 * Turns configured aeon fields into UGH metadata.
 *
 * This is the only place that touches UGH on behalf of the plugin's configured fields, and it deliberately knows
 * nothing about Helper, ConfigPlugins or FacesContext, so that it can be tested against a real ruleset instead of
 * being checked by building the plugin and looking at the result.
 */
public class AeonMetadataWriter {

    private AeonMetadataWriter() {
        // hide implicit public constructor
    }

    /**
     * Metadata type names among the given fields that the ruleset does not define, each named once.
     *
     * Process creation calls this before it creates anything: a typo or a missing ruleset entry is a configuration
     * defect, and catching it here means no process is written with parts of its data silently dropped.
     */
    public static List<String> unknownTypes(Prefs prefs, List<AeonProperty> fields) {
        List<String> unknown = new ArrayList<>();
        for (AeonProperty field : fields) {
            String typeName = field.getVariableName();
            if (field.isMetadata() && prefs.getMetadataTypeByName(typeName) == null && !unknown.contains(typeName)) {
                unknown.add(typeName);
            }
        }
        return unknown;
    }

    /**
     * Writes one value under one metadata type.
     *
     * Both failures are configuration defects rather than data problems, so both are reported to the caller instead of
     * being logged and skipped: an unknown type means the ruleset is missing an entry, a rejected one means the entry
     * exists but is not permitted where the field was configured to go.
     */
    public static void add(DocStruct docStruct, Prefs prefs, String typeName, String value) throws AeonMetadataException {
        MetadataType type = prefs.getMetadataTypeByName(typeName);
        if (type == null) {
            throw new AeonMetadataException(typeName, "the ruleset defines no such metadata type");
        }
        try {
            Metadata metadata = new Metadata(type);
            metadata.setValue(value);
            docStruct.addMetadata(metadata);
        } catch (MetadataTypeNotAllowedException e) {
            throw new AeonMetadataException(typeName, "not allowed on docstruct " + docStruct.getType().getName());
        }
    }
}
