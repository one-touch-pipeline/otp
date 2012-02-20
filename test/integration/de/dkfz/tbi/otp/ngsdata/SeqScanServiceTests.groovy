package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import java.io.File;

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*


class SeqScanServiceTests {

    def seqScanService
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

    @Test
    void testBuildSeqScans() {
        Run run = new Run()
        assertFalse(run.validate())
        run.name = "testRun"
        run.complete = false
        run.dataPath = dataPath
        run.mdPath = mdPath
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run.seqCenter = seqCenter
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
        run.seqPlatform = seqPlatform
        assert(run.save())
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz")
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SeqTrack seqTrack = new SeqTrack(laneId: "testLaneId", pipelineVersion: "2", run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack.save())
        SeqScan seqScan = new SeqScan(sample: sample, seqType: seqType, seqPlatform: seqPlatform)
        assert(seqScan.save())
        // indicates that seqTrack is used
        MergingAssignment mergingAssignment = new MergingAssignment(seqScan: seqScan, seqTrack: seqTrack)
        assert(mergingAssignment.save())
        // already used seqTrack
        seqScanService.buildSeqScans()
        // unused seqTrack triggers processing
        SeqTrack seqTrack2 = new SeqTrack(laneId: "testLaneId2", pipelineVersion: "2", run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack2.save())
        seqScanService.buildSeqScans()
    }

    @Test
    void testBuildSeqScan() {
        Run run = new Run()
        assertFalse(run.validate())
        run.name = "testRun"
        run.complete = false
        run.dataPath = dataPath
        run.mdPath = mdPath
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run.seqCenter = seqCenter
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
        run.seqPlatform = seqPlatform
        assert(run.save())
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz")
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SeqTrack seqTrack = new SeqTrack(laneId: "testLaneId", pipelineVersion: "2", run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack.save())
        SeqScan seqScan = new SeqScan(sample: sample, seqType: seqType, seqPlatform: seqPlatform)
        assert(seqScan.save())
        // indicates that seqTrack is used
        MergingAssignment mergingAssignment = new MergingAssignment(seqScan: seqScan, seqTrack: seqTrack)
        assert(mergingAssignment.save())
        // already used seqTrack
        seqScanService.buildSeqScan(seqTrack)
        SeqTrack seqTrack2 = new SeqTrack(laneId: "testLaneId2", pipelineVersion: "2", run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack2.save())
        // unused seqTrack triggers processing
        seqScanService.buildSeqScan(seqTrack2)
    }

    @Test
    void testBuildSeqScanWithReturningSeqScan() {
        Run run = new Run()
        assertFalse(run.validate())
        run.name = "testRun"
        run.complete = false
        run.dataPath = dataPath
        run.mdPath = mdPath
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run.seqCenter = seqCenter
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
        run.seqPlatform = seqPlatform
        assert(run.save())
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz")
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SeqTrack seqTrack = new SeqTrack(laneId: "testLaneId", pipelineVersion: "2", run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack.save())
        AlignmentParams alignmentParams = new AlignmentParams(programName: "testProgram")
        assert(alignmentParams.save())
        SeqScan seqScan = seqScanService.buildSeqScan([seqTrack], alignmentParams)
        assertEquals(individual.mockFullName, seqScan.sample.individual.mockFullName)
        assertEquals(sample.type, seqScan.sample.type)
        assertEquals(seqType.name, seqScan.seqType.name)
        assertEquals(seqType.libraryLayout, seqScan.seqType.libraryLayout)
    }

    @Test
    void testFillSeqScan() {
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz")
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
        SeqScan seqScan = new SeqScan(sample: sample, seqType: seqType, seqCenters: "testCenters", insertSize: "7", nLanes: 4, nBasePairs: 9l, coverage: 8.8, state: SeqScan.State.PROCESSING, qcState: SeqScan.QCState.PASS, seqPlatform: seqPlatform)
        assert(seqScan.save())
        seqScanService.fillSeqCenters(seqScan)
        assertEquals(4, seqScan.nLanes)
        assertEquals(9, seqScan.nBasePairs)
    }

    @Test
    void testFillInsertSize() {
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz")
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
        SeqScan seqScan = new SeqScan(sample: sample, seqType: seqType, seqCenters: "testCenters", insertSize: "7", nLanes: 4, nBasePairs: 9l, coverage: 8.8, state: SeqScan.State.PROCESSING, qcState: SeqScan.QCState.PASS, seqPlatform: seqPlatform)
        assert(seqScan.save())
        Run run = new Run()
        assertFalse(run.validate())
        run.name = "testRun"
        run.complete = false
        run.dataPath = dataPath
        run.mdPath = mdPath
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        assert(run.save())
        SeqTrack seqTrack = new SeqTrack(laneId: "testLaneId", pipelineVersion: "2", run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample, insertSize: "42")
        assert(seqTrack.save())
        MergingAssignment mergingAssignment = new MergingAssignment(seqScan: seqScan, seqTrack: seqTrack)
        assert(mergingAssignment.save())
        seqScanService.fillInsertSize(seqScan)
        assertEquals(seqTrack.insertSize.toString(), seqScan.insertSize)
    }
}
