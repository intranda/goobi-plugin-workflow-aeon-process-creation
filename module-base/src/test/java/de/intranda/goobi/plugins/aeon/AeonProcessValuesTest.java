package de.intranda.goobi.plugins.aeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.beans.GoobiProperty;
import org.goobi.beans.GoobiProperty.PropertyOwnerType;
import org.goobi.production.cli.helper.StringPair;
import org.junit.Test;

/**
 * Once a field can live in either store, every read has to ask the field where to look. Reading the wrong store
 * returns nothing rather than failing, which is why this is worth pinning down: the duplicate check treats 'nothing'
 * as 'not a duplicate' and creates the process it exists to prevent.
 */
public class AeonProcessValuesTest {

    private AeonProperty field(String place, String variable) throws Exception {
        XMLConfiguration config = new XMLConfiguration();
        config.setExpressionEngine(new XPathExpressionEngine());
        config.load(new StringReader("<config_plugin><properties><field><title>" + variable + "</title>"
                + "<variable place=\"" + place + "\">" + variable + "</variable>"
                + "<value></value></field></properties></config_plugin>"));
        return new AeonProperty(config.configurationsAt("/properties/field").get(0), null);
    }

    private GoobiProperty property(String name, String value) {
        GoobiProperty property = new GoobiProperty(PropertyOwnerType.PROCESS);
        property.setPropertyName(name);
        property.setPropertyValue(value);
        return property;
    }

    @Test
    public void testMetadataFieldIsReadFromTheMetadata() throws Exception {
        AeonProcessValues values = new AeonProcessValues(
                List.of(new StringPair("TitleDocMain", "A title")),
                List.of(property("TitleDocMain", "wrong store")));
        assertEquals("A title", values.first(field("metadata", "TitleDocMain")));
    }

    @Test
    public void testPropertyFieldIsReadFromTheProperties() throws Exception {
        AeonProcessValues values = new AeonProcessValues(
                List.of(new StringPair("Patron type", "wrong store")),
                List.of(property("Patron type", "Yale")));
        assertEquals("Yale", values.first(field("process", "Patron type")));
    }

    @Test
    public void testFirstIsEmptyWhenTheProcessCarriesNothing() throws Exception {
        AeonProcessValues values = new AeonProcessValues(List.of(), List.of());
        assertEquals("", values.first(field("metadata", "TitleDocMain")));
    }

    /**
     * A multiselect field leaves one metadatum per selected value, so a reader that only ever took the first would
     * silently drop the rest.
     */
    @Test
    public void testAllReturnsEveryValueInOrder() throws Exception {
        AeonProcessValues values = new AeonProcessValues(
                List.of(new StringPair("TypeOfRequest", "Classroom use"), new StringPair("TypeOfRequest", "Throwaway")),
                List.of());
        assertEquals(List.of("Classroom use", "Throwaway"), values.all(field("metadata", "TypeOfRequest")));
    }

    @Test
    public void testNullStoresAreTreatedAsEmpty() throws Exception {
        AeonProcessValues values = new AeonProcessValues(null, null);
        assertTrue(values.all(field("metadata", "TitleDocMain")).isEmpty());
        assertTrue(values.all(field("process", "Patron type")).isEmpty());
    }
}
