package de.intranda.goobi.plugins.aeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.junit.Test;

import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;

/**
 * Both checks here exist because the failure they prevent is silent. A misspelled place attribute stores a value where
 * nothing reads it back from; a transaction field name that does not match the field carrying the transaction number
 * makes the duplicate check and the cancellation search return nothing, which reads as 'no duplicates' rather than as
 * an error.
 */
public class AeonConfigValidatorTest {

    private AeonProperty field(String fieldXml) throws Exception {
        XMLConfiguration config = new XMLConfiguration();
        config.setExpressionEngine(new XPathExpressionEngine());
        config.load(new StringReader("<config_plugin><properties>" + fieldXml + "</properties></config_plugin>"));
        return new AeonProperty(config.configurationsAt("/properties/field").get(0), null);
    }

    // ----- invalidPlaces -----

    @Test
    public void testInvalidPlacesReportsAMisspelledPlace() throws Exception {
        AeonProperty broken = field("<field><title>Title</title>"
                + "<variable place=\"metdata\">TitleDocMain</variable><value></value></field>");
        assertEquals(List.of("Title"), AeonConfigValidator.invalidPlaces(List.of(broken)));
    }

    @Test
    public void testInvalidPlacesAcceptsBothStores() throws Exception {
        AeonProperty metadata = field("<field><title>Title</title>"
                + "<variable place=\"metadata\">TitleDocMain</variable><value></value></field>");
        AeonProperty property = field("<field><title>Patron type</title>"
                + "<variable place=\"process\">Patron type</variable><value></value></field>");
        assertTrue(AeonConfigValidator.invalidPlaces(List.of(metadata, property)).isEmpty());
    }

    /**
     * The plugin documentation offers these two and its own examples use them, though nothing ever acted on either.
     */
    @Test
    public void testInvalidPlacesAcceptsTheDocumentedLegacyValues() throws Exception {
        AeonProperty work = field("<field><title>CoWorker</title>"
                + "<variable place=\"work\">CoWorker</variable><value></value></field>");
        AeonProperty template = field("<field><title>Status</title>"
                + "<variable place=\"template\">Status</variable><value></value></field>");
        assertTrue(AeonConfigValidator.invalidPlaces(List.of(work, template)).isEmpty());
    }

    @Test
    public void testInvalidPlacesAcceptsAFieldWithNoPlaceAtAll() throws Exception {
        AeonProperty property = field("<field><title>Patron type</title>"
                + "<variable>Patron type</variable><value></value></field>");
        assertTrue(AeonConfigValidator.invalidPlaces(List.of(property)).isEmpty());
    }

    // ----- transactionNumberField -----

    @Test
    public void testTransactionNumberFieldIsFoundByItsAeonField() throws Exception {
        AeonProperty transactionNumber = field("<field aeon=\"transactionNumber\"><title>Transaction Identifier</title>"
                + "<variable place=\"metadata\">TransactionIdentifier</variable><value></value></field>");
        AeonProperty other = field("<field aeon=\"itemTitle\"><title>Item title</title>"
                + "<variable place=\"metadata\">TitleDocMain</variable><value></value></field>");
        AeonProperty found = AeonConfigValidator.transactionNumberField(List.of(other, transactionNumber), "transactionNumber");
        assertEquals("TransactionIdentifier", found.getVariableName());
    }

    @Test
    public void testTransactionNumberFieldIsNullWhenNoFieldCarriesIt() throws Exception {
        AeonProperty other = field("<field aeon=\"itemTitle\"><title>Item title</title>"
                + "<variable place=\"metadata\">TitleDocMain</variable><value></value></field>");
        assertNull(AeonConfigValidator.transactionNumberField(List.of(other), "transactionNumber"));
    }

    // ----- catalogueByName -----

    private ConfigOpacCatalogue catalogue(String title) {
        return new ConfigOpacCatalogue(title, null, null, null, null, 80, null, "intranda_opac_json", null);
    }

    @Test
    public void testCatalogueByNameFindsTheConfiguredCatalogue() {
        List<ConfigOpacCatalogue> catalogues = List.of(catalogue("K10Plus"), catalogue("Metadata cloud"));
        assertEquals("Metadata cloud", AeonConfigValidator.catalogueByName(catalogues, "Metadata cloud").getTitle());
    }

    /**
     * The name no catalogue carries is what produced the NullPointerException this check replaces: the lookup found
     * nothing, and the request went on to call the catalogue that was never found.
     */
    @Test
    public void testCatalogueByNameIsNullWhenNoCatalogueCarriesTheName() {
        List<ConfigOpacCatalogue> catalogues = List.of(catalogue("Opac Name"), catalogue("K10Plus"));
        assertNull(AeonConfigValidator.catalogueByName(catalogues, "Metadata cloud"));
    }

    /**
     * Goobi matches catalogue titles exactly, and the plugin documentation and its sample configuration spell this
     * one differently, so a case difference is a mistake someone will actually make.
     */
    @Test
    public void testCatalogueByNameIsCaseSensitive() {
        assertNull(AeonConfigValidator.catalogueByName(List.of(catalogue("Metadata cloud")), "metadata cloud"));
    }

    /**
     * An absent opacName element leaves the name blank. Reported as the same misconfiguration rather than matched
     * against a catalogue that happens to carry no title.
     */
    @Test
    public void testCatalogueByNameIsNullWhenNoNameIsConfigured() {
        List<ConfigOpacCatalogue> catalogues = List.of(catalogue(""), catalogue("K10Plus"));
        assertNull(AeonConfigValidator.catalogueByName(catalogues, ""));
        assertNull(AeonConfigValidator.catalogueByName(catalogues, null));
    }

    // ----- catalogueTitles -----

    /**
     * The titles go into the error message, because the catalogues a workflow is offered come from three files at
     * once and naming the ones that exist is what makes the message actionable.
     */
    @Test
    public void testCatalogueTitlesNamesEveryOfferedCatalogue() {
        List<ConfigOpacCatalogue> catalogues = List.of(catalogue("Opac Name"), catalogue("K10Plus"));
        assertEquals("Opac Name, K10Plus", AeonConfigValidator.catalogueTitles(catalogues));
    }

    @Test
    public void testCatalogueTitlesOfNoCatalogueAtAll() {
        assertEquals("", AeonConfigValidator.catalogueTitles(List.of()));
    }
}
