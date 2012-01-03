package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*

class FilesCompletenessServiceTests {


    def filesCompletenessService

    File dataPath
    File mdPath

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

    void tearDown() {
        dataPath.deleteDir()
        mdPath.deleteDir()
    }

    @Test
    void testCheckInitialSequenceFiles() {
        // the Run to be checked
        Run run = new Run()
        assertFalse(run.validate())
        run.name = "testRun"
        run.complete = false
        run.dataPath = dataPath
        run.mdPath = mdPath
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run.seqCenter = seqCenter
        SeqTech seqTech = new SeqTech(name: "testSolid")
        assert(seqTech.save())
        run.seqTech = seqTech
        assert(run.save())
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Project project = new Project(name: "testProject", dirName: "testDir", host: "lokalhorst")
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SeqTrack seqTrack = new SeqTrack(laneId: "testLaneId", run: run, seqType: seqType, seqTech: seqTech, sample: sample)
        assert(seqTrack.save())
        // one of the two file types leading to full check
        FileType fileType = new FileType(type: FileType.Type.ALIGNMENT)
        assert(fileType.save())
        // three data files (of which two are associated with the run) to be able to loop over more than one data file
        DataFile dataFile1 = new DataFile(fileName: "dataFile1", pathName: "testPath", run: run, fileType: fileType ,seqTrack: seqTrack)
        assert(dataFile1.save())
        DataFile dataFile2 = new DataFile(fileName: "dataFile2", pathName: "testPath", run: run, fileType: fileType)
        assert(dataFile2.save())
        DataFile dataFile3 = new DataFile(fileName: "dataFile3", pathName: "testPath", run: null, fileType: fileType)
        assert(dataFile3.save())
        // the complete paths to be checked have to be created
        new File(dataPath.absolutePath + "/run" + run.name + "/" + dataFile1.pathName + "/" + "/" + dataFile1.fileName).mkdirs()
        new File(dataPath.absolutePath + "/run" + run.name + "/" + dataFile2.pathName + "/" + "/" + dataFile2.fileName).mkdirs()
        new File(dataPath.absolutePath + "/run" + run.name + "/" + dataFile3.pathName + "/" + "/" + dataFile3.fileName).mkdirs()
        // call the service method and check whether it returns true
        assertTrue(filesCompletenessService.checkInitialSequenceFiles(run))
    }
}
