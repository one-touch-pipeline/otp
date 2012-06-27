package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.test.ControllerUnitTestCase
import org.junit.*

class RunControllerTests extends ControllerUnitTestCase {

    void testDisplayRedirect() {
        controller.display()
        assertEquals("show", controller.redirectArgs.action)
    }

    void testShowRunNonExisting() {
        controller.params.id = "0"
        controller.show()
        assertEquals(404, controller.response.status)
    }

    void testShowRunMissingId() {
        controller.show()
        assertEquals(404, controller.response.status)
    }

    void testShowRunIdNoLong() {
        controller.params.id = "test"
        controller.show()
        assertEquals(404, controller.response.status)
    }

    @Ignore
    void testShowRunMinimalData() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
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
    void testShowRunByName() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
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

    @Ignore
    void testShowRunWithNextRun() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())
        Run run2 = new Run(name: "test1", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run2.save())
        Run run3 = new Run(name: "test2", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run3.save())
        // test the outcome
        controller.params.id = run.id.toString()
        def model = controller.show()
        assertEquals(run, model.run)
        assertEquals(run2, model.nextRun)
        assertNull(model.previousRun)
    }

    void testShowRunWithPrevRun() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())
        Run run2 = new Run(name: "test1", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run2.save())
        Run run3 = new Run(name: "test2", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run3.save())
        // test the outcome
        controller.params.id = run3.id.toString()
        def model = controller.show()
        assertEquals(run3, model.run)
        assertEquals(run2, model.previousRun)
        assertNull(model.nextRun)
    }

    void testShowRunWithPrevNextRun() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run1 = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run1.save())
        Run run2 = new Run(name: "test1", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run2.save())
        Run run3 = new Run(name: "test2", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run3.save())
        // test the outcome
        controller.params.id = run2.id.toString()
        def model = controller.show()
        assertEquals(run2, model.run)
        assertEquals(run1, model.previousRun)
        assertEquals(run3, model.nextRun)
    }
}
