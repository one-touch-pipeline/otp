package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class RunServiceTests extends AbstractIntegrationTest {
    def runService

    void testGetRunWithoutRun() {
        assertNull(runService.getRun(null))
        assertNull(runService.getRun(""))
        assertNull(runService.getRun(0))
        assertNull(runService.getRun("test"))
    }

    void testGetRunByIdentifier() {
        Run run = mockRun("test")
        assertEquals(run, runService.getRun(run.id))
        assertEquals(run, runService.getRun("${run.id}"))
    }

    void testGetRunByName() {
        Run run = mockRun("test")
        assertEquals(run, runService.getRun("test"))
    }

    void testGetRunByNameAsIdentifier() {
        Run run = mockRun("2")
        assertEquals(run, runService.getRun("2"))
        run.name = run.id + 1
        assertNotNull(run.save())
        assertEquals(run, runService.getRun(run.name))
        Run run2 = new Run(name: "foo", seqCenter: SeqCenter.list().first(), seqPlatform: SeqPlatform.list().first())
        assertNotNull(run2.save())
        assertEquals(run2, runService.getRun(run.name))
    }

    void testRetrieveProcessParameterEmpty() {
        assertTrue(runService.retrieveProcessParameters(null).isEmpty())
        Run run = mockRun("test")
        assertTrue(runService.retrieveProcessParameters(run).isEmpty())
    }

    void testRetrieveProcessParameter() {
        // create JobExecutionPlan
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())

        // create Run
        Run run = mockRun("test")

        ProcessParameter param = new ProcessParameter(value: run.id, className: Run.class.name, process: process)
        assertNotNull(param.save())

        assertEquals(1, runService.retrieveProcessParameters(run).size())
        assertEquals(param, runService.retrieveProcessParameters(run).first())

        // create second Process
        Process process2 = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process2.save())
        ProcessParameter param2 = new ProcessParameter(value: run.id, className: Run.class.name, process: process2)
        assertNotNull(param2.save())

        assertEquals(2, runService.retrieveProcessParameters(run).size())
        assertEquals(param, runService.retrieveProcessParameters(run).first())
        assertEquals(param2, runService.retrieveProcessParameters(run).last())
    }

    @Ignore
    void testPreviousRun() {
        // no Run yet, should be null
        assertNull(runService.previousRun(null))
        // create a run
        Run run = mockRun("test")
        // no previous run yet
        assertNull(runService.previousRun(run))
        // create another run
        Run run2 = new Run(name: "test2", seqCenter: SeqCenter.list().first(), seqPlatform: SeqPlatform.list().first())
        assertNotNull(run2.save())
        assertNull(runService.previousRun(run))
        assertEquals(run, runService.previousRun(run2))
        // create a third run
        Run run3 = new Run(name: "test3", seqCenter: SeqCenter.list().first(), seqPlatform: SeqPlatform.list().first())
        assertNotNull(run3.save())
        assertNull(runService.previousRun(run))
        assertEquals(run, runService.previousRun(run2))
        assertEquals(run2, runService.previousRun(run3))
    }

    void testNextRun() {
        // no Run yet, should be null
        assertNull(runService.nextRun(null))
        // create a run
        Run run = mockRun("test")
        // no previous run yet
        assertNull(runService.nextRun(run))
        // create another run
        Run run2 = new Run(name: "test2", seqCenter: SeqCenter.list().first(), seqPlatform: SeqPlatform.list().first())
        assertNotNull(run2.save())
        assertEquals(run2, runService.nextRun(run))
        assertNull(runService.nextRun(run2))
        // create a third run
        Run run3 = new Run(name: "test3", seqCenter: SeqCenter.list().first(), seqPlatform: SeqPlatform.list().first())
        assertNotNull(run3.save())
        assertEquals(run2, runService.nextRun(run))
        assertEquals(run3, runService.nextRun(run2))
        assertNull(runService.nextRun(run3))
    }

    @Ignore
    void testMDFilesByInitialPathIsEmpty() {
        assertTrue(runService.retrieveMetaDataFilesByInitialPath(null).empty)
        Run run = mockRun("test")
        assertTrue(runService.retrieveMetaDataFilesByInitialPath(run).empty)
        RunSegment segment1 = mockRunSegment(run, "test")
        assertTrue(runService.retrieveMetaDataFilesByInitialPath(run).empty)
        RunSegment segment2 = mockRunSegment(run, "test2")
        RunSegment segment3 = mockRunSegment(run, "test3")
        assertTrue(runService.retrieveMetaDataFilesByInitialPath(run).empty)
        // create a second Run and add a MetaDataFile to it
        Run run2 = mockRun("test2")
        RunSegment segment4 = mockRunSegment(run2, "test")
        MetaDataFile md = mockMetaDataFile(segment4, "test")
        assertTrue(runService.retrieveMetaDataFilesByInitialPath(run).empty)
    }

    @Ignore
    void testMDFilesByInitialPath() {
        Run run = mockRun("test")
        RunSegment segment1 = mockRunSegment(run, "test")
        MetaDataFile md = mockMetaDataFile(segment1, "test")
        List<MetaDataFile> files = runService.retrieveMetaDataFilesByInitialPath(run)
        assertFalse(files.empty)
        assertEquals(1, files.size())
        assertSame(md, files[0])
        // add second file
        MetaDataFile md2 = mockMetaDataFile(segment1, "test2")
        files = runService.retrieveMetaDataFilesByInitialPath(run)
        assertFalse(files.empty)
        assertEquals(2, files.size())
        assertSame(md, files[0])
        assertSame(md2, files[1])
        // add a second RunSegment
        RunSegment segment2 = mockRunSegment(run, "test2")
        MetaDataFile md3 = mockMetaDataFile(segment2, "test3")
        MetaDataFile md4 = mockMetaDataFile(segment2, "test4")
        files = runService.retrieveMetaDataFilesByInitialPath(run)
        assertFalse(files.empty)
        assertEquals(4, files.size())
        assertSame(md, files[0])
        assertSame(md2, files[1])
        assertSame(md3, files[2])
        assertSame(md4, files[3])

        // add a second Run and attach files
        Run run2 = mockRun("test2")
        RunSegment segment3 = mockRunSegment(run2, "test3")
        MetaDataFile md5 = mockMetaDataFile(segment3, "test5")
        MetaDataFile md6 = mockMetaDataFile(segment3, "test6")
        // should be unchanged
        files = runService.retrieveMetaDataFilesByInitialPath(run)
        assertFalse(files.empty)
        assertEquals(4, files.size())
        assertSame(md, files[0])
        assertSame(md2, files[1])
        assertSame(md3, files[2])
        assertSame(md4, files[3])
        // and for run2
        files = runService.retrieveMetaDataFilesByInitialPath(run2)
        assertFalse(files.empty)
        assertEquals(2, files.size())
        assertSame(md5, files[0])
        assertSame(md6, files[1])
    }

    @Ignore
    void testRetrieveSequenceTrackInformationIsEmpty() {
        assertTrue(runService.retrieveSequenceTrackInformation(null).isEmpty())
        Run run = mockRun("test")
        assertTrue(runService.retrieveSequenceTrackInformation(run).isEmpty())
        SeqTrack seqTrack = mockSeqTrack(run, "test", "test")
        Map data = runService.retrieveSequenceTrackInformation(run)
        assertFalse(data.isEmpty())
        assertEquals(1, data.size())
        assertTrue(data.containsKey(seqTrack))
        Map seqTrackData = data.get(seqTrack)
        assertTrue(seqTrackData.files.isEmpty())
        assertTrue(seqTrackData.alignments.isEmpty())
        // create a second seq track
        SeqTrack seqTrack2 = mockSeqTrack(run, "test", "test2")
        data = runService.retrieveSequenceTrackInformation(run)
        assertFalse(data.isEmpty())
        assertEquals(2, data.size())
        assertTrue(data.containsKey(seqTrack))
        assertTrue(data.containsKey(seqTrack2))
        seqTrackData = data.get(seqTrack)
        assertTrue(seqTrackData.files.isEmpty())
        assertTrue(seqTrackData.alignments.isEmpty())
        seqTrackData = data.get(seqTrack2)
        assertTrue(seqTrackData.files.isEmpty())
        assertTrue(seqTrackData.alignments.isEmpty())
        // create another seq track for a different run
        Run run2 = mockRun("test2")
        SeqTrack seqTrack3 = mockSeqTrack(run2, "test2", "test3")
        data = runService.retrieveSequenceTrackInformation(run)
        assertFalse(data.isEmpty())
        assertEquals(2, data.size())
        assertTrue(data.containsKey(seqTrack))
        assertTrue(data.containsKey(seqTrack2))
        data = runService.retrieveSequenceTrackInformation(run2)
        assertFalse(data.isEmpty())
        assertEquals(1, data.size())
        assertTrue(data.containsKey(seqTrack3))
    }

    @Ignore
    void testRetrieveSequenceTrackInformation() {
        Run run = mockRun("test")
        SeqTrack seqTrack = mockSeqTrack(run, "test", "test")
        SeqTrack seqTrack2 = mockSeqTrack(run, "test", "test2")
        DataFile dataFile1 = new DataFile(run: run, seqTrack: seqTrack)
        assertNotNull(dataFile1.save())
        DataFile dataFile2 = new DataFile(run: run, seqTrack: seqTrack)
        assertNotNull(dataFile2.save())
        DataFile dataFile3 = new DataFile(run: run, seqTrack: seqTrack2)
        assertNotNull(dataFile3.save())
        Map data = runService.retrieveSequenceTrackInformation(run)
        assertFalse(data.isEmpty())
        assertEquals(2, data.size())
        assertTrue(data.containsKey(seqTrack))
        assertTrue(data.containsKey(seqTrack2))
        Map seqTrackData = data.get(seqTrack)
        assertTrue(seqTrackData.alignments.isEmpty())
        assertFalse(seqTrackData.files.isEmpty())
        assertEquals(2, seqTrackData.files.size())
        assertSame(dataFile1, seqTrackData.files[0])
        assertSame(dataFile2, seqTrackData.files[1])
        seqTrackData = data.get(seqTrack2)
        assertTrue(seqTrackData.alignments.isEmpty())
        assertFalse(seqTrackData.files.isEmpty())
        assertEquals(1, seqTrackData.files.size())
        assertSame(dataFile3, seqTrackData.files[0])
        // second run
        Run run2 = mockRun("test2")
        SeqTrack seqTrack3 = mockSeqTrack(run2, "test2", "test3")
        // create an AlignmentLog
        AlignmentLog log = mockAlignmentLog(seqTrack)
        AlignmentLog log2 = mockAlignmentLog(seqTrack2)
        AlignmentLog log3 = mockAlignmentLog(seqTrack3)
        data = runService.retrieveSequenceTrackInformation(run)
        // data file information unchanged
        seqTrackData = data.get(seqTrack)
        assertFalse(seqTrackData.files.isEmpty())
        assertEquals(2, seqTrackData.files.size())
        assertSame(dataFile1, seqTrackData.files[0])
        assertSame(dataFile2, seqTrackData.files[1])
        // alignment data is changed
        assertFalse(seqTrackData.alignments.isEmpty())
        assertEquals(1, seqTrackData.alignments.size())
        assertTrue(seqTrackData.alignments.containsKey(log))
        assertFalse(seqTrackData.alignments.containsKey(log2))
        assertFalse(seqTrackData.alignments.containsKey(log3))
        assertTrue(seqTrackData.alignments.get(log).isEmpty())
        // second log
        seqTrackData = data.get(seqTrack2)
        assertFalse(seqTrackData.alignments.isEmpty())
        assertEquals(1, seqTrackData.alignments.size())
        assertFalse(seqTrackData.alignments.containsKey(log))
        assertTrue(seqTrackData.alignments.containsKey(log2))
        assertFalse(seqTrackData.alignments.containsKey(log3))
        assertTrue(seqTrackData.alignments.get(log2).isEmpty())

        // now lets attach DataFiles to the AlignmentLog
        dataFile1.alignmentLog = log
        assertNotNull(dataFile1.save())
        dataFile2.alignmentLog = log
        assertNotNull(dataFile1.save())
        data = runService.retrieveSequenceTrackInformation(run)
        // data file information unchanged
        seqTrackData = data.get(seqTrack)
        assertFalse(seqTrackData.files.isEmpty())
        assertEquals(2, seqTrackData.files.size())
        assertSame(dataFile1, seqTrackData.files[0])
        assertSame(dataFile2, seqTrackData.files[1])
        // alignment data is changed
        assertFalse(seqTrackData.alignments.isEmpty())
        assertEquals(1, seqTrackData.alignments.size())
        assertTrue(seqTrackData.alignments.containsKey(log))
        assertFalse(seqTrackData.alignments.containsKey(log2))
        assertFalse(seqTrackData.alignments.containsKey(log3))
        assertFalse(seqTrackData.alignments.get(log).isEmpty())
        assertEquals(2, seqTrackData.alignments.get(log).size())
        assertSame(dataFile1, seqTrackData.alignments.get(log)[0])
        assertSame(dataFile2, seqTrackData.alignments.get(log)[1])
        // second log
        seqTrackData = data.get(seqTrack2)
        assertFalse(seqTrackData.alignments.isEmpty())
        assertEquals(1, seqTrackData.alignments.size())
        assertFalse(seqTrackData.alignments.containsKey(log))
        assertTrue(seqTrackData.alignments.containsKey(log2))
        assertFalse(seqTrackData.alignments.containsKey(log3))
        assertTrue(seqTrackData.alignments.get(log2).isEmpty())
    }

    private Run mockRun(String name) {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = SeqCenter.findOrCreateByNameAndDirName("test", "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: name, seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())
        return run
    }

    private RunSegment mockRunSegment(Run run, String path) {
        RunSegment segment = new RunSegment(run: run, dataPath: path, mdPath: "${path}.md5")
        assertNotNull(segment.save())
        return segment
    }

    private MetaDataFile mockMetaDataFile(RunSegment segment, String name) {
        MetaDataFile md = new MetaDataFile(RunSegment: segment, fileName: name, filePath: name, dateCreated: new Date())
        assertNotNull(md.save())
        return md
    }

    private SeqTrack mockSeqTrack(Run run, String pid, String laneId) {
        Realm realm = Realm.findOrCreateByNameAndRootPathAndWebHostAndHostAndPortAndUnixUserAndTimeoutAndPbsOptions("test", "/", "http://localhost", "pbs", 1234, "test", 0, "")
        assertNotNull(realm.save())
        Project project = Project.findOrCreateByNameAndDirNameAndRealm("test", "test", realm)
        assertNotNull(project.save())
        Individual individual = Individual.findOrCreateByPidAndMockPidAndMockFullNameAndTypeAndProject(pid, pid, pid, Individual.Type.UNDEFINED, project)
        assertNotNull(individual.save())
        Sample sample = Sample.findOrCreateByIndividualAndType(individual, Sample.Type.UNKNOWN)
        assertNotNull(sample.save())
        SeqType seqType = SeqType.findOrCreateByNameAndDirNameAndLibraryLayout("test", "test", "test")
        assertNotNull(seqType.save())
        SoftwareTool software = SoftwareTool.findOrCreateByProgramNameAndType("test", SoftwareTool.Type.ALIGNMENT)
        assertNotNull(software.save())
        SeqTrack seqTrack = new SeqTrack(run: run, sample: sample, seqType: seqType, seqPlatform: run.seqPlatform, pipelineVersion: software, laneId: laneId)
        assertNotNull(seqTrack.save())
        return seqTrack
    }

    private AlignmentLog mockAlignmentLog(SeqTrack seqTrack) {
        SoftwareTool software = SoftwareTool.findOrCreateByProgramNameAndType("test", SoftwareTool.Type.ALIGNMENT)
        assertNotNull(software.save())
        AlignmentParams params = AlignmentParams.findOrCreateByPipeline(software)
        assertNotNull(params.save())
        AlignmentLog log = AlignmentLog.findOrCreateBySeqTrackAndAlignmentParams(seqTrack, params)
        assertNotNull(log.save())
        return log
    }
}
