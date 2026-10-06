package de.intranda.goobi.plugins.aeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.junit.Before;
import org.junit.Test;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Metadata;
import ugh.dl.Prefs;

/**
 * The writer is the one place that turns configured aeon fields into UGH metadata. It is deliberately free of Helper,
 * ConfigPlugins and FacesContext so that it can be exercised against a real ruleset here, rather than by building the
 * plugin and looking at the result - which is how the surrounding code has to be checked.
 */
public class AeonMetadataWriterTest {

    private static final String RULESET = "src/test/resources/rulesets/aeon_test_ruleset.xml";

    private Prefs prefs;

    @Before
    public void setUp() throws Exception {
        String ruleset = RULESET;
        if (!Paths.get(ruleset).toFile().exists()) {
            // when the build runs from the repository root rather than from the module directory
            ruleset = Paths.get("target/test-classes/rulesets/aeon_test_ruleset.xml").toString();
        }
        prefs = new Prefs();
        prefs.loadPrefs(ruleset);
    }

    private DocStruct monograph() throws Exception {
        DigitalDocument document = new DigitalDocument();
        return document.createDocStruct(prefs.getDocStrctTypeByName("Monograph"));
    }

    private AeonProperty field(String fieldXml) throws Exception {
        XMLConfiguration config = new XMLConfiguration();
        config.setExpressionEngine(new XPathExpressionEngine());
        config.load(new StringReader("<config_plugin><properties>" + fieldXml + "</properties></config_plugin>"));
        return new AeonProperty(config.configurationsAt("/properties/field").get(0), null);
    }

    private AeonProperty metadataField(String typeName, String value) throws Exception {
        AeonProperty property = field("<field><title>" + typeName + "</title>"
                + "<variable place=\"metadata\">" + typeName + "</variable>"
                + "<value></value></field>");
        property.setOverwriteMainField(true);
        property.setValue(value);
        return property;
    }

    private List<String> valuesOf(DocStruct docStruct, String typeName) {
        return docStruct.getAllMetadata()
                .stream()
                .filter(m -> m.getType().getName().equals(typeName))
                .map(Metadata::getValue)
                .toList();
    }

    // ----- unknownTypes: the pre-flight check that runs before anything is created -----

    @Test
    public void testUnknownTypesReportsATypeTheRulesetDoesNotDefine() throws Exception {
        List<String> unknown = AeonMetadataWriter.unknownTypes(prefs, List.of(metadataField("NoSuchType", "x")));
        assertEquals(List.of("NoSuchType"), unknown);
    }

    @Test
    public void testUnknownTypesIsEmptyWhenEveryTypeIsDefined() throws Exception {
        List<String> unknown = AeonMetadataWriter.unknownTypes(prefs, List.of(metadataField("TitleDocMain", "x")));
        assertTrue(unknown.isEmpty());
    }

    /**
     * A field that stays a process property names no metadata type, so the ruleset has nothing to say about it.
     */
    @Test
    public void testUnknownTypesIgnoresPropertyFields() throws Exception {
        AeonProperty property = field("<field><title>Patron type</title>"
                + "<variable place=\"process\">Patron type</variable><value></value></field>");
        assertTrue(AeonMetadataWriter.unknownTypes(prefs, List.of(property)).isEmpty());
    }

    @Test
    public void testUnknownTypesReportsEachNameOnce() throws Exception {
        List<AeonProperty> fields = List.of(metadataField("NoSuchType", "a"), metadataField("NoSuchType", "b"));
        assertEquals(List.of("NoSuchType"), AeonMetadataWriter.unknownTypes(prefs, fields));
    }

    // ----- add: a single value -----

    @Test
    public void testAddWritesTheValue() throws Exception {
        DocStruct docStruct = monograph();
        AeonMetadataWriter.add(docStruct, prefs, "TitleDocMain", "A title");
        assertEquals(List.of("A title"), valuesOf(docStruct, "TitleDocMain"));
    }

    @Test
    public void testAddRejectsATypeTheRulesetDoesNotDefine() throws Exception {
        DocStruct docStruct = monograph();
        AeonMetadataException thrown =
                assertThrows(AeonMetadataException.class, () -> AeonMetadataWriter.add(docStruct, prefs, "NoSuchType", "x"));
        assertEquals("NoSuchType", thrown.getMetadataType());
    }

    /**
     * The type exists but the docstruct does not permit it. This is the case pre-flight cannot catch, because the
     * docstruct type of a record is only known once the catalogue has answered.
     */
    @Test
    public void testAddRejectsATypeTheDocstructDoesNotPermit() throws Exception {
        DocStruct docStruct = monograph();
        assertNotNull(prefs.getMetadataTypeByName("CallNumber"));
        AeonMetadataException thrown =
                assertThrows(AeonMetadataException.class, () -> AeonMetadataWriter.add(docStruct, prefs, "CallNumber", "Mus 12"));
        assertEquals("CallNumber", thrown.getMetadataType());
    }

    // ----- add: a value the docstruct already holds -----

    private DocStruct boundBook() throws Exception {
        DigitalDocument document = new DigitalDocument();
        return document.createDocStruct(prefs.getDocStrctTypeByName("BoundBook"));
    }

    /**
     * The catalogue import and a record field can both map the same catalogue value, e.g. the title, to one type. The
     * second write carries nothing new, so it must not fail on a type that allows a single value.
     */
    @Test
    public void testAddSkipsAValueTheDocstructAlreadyHoldsOnASingleValuedType() throws Exception {
        DocStruct docStruct = boundBook();
        AeonMetadataWriter.add(docStruct, prefs, "pathimagefiles", "file:///images");
        AeonMetadataWriter.add(docStruct, prefs, "pathimagefiles", "file:///images");
        assertEquals(List.of("file:///images"), valuesOf(docStruct, "pathimagefiles"));
    }

    @Test
    public void testAddStoresAnIdenticalValueOnlyOnceOnARepeatableType() throws Exception {
        DocStruct docStruct = monograph();
        AeonMetadataWriter.add(docStruct, prefs, "TitleDocMain", "A title");
        AeonMetadataWriter.add(docStruct, prefs, "TitleDocMain", "A title");
        assertEquals(List.of("A title"), valuesOf(docStruct, "TitleDocMain"));
    }

    /**
     * Two sources disagreeing about a single valued type is a conflict, not a duplicate. It must still be reported.
     */
    @Test
    public void testAddStillRejectsADifferentValueOnASingleValuedType() throws Exception {
        DocStruct docStruct = boundBook();
        AeonMetadataWriter.add(docStruct, prefs, "pathimagefiles", "file:///images");
        AeonMetadataException thrown = assertThrows(AeonMetadataException.class,
                () -> AeonMetadataWriter.add(docStruct, prefs, "pathimagefiles", "file:///other"));
        assertEquals("pathimagefiles", thrown.getMetadataType());
        assertEquals(List.of("file:///images"), valuesOf(docStruct, "pathimagefiles"));
    }

    @Test
    public void testAddKeepsDifferentValuesOnARepeatableType() throws Exception {
        DocStruct docStruct = monograph();
        AeonMetadataWriter.add(docStruct, prefs, "TitleDocMain", "A title");
        AeonMetadataWriter.add(docStruct, prefs, "TitleDocMain", "Another title");
        assertEquals(List.of("A title", "Another title"), valuesOf(docStruct, "TitleDocMain"));
    }

}
