package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import de.dkfz.tbi.otp.ngsdata.RunInitialPath

class FilesCompletenessServiceTests extends AbstractIntegrationTest {


    def filesCompletenessService
    def grailsApplication

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

    @Ignore
    @Test
    void testCheckInitialSequenceFiles() {
        // the Run to be checked
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
        // when no data file is associated it should always throw exception
        shouldFail(ProcessingException) { filesCompletenessService.checkInitialSequenceFiles(run) }
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Realm realm = new Realm(name: "test", rootPath: "/", webHost: "http://test.me", host: "127.0.0.1", port: 12345, unixUser: "test", timeout: 100, pbsOptions: "")
        assertNotNull(realm.save())
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz", realm: realm)
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SoftwareTool softwareTool = new SoftwareTool(programName: "testProgram", type: SoftwareTool.Type.BASECALLING)
        assert(softwareTool.save())
        SeqTrack seqTrack = new SeqTrack(laneId: "testLaneId", pipelineVersion: softwareTool, run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
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
        new File(dataPath.absolutePath + "/run" + run.name + "/" + dataFile1.pathName + "/" + dataFile1.fileName).mkdirs()
        new File(dataPath.absolutePath + "/run" + run.name + "/" + dataFile2.pathName + "/" + dataFile2.fileName).mkdirs()
        new File(dataPath.absolutePath + "/run" + run.name + "/" + dataFile3.pathName + "/" + dataFile3.fileName).mkdirs()
        //  Cannot get property 'dataPath' on null object
        shouldFail(NullPointerException) {
            filesCompletenessService.checkInitialSequenceFiles(run)
        }
        dataFile3.run = run
        assert(dataFile3.save())
        // Cannot get property 'dataPath' on null object
        shouldFail(NullPointerException) {
            filesCompletenessService.checkInitialSequenceFiles(run)
        }
        RunInitialPath runInitialPath = new RunInitialPath(dataPath: dataPath.absolutePath, mdPath: mdPath.absolutePath, run: run)
        assert(runInitialPath.save())
        dataFile1.runInitialPath = runInitialPath
        dataFile2.runInitialPath = runInitialPath
        dataFile3.runInitialPath = runInitialPath
        // call the service method and check whether it returns true
        assertTrue(filesCompletenessService.checkInitialSequenceFiles(run))
    }

    @Ignore
    @Test
    void testCheckFinalLocation() {
        // the Run to be checked
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
        // when no data file is associated it should always throw exception
        shouldFail(ProcessingException) { filesCompletenessService.checkFinalLocation(run) }
        SeqType seqType1 = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir1")
        assert(seqType1.save())
        SeqType seqType2 = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir2")
        assert(seqType2.save())
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Realm realm = new Realm(name: "test", rootPath: "/", webHost: "http://test.me", host: "127.0.0.1", port: 12345, unixUser: "test", timeout: 100, pbsOptions: "")
        assertNotNull(realm.save())
        Project project = new Project(name: "testProject", dirName: "testProjectDir", host: "dkfz", realm: realm)
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SoftwareTool softwareTool = new SoftwareTool(programName: "testProgram", type: SoftwareTool.Type.BASECALLING)
        assert(softwareTool.save())
        SeqTrack seqTrack1 = new SeqTrack(laneId: "testLaneId", pipelineVersion: softwareTool, run: run, seqType: seqType1, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack1.save())
        SeqTrack seqTrack2 = new SeqTrack(laneId: "testLaneId", pipelineVersion: softwareTool, run: run, seqType: seqType2, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack2.save())
        // one of the two file types leading to full check
        FileType fileType = new FileType(type: FileType.Type.ALIGNMENT)
        assert(fileType.save())
        // three data files (of which two are associated with the run) to be able to loop over more than one data file
        DataFile dataFile1 = new DataFile(fileName: "dataFile1", pathName: "testPath1", used: true, vbpFilePath: "testPath1", run: run, fileType: fileType, seqTrack: seqTrack1, project: project)
        assert(dataFile1.save())
        DataFile dataFile2 = new DataFile(fileName: "dataFile2", pathName: "testPath2", used: true, vbpFilePath: "testPath2", run: run, fileType: fileType, seqTrack: seqTrack2, project: project)
        assert(dataFile2.save())
        // not used for the run due to run set to null
        DataFile dataFile3 = new DataFile(fileName: "dataFile3", pathName: "testPath3", run: null, fileType: fileType)
        assert(dataFile3.save())
        // the complete paths to be checked have to be created
        String tmpPath1 = dataPath.absolutePath + "/" + project.dirName + "/sequencing/" + seqType1.dirName + "/" + seqCenter.dirName + "/run" + run.name + "/" + dataFile1.vbpFilePath + "/" + dataFile1.fileName
        String tmpPath2 = dataPath.absolutePath + "/" + project.dirName + "/sequencing/" + seqType2.dirName + "/" + seqCenter.dirName + "/run" + run.name + "/" + dataFile2.vbpFilePath + "/" + dataFile2.fileName
        new File(tmpPath1).mkdirs()
        new File(tmpPath2).mkdirs()
        // fake dataPath normally read from configuration
        grailsApplication.config.otp.dataPath.dkfz = "/tmp/otp/dataPath/"
        // call the service method and check whether it returns true
        assertTrue(filesCompletenessService.checkFinalLocation(run))
    }

    @Ignore
    @Test
    void testCheckViewByPid() {
        // the Run to be checked
        Run run1 = new Run()
        assertFalse(run1.validate())
        run1.name = "testRun1"
        run1.complete = false
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run1.seqCenter = seqCenter
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
        run1.seqPlatform = seqPlatform
        assert(run1.save())
        // when no data files are associated it should always throw exception
        shouldFail(ProcessingException) { filesCompletenessService.checkViewByPid(run1) }
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Realm realm = new Realm(name: "test", rootPath: "/", webHost: "http://test.me", host: "127.0.0.1", port: 12345, unixUser: "test", timeout: 100, pbsOptions: "")
        assertNotNull(realm.save())
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz", realm: realm)
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SoftwareTool softwareTool = new SoftwareTool(programName: "testProgram", type: SoftwareTool.Type.BASECALLING)
        assert(softwareTool.save())
        Run run = new Run()
        assertFalse(run.validate())
        run.name = "testRun"
        run.complete = false
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        assert(run.save())
        SeqTrack seqTrack1 = new SeqTrack(laneId: "testLaneId", pipelineVersion: softwareTool, run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack1.save())
        FileType fileType = new FileType(type: FileType.Type.ALIGNMENT, vbpPath: "vbpTestPath")
        assert(fileType.save())
        // a data file with an associated run but not all needed variables and keys set
        DataFile dataFile3 = new DataFile(fileName: "dataFile3", pathName: "testPath", used: true, run: run1, fileType: fileType)
        assert(dataFile3.save())
        // seqTrack not associated
        shouldFail(NullPointerException) {
            filesCompletenessService.checkViewByPid(run1)
        }
        // make second run with correct associations
        Run run2 = new Run(name: "testRun2", complete: false, seqCenter: seqCenter, seqPlatform: seqPlatform)
        assert(run2.save())
        SeqTrack seqTrack2 = new SeqTrack(laneId: "testLaneId", pipelineVersion: softwareTool, run: run2, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack2.save())
        // two correct data files to be able to loop over more than one data file
        DataFile dataFile1 = new DataFile(fileName: "dataFile1", pathName: "testPath", used: true, vbpFileName: "vbpTestFileName", run: run2, fileType: fileType, seqTrack: seqTrack2, project: project)
        assert(dataFile1.save())
        DataFile dataFile2 = new DataFile(fileName: "dataFile2", pathName: "testPath", used: true, vbpFileName: "vbpTestFileName", run: run2, fileType: fileType, seqTrack: seqTrack2, project: project)
        assert(dataFile2.save())
        // the complete paths to be checked have to be created
        String pid = seqTrack2.sample.individual.pid
        String tmpPath1 = dataPath.absolutePath + "/" + dataFile1.project?.dirName + "/sequencing/" + seqTrack2.seqType.dirName +  "/view-by-pid/" + pid + "/" + seqTrack2.sample.type.toString().toLowerCase() + "/" + seqTrack2.seqType.libraryLayout.toLowerCase() + "/run" + dataFile1.run.name + "/" + dataFile1.fileType.vbpPath + "/" + dataFile1.vbpFileName
        String tmpPath2 = dataPath.absolutePath + "/" + dataFile2.project?.dirName + "/sequencing/" + seqTrack2.seqType.dirName +  "/view-by-pid/" + pid + "/" + seqTrack2.sample.type.toString().toLowerCase() + "/" + seqTrack2.seqType.libraryLayout.toLowerCase() + "/run" + dataFile2.run.name + "/" + dataFile2.fileType.vbpPath + "/" + dataFile2.vbpFileName
        new File(tmpPath1).mkdirs()
        new File(tmpPath2).mkdirs()
        // call the service method and check whether it returns true
        assertTrue(filesCompletenessService.checkViewByPid(run2))
    }

    @Test
    void testCheckAllFiles() {
        long senseLess = 123456789L
        // the handed over 'id' does not have a Run associated
        shouldFail(ProcessingException) { filesCompletenessService.checkAllFiles(senseLess) }
        // the Run to be checked
        Run run1 = new Run()
        assertFalse(run1.validate())
        run1.name = "testRun1"
        run1.complete = false
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run1.seqCenter = seqCenter
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
        run1.seqPlatform = seqPlatform
        assert(run1.save())
        // when no data files are associated it should always throw exception
        shouldFail(ProcessingException) {
            filesCompletenessService.checkAllFiles(run1.id)
        }
    }

    @Test
    void testCheckAllRuns() {
        shouldFail(ProcessingException) {
            filesCompletenessService.checkAllRuns(null, "bla")
        }
        Realm realm = new Realm(name: "test", rootPath: "/", webHost: "http://test.me", host: "127.0.0.1", port: 12345, unixUser: "test", timeout: 100, pbsOptions: "")
        assertNotNull(realm.save())
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz", realm: realm)
        assert(project.save())
        // fake dataPath normally read from configuration
        grailsApplication.config.otp.dataPath.dkfz = "/tmp/otp/dataPath/"
        shouldFail(ProcessingException) {
            filesCompletenessService.checkAllRuns(project.name, "bla")
        }
        shouldFail(ProcessingException) {
            filesCompletenessService.checkAllRuns(project.name, null)
        }
        // ProcessingException: No sequencing directories could be found.
        shouldFail(ProcessingException) {
            filesCompletenessService.checkAllRuns(project.name, "dkfz")
        }
        // first part of temp path used for all paths
        String tmpPath = dataPath.absolutePath + "/" + project.dirName + "/sequencing"
        new File(tmpPath).mkdirs()
        // the seqTypeDirs
        new File(tmpPath + "/seqTypeDir1").mkdir()
        new File(tmpPath + "/seqTypeDir2").mkdir()
        new File(tmpPath + "/seqTypeDir3").mkdir()
        new File(tmpPath + "/seqTypeDir4").mkdir()
        new File(tmpPath + "/seqTypeDir5").mkdir()
        // the seqTypes
        SeqType seqType1 = new SeqType(name: "testSeqType1", libraryLayout: "testLibraryLayout1", dirName: "seqTypeDir1")
        assert(seqType1.save())
        SeqType seqType2 = new SeqType(name: "testSeqType2", libraryLayout: "testLibraryLayout2", dirName: "seqTypeDir2")
        assert(seqType2.save())
        SeqType seqType3 = new SeqType(name: "testSeqType3", libraryLayout: "testLibraryLayout3", dirName: "seqTypeDir3")
        assert(seqType3.save())
        SeqType seqType4 = new SeqType(name: "testSeqType4", libraryLayout: "testLibraryLayout4", dirName: "seqTypeDir4")
        assert(seqType4.save())
        SeqType seqType5 = new SeqType(name: "testSeqType5", libraryLayout: "testLibraryLayout5", dirName: "seqTypeDir5")
        assert(seqType5.save())
        // the seqCenterDirs
        new File(tmpPath + "/seqTypeDir1" + "/seqCenterDir1").mkdir()
        new File(tmpPath + "/seqTypeDir2" + "/seqCenterDir2").mkdir()
        new File(tmpPath + "/seqTypeDir3" + "/seqCenterDir3").mkdir()
        new File(tmpPath + "/seqTypeDir4" + "/seqCenterDir4").mkdir()
        new File(tmpPath + "/seqTypeDir5" + "/seqCenterDir5").mkdir()
        // the seqCenters
        SeqCenter seqCenter1 = new SeqCenter(name: "testSeqCenter1", dirName: "seqCenterDir1")
        assert(seqCenter1.save())
        SeqCenter seqCenter2 = new SeqCenter(name: "testSeqCenter2", dirName: "seqCenterDir2")
        assert(seqCenter2.save())
        SeqCenter seqCenter3 = new SeqCenter(name: "testSeqCenter3", dirName: "seqCenterDir3")
        assert(seqCenter3.save())
        SeqCenter seqCenter4 = new SeqCenter(name: "testSeqCenter4", dirName: "seqCenterDir4")
        assert(seqCenter4.save())
        SeqCenter seqCenter5 = new SeqCenter(name: "testSeqCenter5", dirName: "seqCenterDir5")
        assert(seqCenter5.save())
        // the runDirs
        new File(tmpPath + "/seqTypeDir1" + "/seqCenterDir1" + "/runtestRun1").mkdir()
        new File(tmpPath + "/seqTypeDir2" + "/seqCenterDir2" + "/runtestRun2").mkdir()
        new File(tmpPath + "/seqTypeDir3" + "/seqCenterDir3" + "/runtestRun3").mkdir()
        new File(tmpPath + "/seqTypeDir4" + "/seqCenterDir4" + "/runtestRun4").mkdir()
        new File(tmpPath + "/seqTypeDir5" + "/seqCenterDir5" + "/runtestRun5").mkdir()
        // the seqTechs
        SeqPlatform seqPlatform1 = new SeqPlatform(name: "testSolid1")
        assert(seqPlatform1.save())
        SeqPlatform seqPlatform2 = new SeqPlatform(name: "testSolid2")
        assert(seqPlatform2.save())
        SeqPlatform seqPlatform3 = new SeqPlatform(name: "testSolid3")
        assert(seqPlatform3.save())
        SeqPlatform seqPlatform4 = new SeqPlatform(name: "testSolid4")
        assert(seqPlatform4.save())
        SeqPlatform seqPlatform5 = new SeqPlatform(name: "testSolid5")
        assert(seqPlatform5.save())
        // the runs
        Run run1 = new Run(name: "testRun1", complete: false, seqCenter: seqCenter1, seqPlatform: seqPlatform1)
        assert(run1.save())
        Run run2 = new Run(name: "testRun2", complete: false, seqCenter: seqCenter2, seqPlatform: seqPlatform2)
        assert(run2.save())
        Run run3 = new Run(name: "testRun3", complete: false, seqCenter: seqCenter3, seqPlatform: seqPlatform3)
        assert(run3.save())
        Run run4 = new Run(name: "testRun4", complete: false, seqCenter: seqCenter4, seqPlatform: seqPlatform4)
        assert(run4.save())
        Run run5 = new Run(name: "testRun5", complete: false, seqCenter: seqCenter5, seqPlatform: seqPlatform5)
        assert(run5.save())
        // should correctly and completely run and therefore return true
        assertTrue(filesCompletenessService.checkAllRuns(project.name, "dkfz"))
    }
}
