package de.intranda.goobi.plugins.aeon;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * The cancellation list is sorted by the container grouping of each record. It used to be read out of the process
 * properties on every single comparison, under a hardcoded property name; now it is resolved once when the record is
 * built, so the comparison is a plain string compare and the name comes from the configuration.
 */
public class AeonRecordTest {

    private AeonRecord cancellationRecord(String orderValue) {
        AeonRecord record = new AeonRecord();
        record.setOrderValue(orderValue);
        return record;
    }

    @Test
    public void testCancellationRecordsSortByTheirOrderValue() {
        List<AeonRecord> records = new ArrayList<>(List.of(cancellationRecord("b 2"), cancellationRecord("a 1")));
        records.sort(null);
        assertTrue(records.get(0).getOrderValue().equals("a 1"));
    }

    @Test
    public void testCancellationRecordsWithoutAnOrderValueCompareEqual() {
        assertTrue(cancellationRecord("").compareTo(cancellationRecord("")) == 0);
    }
}
