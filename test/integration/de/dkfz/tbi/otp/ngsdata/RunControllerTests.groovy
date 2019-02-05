/*
 * Copyright 2011-2019 The OTP authors
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

package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.*

import de.dkfz.tbi.otp.security.UserAndRoles

import static org.junit.Assert.*

class RunControllerTests implements UserAndRoles {

    RunController controller

    @Before
    void setUp() {
        controller = new RunController()
        createUserAndRoles()
    }

    @Test
    void testDisplayRedirect() {
        controller.display()
        assertEquals("/run/show", controller.response.redirectedUrl)
    }

    @Test
    void testShowRunNonExisting() {
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.params.id = "0"
            controller.show()
            assertEquals(404, controller.response.status)
        }
    }

    @Test
    void testShowRunMissingId() {
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.show()
            assertEquals(404, controller.response.status)
        }
    }

    @Test
    void testShowRunIdNoLong() {
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.params.id = "test"
            controller.show()
            assertEquals(404, controller.response.status)
        }
    }

    @Ignore
    @Test
    void testShowRunMinimalData() {
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())

        // test the outcome
        controller.params.id = run.id.toString()
        def model = controller.show()
        assertEquals(run, model.run)
        assertEquals(0, model.finalPaths.size())
        assertNull(model.keys[0])
        assertNull(model.keys[1])
        assertTrue(model.processParameters.isEmpty())
        assertTrue(model.metaDataFiles.isEmpty())
        assertTrue(model.seqTracks.isEmpty())
        assertNull(model.nextRun)
        assertNull(model.previousRun)
    }

    @Ignore
    @Test
    void testShowRunByName() {
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())

        // test the outcome
        controller.params.id = "test"
        def model = controller.show()
        assertEquals(run, model.run)
        assertEquals(0, model.finalPaths.size())
        assertNull(model.keys[0])
        assertNull(model.keys[1])
        assertTrue(model.processParameters.isEmpty())
        assertTrue(model.metaDataFiles.isEmpty())
        assertTrue(model.seqTracks.isEmpty())
        assertNull(model.nextRun)
        assertNull(model.previousRun)
    }
}
