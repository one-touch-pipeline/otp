package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.UserAndRoles

import static org.junit.Assert.*

import org.junit.*
import org.springframework.security.acls.domain.BasePermission
import grails.plugin.springsecurity.SpringSecurityUtils


class RunControllerTests implements UserAndRoles{

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
        SpringSecurityUtils.doWithAuth("testuser") {
            controller.params.id = "0"
            controller.show()
            assertEquals(404, controller.response.status)
        }
    }

    @Test
    void testShowRunMissingId() {
        SpringSecurityUtils.doWithAuth("testuser") {
            controller.show()
            assertEquals(404, controller.response.status)
        }
    }

    @Test
    void testShowRunIdNoLong() {
        SpringSecurityUtils.doWithAuth("testuser") {
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
