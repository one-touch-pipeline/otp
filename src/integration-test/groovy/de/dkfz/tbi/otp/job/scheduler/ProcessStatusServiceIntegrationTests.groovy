/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.scheduler

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.After
import org.junit.Test

import static org.junit.Assert.*

@Rollback
@Integration
class ProcessStatusServiceIntegrationTests {

    ProcessStatusService processStatusService

    private static final String LOG_FILE_DIRECTORY = "/tmp/statusTest"
    private static final String LOG_FILE = "/tmp/statusTest/status.log"
    File dir
    File file

    void setupData() {
        dir = new File(LOG_FILE_DIRECTORY)
        if (!dir.exists()) {
            assertTrue(dir.mkdirs())
        }
        file = new File(LOG_FILE)
        if (!file.exists()) {
            assertTrue(file.createNewFile())
        }
    }

    // false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @After
    void tearDown() {
        file.writable = true
        file.readable = true
        dir.writable = true
        assertTrue(file.delete())
        assertTrue(dir.delete())
    }

    @Test(expected = IllegalArgumentException)
    void testStatusLogFileNull() {
        setupData()
        processStatusService.statusLogFile(null)
    }

    @Test
    void testStatusLogFile() {
        setupData()
        assertEquals(LOG_FILE, processStatusService.statusLogFile(LOG_FILE_DIRECTORY))
    }

    @Test(expected = IllegalArgumentException)
    void testStatusSuccessfulLogFileNull() {
        setupData()
        processStatusService.statusSuccessful(null, "PreviousJob")
    }

    @Test(expected = IllegalArgumentException)
    void testStatusSuccessfulPreviousJobNull() {
        setupData()
        processStatusService.statusSuccessful(LOG_FILE, null)
    }

    @Test(expected = IllegalArgumentException)
    void testStatusSuccessfulNotReadable() {
        setupData()
        file.readable = false
        processStatusService.statusSuccessful(LOG_FILE, "PreviousJob")
    }

    @Test
    void testStatusSuccessful() {
        setupData()
        file << "PreviousJob\n"
        assertTrue(processStatusService.statusSuccessful(LOG_FILE, "PreviousJob"))
    }

    @Test
    void testStatusSuccessfulJobNotInFile() {
        setupData()
        file << "WrongPreviousTestJob\n"
        assertFalse(processStatusService.statusSuccessful(LOG_FILE, "PreviousJob"))
    }
}
