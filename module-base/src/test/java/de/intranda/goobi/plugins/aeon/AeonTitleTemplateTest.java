package de.intranda.goobi.plugins.aeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * The process title is configured as fixed text with {Field title} placeholders. A placeholder names a transaction or
 * property field by its title; the plugin supplies the current value of that field when the title is rendered.
 */
public class AeonTitleTemplateTest {

    // core's default ProcessTitleGenerationRegex
    private static final String INVALID = "[\\W]";

    private Map<String, String> values;

    @Before
    public void setUp() {
        values = new HashMap<>();
        values.put("Transaction Identifier", "286670");
        values.put("Home site", "MUS");
    }

    private AeonTitleTemplate.Result render(String template) {
        return AeonTitleTemplate.parse(template).render(values::get, INVALID);
    }

    // ----- parsing -----

    @Test(expected = IllegalArgumentException.class)
    public void testBlankTemplateIsRejected() {
        AeonTitleTemplate.parse("  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTemplateIsRejected() {
        AeonTitleTemplate.parse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnclosedPlaceholderIsRejected() {
        AeonTitleTemplate.parse("{Transaction Identifier_group");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnopenedPlaceholderIsRejected() {
        AeonTitleTemplate.parse("Transaction Identifier}_group");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNestedPlaceholderIsRejected() {
        AeonTitleTemplate.parse("{Transaction {Home site}}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPlaceholderIsRejected() {
        AeonTitleTemplate.parse("{ }_group");
    }

    // ----- references -----

    @Test
    public void testReferencesInOrderOfAppearance() {
        assertEquals(List.of("Transaction Identifier", "Home site"),
                AeonTitleTemplate.parse("{Transaction Identifier}_{Home site}_group").references());
    }

    @Test
    public void testReferencesAreTrimmed() {
        assertEquals(List.of("Home site"), AeonTitleTemplate.parse("x_{ Home site }").references());
    }

    @Test
    public void testReferencesListEachFieldOnce() {
        assertEquals(List.of("Home site"), AeonTitleTemplate.parse("{Home site}_{Home site}").references());
    }

    @Test
    public void testFixedTextOnlyHasNoReferences() {
        assertEquals(List.of(), AeonTitleTemplate.parse("static_title").references());
    }

    // ----- rendering -----

    @Test
    public void testRenderReplacesPlaceholdersAndKeepsFixedText() {
        AeonTitleTemplate.Result result = render("{Transaction Identifier}_{Home site}_group");
        assertTrue(result.isComplete());
        assertEquals("286670_MUS_group", result.title());
    }

    @Test
    public void testRenderFixedTextOnly() {
        assertEquals("static_title", render("static_title").title());
    }

    @Test
    public void testFixedTextIsNotSanitised() {
        // the fixed text is configured on purpose; only values that arrive from AEON or the operator are cleaned
        assertEquals("AEON-286670", render("AEON-{Transaction Identifier}").title());
    }

    @Test
    public void testValuesAreSanitised() {
        values.put("Home site", "Music Lib./B");
        assertEquals("286670_Music_Lib__B", render("{Transaction Identifier}_{Home site}").title());
    }

    @Test
    public void testAbsentFieldValueIsReportedAsEmpty() {
        values.remove("Home site");
        AeonTitleTemplate.Result result = render("{Transaction Identifier}_{Home site}");
        assertFalse(result.isComplete());
        assertEquals(List.of("Home site"), result.emptyFields());
    }

    @Test
    public void testBlankFieldValueIsReportedAsEmpty() {
        values.put("Home site", "  ");
        assertEquals(List.of("Home site"), render("{Transaction Identifier}_{Home site}").emptyFields());
    }

    @Test
    public void testEveryEmptyFieldIsReportedOnce() {
        values.clear();
        assertEquals(List.of("Transaction Identifier", "Home site"),
                render("{Transaction Identifier}_{Home site}_{Home site}").emptyFields());
    }
}
