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
        List<String> paths = [
            "./target/otp/dataPath",
            "./target/otp/mdPath"
        ]
        for(String path in paths) {
            assertDirectoryExists(path)
        }
        dataPath = new File(paths.get(0))
        mdPath = new File(paths.get(1))
    }

    private void assertDirectoryExists(String path) {
        File file = new File(path)
        if (!file.isDirectory()) {
            file.mkdirs()
            assertTrue(file.isDirectory())
        }
    }

    @After
    void tearDown() {
        dataPath.deleteDir()
        mdPath.deleteDir()
    }

    @Ignore
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
