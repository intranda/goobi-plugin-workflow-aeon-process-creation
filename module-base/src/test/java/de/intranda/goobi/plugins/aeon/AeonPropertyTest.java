package de.intranda.goobi.plugins.aeon;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.junit.Test;

/**
 * A property field carries an optional material type restriction in its 'type' attribute. Display, validation, cloning
 * and export all decide from this one predicate whether the field takes part in the current transaction, so it has to
 * answer the same way for all of them - a field that is hidden but still required makes process creation impossible
 * with nothing on screen to correct.
 *
 * AEON does not always deliver a material type. That case is deliberately not special: a restricted field then applies
 * to nothing, which means it is left out everywhere rather than only in some of the four places.
 */
public class AeonPropertyTest {

    private static final String RESTRICTED_FIELD = "<field type=\"Video (DRMS)\">"
            + "<title>Patron type</title>"
            + "<type readonly=\"false\">radio</type>"
            + "<variable place=\"process\">Patron type</variable>"
            + "<value></value>"
            + "</field>";

    private static final String UNRESTRICTED_FIELD = "<field aeon=\"location\">"
            + "<title>Return location</title>"
            + "<type readonly=\"false\">input</type>"
            + "<variable place=\"process\">Return location</variable>"
            + "<value></value>"
            + "</field>";

    /**
     * Builds a property the same way the plugin does, from a &lt;field&gt; element of the plugin configuration. The
     * plugin itself cannot be instantiated here: its constructor reads the configuration through ConfigPlugins and
     * queries the process templates from the database.
     */
    private AeonProperty propertyFrom(String fieldXml) throws Exception {
        XMLConfiguration config = new XMLConfiguration();
        config.setExpressionEngine(new XPathExpressionEngine());
        config.load(new StringReader("<config_plugin><properties>" + fieldXml + "</properties></config_plugin>"));
        return new AeonProperty(config.configurationsAt("/properties/field").get(0), null);
    }

    // ----- fields without a restriction apply to every transaction -----

    @Test
    public void testUnrestrictedFieldAppliesToAnyMaterialType() throws Exception {
        assertTrue(propertyFrom(UNRESTRICTED_FIELD).appliesTo("Video (DRMS)"));
    }

    @Test
    public void testUnrestrictedFieldAppliesWhenMaterialTypeIsMissing() throws Exception {
        assertTrue(propertyFrom(UNRESTRICTED_FIELD).appliesTo(null));
    }

    // ----- restricted fields apply to their own material type only -----

    @Test
    public void testRestrictedFieldAppliesToItsOwnMaterialType() throws Exception {
        assertTrue(propertyFrom(RESTRICTED_FIELD).appliesTo("Video (DRMS)"));
    }

    @Test
    public void testRestrictedFieldDoesNotApplyToAnotherMaterialType() throws Exception {
        assertFalse(propertyFrom(RESTRICTED_FIELD).appliesTo("Film (DRMS)"));
    }

    /**
     * The reason this predicate exists in one place: AEON stopped delivering the material type, and the four callers
     * used to disagree about what that meant. The answer has to be a plain no, not a special case.
     */
    @Test
    public void testRestrictedFieldDoesNotApplyWhenMaterialTypeIsMissing() throws Exception {
        assertFalse(propertyFrom(RESTRICTED_FIELD).appliesTo(null));
    }

    @Test
    public void testRestrictedFieldDoesNotApplyWhenMaterialTypeIsEmpty() throws Exception {
        assertFalse(propertyFrom(RESTRICTED_FIELD).appliesTo(""));
    }
}