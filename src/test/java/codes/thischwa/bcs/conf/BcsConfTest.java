package codes.thischwa.bcs.conf;

import codes.thischwa.bcs.AbstractTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BcsConfTest extends AbstractTest {

    @Autowired
    private BcsConf bcsConf;

    @Test
    void testProperties() {
        assertEquals("BirthdayCalendarService", bcsConf.product());
        assertEquals("Birthday", bcsConf.calendarCategory());
        assertEquals("0 30 4 * * SUN", bcsConf.cron());
        assertFalse(bcsConf.runOnStart());
    }

    @Test
    void testGetProdId() {
        String expectedProdId = "-//BirthdayCalendarService//iCal4j 1.0//EN";
        assertEquals(expectedProdId, bcsConf.getProdId());
    }

}