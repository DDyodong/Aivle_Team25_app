package com.example.app;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void loginRejectsNullAndBlankCredentials() {
        MockTbmRepository repository = new MockTbmRepository();

        assertFalse(repository.login(null, "password"));
        assertFalse(repository.login("employee", null));
        assertFalse(repository.login(" ", "password"));
        assertTrue(repository.login("240071", "password"));
    }

    @Test
    public void completingTodayTbmUpdatesExistingHistoryRecord() {
        MockTbmRepository repository = new MockTbmRepository();

        repository.completeTodayTbm(8, "first briefing", true);
        repository.completeTodayTbm(9, "updated briefing", true);

        List<TbmRecord> records = repository.getRecentRecords();
        assertEquals(4, records.size());
        assertEquals(repository.getTodayTbm().id, records.get(0).id);
        assertEquals(9, records.get(0).participants);
        assertEquals("updated briefing", records.get(0).briefing);
    }
}
