package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class AeonProcessCreationPluginTest {

    private static String resourcesFolder;

    @BeforeClass
    public static void setUpClass() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        String log4jFile = resourcesFolder + "log4j2.xml"; // for junit tests in eclipse

        System.setProperty("log4j.configurationFile", log4jFile);
    }

    @Test
    public void testVersion() throws IOException {
        String s = "xyz";
        assertNotNull(s);
    }

    @Test
    public void recordTitleMarksTheOrderNumberAsUnknownBeforeCreation() {
        assertEquals("txn123_music_\u2026", AeonProcessCreationWorkflowPlugin.composeRecordTitle("txn123_music", false));
    }

    @Test
    public void recordTitleIsShownUnchangedAfterCreation() {
        assertEquals("txn123_music_001", AeonProcessCreationWorkflowPlugin.composeRecordTitle("txn123_music_001", true));
    }

    // ----- reporting a material type AEON did not deliver -----

    /*
     * An installation states through <materialTypeField> whether it works with material types at all. AEON stopped
     * delivering one, so an empty field is the supported setup rather than a fault: no property restricted to a
     * material type applies, which is the intended result and nothing to report. A field the configuration does name
     * and AEON leaves blank is the opposite - a value was expected and did not arrive.
     */

    @Test
    public void reportsAConfiguredMaterialTypeAeonDoesNotDeliver() {
        assertTrue(AeonProcessCreationWorkflowPlugin.shouldReportMissingMaterialType("customFieldValues/MaterialType", null));
    }

    @Test
    public void reportsAConfiguredMaterialTypeAeonDeliversEmpty() {
        assertTrue(AeonProcessCreationWorkflowPlugin.shouldReportMissingMaterialType("customFieldValues/MaterialType", "   "));
    }

    @Test
    public void staysSilentWhenTheMaterialTypeArrives() {
        assertFalse(AeonProcessCreationWorkflowPlugin.shouldReportMissingMaterialType("customFieldValues/MaterialType", "Film (DRMS)"));
    }

    @Test
    public void staysSilentWhenNoMaterialTypeFieldIsConfigured() {
        assertFalse(AeonProcessCreationWorkflowPlugin.shouldReportMissingMaterialType(null, null));
    }

    @Test
    public void staysSilentWhenTheConfiguredMaterialTypeFieldIsBlank() {
        assertFalse(AeonProcessCreationWorkflowPlugin.shouldReportMissingMaterialType("   ", null));
    }

    // ----- the legacy per record creation mode -----

    /*
     * Creating one process per record is a legacy mode, off unless <allowMultiProcessCreation> switches it on. Hiding
     * the radio alone would still let a stale or crafted request post 'multi', so the bean itself falls back to the
     * batch mode whenever the legacy one is not allowed.
     */

    @Test
    public void fallsBackToBatchModeWhenMultiIsNotAllowed() {
        assertEquals("single", AeonProcessCreationWorkflowPlugin.effectiveCreationMode("multi", false));
    }

    @Test
    public void keepsMultiModeWhenAllowed() {
        assertEquals("multi", AeonProcessCreationWorkflowPlugin.effectiveCreationMode("multi", true));
    }

    @Test
    public void keepsBatchModeWhenMultiIsNotAllowed() {
        assertEquals("single", AeonProcessCreationWorkflowPlugin.effectiveCreationMode("single", false));
    }

    @Test
    public void keepsBatchModeWhenMultiIsAllowed() {
        assertEquals("single", AeonProcessCreationWorkflowPlugin.effectiveCreationMode("single", true));
    }

    @Test
    public void fallsBackToBatchModeWithoutARequestedMode() {
        assertEquals("single", AeonProcessCreationWorkflowPlugin.effectiveCreationMode(null, true));
    }

    @Test
    public void fallsBackToBatchModeForAnUnknownMode() {
        assertEquals("single", AeonProcessCreationWorkflowPlugin.effectiveCreationMode("bogus", true));
    }

    // ----- record keys, numbered per request -----

    /*
     * Each selected record is keyed by the transaction and its position among the selected records. The key prefixes
     * the record's properties on a batch and is written as its child key metadata, so it has to be unique across all
     * processes - which the transaction provides - while staying readable within the batch.
     */

    @Test
    public void recordKeyJoinsTransactionAndPaddedPosition() {
        assertEquals("286670_0001", AeonProcessCreationWorkflowPlugin.recordKey("286670", 1));
    }

    @Test
    public void recordKeyPadsTwoDigitPositions() {
        assertEquals("286670_0012", AeonProcessCreationWorkflowPlugin.recordKey("286670", 12));
    }

    @Test
    public void recordKeyKeepsPositionsBeyondFourDigits() {
        assertEquals("286670_10000", AeonProcessCreationWorkflowPlugin.recordKey("286670", 10000));
    }

    @Test
    public void recordKeyTrimsTheTransaction() {
        assertEquals("286670_0001", AeonProcessCreationWorkflowPlugin.recordKey(" 286670 ", 1));
    }

    @Test
    public void perRecordTitlesNumberTheProcessesOfOneRequest() {
        assertEquals(List.of("286670_MUS_0001", "286670_MUS_0002", "286670_MUS_0003"),
                AeonProcessCreationWorkflowPlugin.perRecordTitles("286670_MUS", 3));
    }

    @Test
    public void perRecordTitlesAreEmptyWithoutRecords() {
        assertEquals(List.of(), AeonProcessCreationWorkflowPlugin.perRecordTitles("286670_MUS", 0));
    }
}
