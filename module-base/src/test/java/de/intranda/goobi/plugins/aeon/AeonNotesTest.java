package de.intranda.goobi.plugins.aeon;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class AeonNotesTest {

    private static Map<String, Object> note(String date, String text, String username, String type) {
        Map<String, Object> note = new LinkedHashMap<>();
        note.put("id", Integer.valueOf(1));
        note.put("transactionNumber", Integer.valueOf(1144157));
        note.put("noteDate", date);
        note.put("noteText", text);
        note.put("username", username);
        note.put("noteType", type);
        return note;
    }

    @Test
    public void testNoNotes() {
        assertEquals("", AeonNotes.format(null));
        assertEquals("", AeonNotes.format(new ArrayList<>()));
    }

    @Test
    public void testOnlySystemNotes() {
        assertEquals("", AeonNotes.format(List.of(note("2026-03-02T20:16:26.5Z", "Cleared", "System", "System"))));
    }

    @Test
    public void testSkipsEmptyTextAndMissingParts() {
        List<Map<String, Object>> notes = new ArrayList<>();
        notes.add(note("2025-01-01T00:00:00Z", "  ", "a", "Staff"));
        notes.add(note(null, "No date", null, "User"));
        notes.add(null);
        assertEquals("No date", AeonNotes.format(notes));
    }
}
