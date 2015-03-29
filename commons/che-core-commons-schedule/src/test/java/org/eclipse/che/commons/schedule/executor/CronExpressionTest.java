/*.
 * Copyright 2001-2009 Terracotta, Inc..
 *.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not.
 * use this file except in compliance with the License. You may obtain a copy.
 * of the License at.
 *.
 *   http://www.apache.org/licenses/LICENSE-2.0.
 *...
 * Unless required by applicable law or agreed to in writing, software.
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT.
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the.
 * License for the specific language governing permissions and limitations.
 * under the License.
 */
package org.eclipse.che.commons.schedule.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.testng.Assert.*;

public class CronExpressionTest {
    private static final Logger   LOG      = LoggerFactory.getLogger(CronExpressionTest.class);
    
    private static final String[] VERSIONS = new String[]{"1.5.2"};

    private static final TimeZone EST_TIME_ZONE = TimeZone.getTimeZone("US/Eastern");


    @Test
    public void testIsSatisfiedBy() throws Exception {
        CronExpression cronExpression = new CronExpression("0 15 10 * * ? 2005");

        Calendar cal = Calendar.getInstance();

        cal.set(2005, Calendar.JUNE, 1, 10, 15, 0);
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(Calendar.YEAR, 2006);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal = Calendar.getInstance();
        cal.set(2005, Calendar.JUNE, 1, 10, 16, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cal = Calendar.getInstance();
        cal.set(2005, Calendar.JUNE, 1, 10, 14, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));
    }

    @Test
    public void testLastDayOffset() throws Exception {
        CronExpression cronExpression = new CronExpression("0 15 10 L-2 * ? 2010");

        Calendar cal = Calendar.getInstance();

        cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0); // last day - 2
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cal.set(2010, Calendar.OCTOBER, 28, 10, 15, 0);
        assertFalse(cronExpression.isSatisfiedBy(cal.getTime()));

        cronExpression = new CronExpression("0 15 10 L-5W * ? 2010");

        cal.set(2010, Calendar.OCTOBER, 26, 10, 15, 0); // last day - 5
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cronExpression = new CronExpression("0 15 10 L-1 * ? 2010");

        cal.set(2010, Calendar.OCTOBER, 30, 10, 15, 0); // last day - 1
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

        cronExpression = new CronExpression("0 15 10 L-1W * ? 2010");

        cal.set(2010, Calendar.OCTOBER, 29, 10, 15, 0); // nearest weekday to last day - 1 (29th is a friday in 2010)
        assertTrue(cronExpression.isSatisfiedBy(cal.getTime()));

    }


    /**
     * QTZ-259 : last day offset causes repeating fire time
     */
    @Test
    public void testQtz259() throws Exception {
        CronExpression cronExpression = new CronExpression("0 0 0 L-2 * ? *");


        int i = 0;
        Date pdate = cronExpression.getNextValidTimeAfter(new Date());
        while (++i < 26) {
            Date date = cronExpression.getNextValidTimeAfter(pdate);
            LOG.info("fireTime: " + date + ", previousFireTime: " + pdate);
            assertFalse(pdate.equals(date), "Next fire time is the same as previous fire time!");
            pdate = date;
        }
    }

    /**
     * QTZ-259 : last day offset causes repeating fire time
     */
    @Test
    public void testQtz259LW() throws Exception {
        CronExpression cronExpression = new CronExpression("0 0 0 LW * ? *");


        int i = 0;
        Date pdate = cronExpression.getNextValidTimeAfter(new Date());
        while (++i < 26) {
            Date date = cronExpression.getNextValidTimeAfter(pdate);
            LOG.info("fireTime: " + date + ", previousFireTime: " + pdate);
            assertFalse(pdate.equals(date), "Next fire time is the same as previous fire time!");
            pdate = date;
        }
    }

    /*
     * QUARTZ-574: Showing that storeExpressionVals correctly calculates the month number
     */
    @Test
    public void testQuartz574() {
        try {
            new CronExpression("* * * * Foo ? ");
            fail("Expected ParseException did not fire for non-existent month");
        } catch (IllegalArgumentException pe) {
            assertTrue(pe.getMessage().startsWith("Invalid Month value:"), "Incorrect ParseException thrown"
                      );
        }

        try {
            new CronExpression("* * * * Jan-Foo ? ");
            fail("Expected ParseException did not fire for non-existent month");
        } catch (IllegalArgumentException pe) {
            assertTrue(pe.getMessage().startsWith("Invalid Month value:"), "Incorrect ParseException thrown"
                      );
        }
    }

    @Test
    public void testQuartz621() {
        try {
            new CronExpression("0 0 * * * *");
            fail("Expected ParseException did not fire for wildcard day-of-month and day-of-week");
        } catch (IllegalArgumentException pe) {
            assertTrue(
                    pe.getMessage().startsWith(
                            "Support for specifying both a day-of-week AND a day-of-month parameter is not " +
                            "implemented."), "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 0 * 4 * *");
            fail("Expected ParseException did not fire for specified day-of-month and wildcard day-of-week");
        } catch (IllegalArgumentException pe) {
            assertTrue(
                    pe.getMessage().startsWith(
                            "Support for specifying both a day-of-week AND a day-of-month parameter is not " +
                            "implemented."), "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 0 * * * 4");
            fail("Expected ParseException did not fire for wildcard day-of-month and specified day-of-week");
        } catch (IllegalArgumentException pe) {
            assertTrue(
                    pe.getMessage().startsWith(
                            "Support for specifying both a day-of-week AND a day-of-month parameter is not " +
                            "implemented."), "Incorrect ParseException thrown");
        }
    }

    @Test
    public void testQuartz640() throws ParseException {
        try {
            new CronExpression("0 43 9 1,5,29,L * ?");
            fail("Expected ParseException did not fire for L combined with other days of the month");
        } catch (IllegalArgumentException pe) {
            assertTrue(
                    pe.getMessage().startsWith(
                            "Support for specifying 'L' and 'LW' with other days of the month is not implemented"),
                    "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 43 9 ? * SAT,SUN,L");
            fail("Expected ParseException did not fire for L combined with other days of the week");
        } catch (IllegalArgumentException pe) {
            assertTrue(
                    pe.getMessage()
                      .startsWith("Support for specifying 'L' with other days of the week is not implemented"),
                    "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 43 9 ? * 6,7,L");
            fail("Expected ParseException did not fire for L combined with other days of the week");
        } catch (IllegalArgumentException pe) {
            assertTrue(
                    pe.getMessage()
                      .startsWith("Support for specifying 'L' with other days of the week is not implemented"),
                    "Incorrect ParseException thrown");
        }
        try {
            new CronExpression("0 43 9 ? * 5L");
        } catch (IllegalArgumentException pe) {
            fail("Unexpected ParseException thrown for supported '5L' expression.");
        }
    }

    @Test
    public void testQtz96() throws ParseException {
        try {
            new CronExpression("0/5 * * 32W 1 ?");
            fail("Expected ParseException did not fire for W with value larger than 31");
        } catch (IllegalArgumentException pe) {
            assertTrue(pe.getMessage().startsWith("The 'W' option does not make sense with values larger than"),
                       "Incorrect ParseException thrown");
        }
    }

    @Test
    public void testQtz395_CopyConstructorMustPreserveTimeZone() throws ParseException {
        TimeZone nonDefault = TimeZone.getTimeZone("Europe/Brussels");
        if (nonDefault.equals(TimeZone.getDefault())) {
            nonDefault = EST_TIME_ZONE;
        }
        CronExpression cronExpression = new CronExpression("0 15 10 * * ? 2005");
        cronExpression.setTimeZone(nonDefault);

        CronExpression copyCronExpression = new CronExpression(cronExpression);
        assertEquals(nonDefault, copyCronExpression.getTimeZone());
    }

    @Test
    public void cron() {
        CronExpression expression = new CronExpression("0 0 14-6 ? * FRI-MON");
        assertEquals(expression.getExpressionSummary(),
                     "seconds: 0\n" +
                     "minutes: 0\n" +
                     "hours: 0,1,2,3,4,5,6,14,15,16,17,18,19,20,21,22,23\n" +
                     "daysOfMonth: ?\n" +
                     "months: *\n" +
                     "daysOfWeek: 1,2,6,7\n" +
                     "lastdayOfWeek: false\n" +
                     "nearestWeekday: false\n" +
                     "NthDayOfWeek: 0\n" +
                     "lastdayOfMonth: false\n" +
                     "years: *\n"
                    );

        expression = new CronExpression("0 0 0-23 ? * *");
        assertEquals(expression.getExpressionSummary(),
                     "seconds: 0\n" +
                     "minutes: 0\n" +
                     "hours: 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23\n" +
                     "daysOfMonth: ?\n" +
                     "months: *\n" +
                     "daysOfWeek: *\n" +
                     "lastdayOfWeek: false\n" +
                     "nearestWeekday: false\n" +
                     "NthDayOfWeek: 0\n" +
                     "lastdayOfMonth: false\n" +
                     "years: *\n"
                    );


        expression = new CronExpression("0-59 0-59 0-23 ? * *");
        assertEquals(expression.getExpressionSummary(),
                     "seconds: 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31," +
                     "32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59\n" +
                     "minutes: 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31," +
                     "32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59\n" +
                     "hours: 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23\n" +
                     "daysOfMonth: ?\n" +
                     "months: *\n" +
                     "daysOfWeek: *\n" +
                     "lastdayOfWeek: false\n" +
                     "nearestWeekday: false\n" +
                     "NthDayOfWeek: 0\n" +
                     "lastdayOfMonth: false\n" +
                     "years: *\n"
                    );

        expression = new CronExpression("0/5 0-59 0-23 ? * *");
        assertEquals(expression.getExpressionSummary(),
                     "seconds: 0,5,10,15,20,25,30,35,40,45,50,55\n" +
                     "minutes: 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31," +
                     "32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59\n" +
                     "hours: 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23\n" +
                     "daysOfMonth: ?\n" +
                     "months: *\n" +
                     "daysOfWeek: *\n" +
                     "lastdayOfWeek: false\n" +
                     "nearestWeekday: false\n" +
                     "NthDayOfWeek: 0\n" +
                     "lastdayOfMonth: false\n" +
                     "years: *\n"
                    );

        // comma test
        expression = new CronExpression("0 5,35 * * * ?");
        assertEquals(expression.getExpressionSummary(),
                     "seconds: 0\n" +
                     "minutes: 5,35\n" +
                     "hours: *\n" +
                     "daysOfMonth: *\n" +
                     "months: *\n" +
                     "daysOfWeek: ?\n" +
                     "lastdayOfWeek: false\n" +
                     "nearestWeekday: false\n" +
                     "NthDayOfWeek: 0\n" +
                     "lastdayOfMonth: false\n" +
                     "years: *\n"
                    );

        expression = new CronExpression("0/2 * * * * ?");
        assertEquals(expression.getExpressionSummary(),
                     "seconds: 0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40,42,44,46,48,50,52,54,56,58\n" +
                     "minutes: *\n" +
                     "hours: *\n" +
                     "daysOfMonth: *\n" +
                     "months: *\n" +
                     "daysOfWeek: ?\n" +
                     "lastdayOfWeek: false\n" +
                     "nearestWeekday: false\n" +
                     "NthDayOfWeek: 0\n" +
                     "lastdayOfMonth: false\n" +
                     "years: *\n"
                    );
    }

}
