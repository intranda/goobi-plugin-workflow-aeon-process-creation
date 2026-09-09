package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.goobi.plugins.aeon.AeonRecord;

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
    public void appendsTheConfiguredBatchTitleSuffix() {
        assertEquals("txn123_music_group", AeonProcessCreationWorkflowPlugin.composeBatchTitle("txn123_music", "_group"));
    }

    @Test
    public void leavesTheBatchTitleAloneWithoutASuffix() {
        assertEquals("txn123_music", AeonProcessCreationWorkflowPlugin.composeBatchTitle("txn123_music", null));
    }

    @Test
    public void leavesTheBatchTitleAloneWithABlankSuffix() {
        assertEquals("txn123_music", AeonProcessCreationWorkflowPlugin.composeBatchTitle("txn123_music", "   "));
    }

    @Test
    public void trimsWhitespaceAroundTheSuffix() {
        assertEquals("txn123_music_group", AeonProcessCreationWorkflowPlugin.composeBatchTitle("txn123_music", "  _group  "));
    }

    @Test
    public void batchTitlePreviewCarriesTheConfiguredSuffix() {
        assertEquals("txn123_music_group",
                AeonProcessCreationWorkflowPlugin.composeBatchTitle(List.of(recordTitled("txn123_music")), "_group"));
    }

    @Test
    public void batchTitlePreviewTakesTheTitleOfTheFirstRecord() {
        assertEquals("txn123_music_group", AeonProcessCreationWorkflowPlugin
                .composeBatchTitle(List.of(recordTitled("txn123_music"), recordTitled("txn123_music")), "_group"));
    }

    @Test
    public void batchTitlePreviewIsBlankWithoutRecords() {
        assertEquals("", AeonProcessCreationWorkflowPlugin.composeBatchTitle(Collections.<AeonRecord> emptyList(), "_group"));
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

    private static AeonRecord recordTitled(String processTitle) {
        AeonRecord record = new AeonRecord();
        record.setProcessTitle(processTitle);
        return record;
    }
}
