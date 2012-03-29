package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import java.io.File;

import org.junit.*

import de.dkfz.tbi.otp.testing.AbstractIntegrationTest;

class MetaDataRegistrationServiceTests extends AbstractIntegrationTest {

    def metaDataRegistrationService

    File dataPath
    File mdPath

    @Before
    void setUp() {
        if(!new File("/tmp/otp/dataPath").isDirectory()) {
            new File("/tmp/otp/dataPath").mkdirs()
            assertTrue(new File("/tmp/otp/dataPath").isDirectory())
        }
        if(!new File("/tmp/otp/mdPath").isDirectory()) {
            new File("/tmp/otp/mdPath").mkdirs()
            assertTrue(new File("/tmp/otp/mdPath").isDirectory())
        }
        dataPath = new File("/tmp/otp/dataPath")
        mdPath = new File("/tmp/otp/mdPath")
    }

    @After
    void tearDown() {
        dataPath.deleteDir()
        mdPath.deleteDir()
    }

    @Test
    void testRegisterInputFiles() {
        Run run = new Run()
        assertFalse(run.validate())
        run.name = "testRun"
        run.complete = false
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run.seqCenter = seqCenter
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
        run.seqPlatform = seqPlatform
        assert(run.save())
        metaDataRegistrationService.registerInputFiles(run.id)
        new File(mdPath.absolutePath + "/runtestRun").mkdir()
        File runtestRun = new File(mdPath.absolutePath + "/runtestRun")
        metaDataRegistrationService.registerInputFiles(run.id)
        FileType fileType = new FileType(type: FileType.Type.SOURCE)
        assert(fileType.save())
        Run run2 = new Run()
        assertFalse(run2.validate())
        run2.name = "testRun2"
        run2.complete = false
        run2.seqCenter = seqCenter
        run2.seqPlatform = seqPlatform
        run2.validate()
        println(run2.errors)
        assert(run2.save())
        // should work
        metaDataRegistrationService.registerInputFiles(run2.id)
    }
}
