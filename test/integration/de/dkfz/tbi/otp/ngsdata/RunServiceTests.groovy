package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.*
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class RunServiceTests extends AbstractIntegrationTest {
    def runService

    @Before
    void setUp() {
        createUserAndRoles()
    }

    @Test
    void testGetRunWithoutRun() {
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(runService.getRun(null))
            assertNull(runService.getRun(""))
            assertNull(runService.getRun(0))
            assertNull(runService.getRun("test"))
        }
    }

    @Test
    void testGetRunPermission() {
        Run run = mockRun("testRun")
        [OPERATOR, ADMIN].each { String username ->
            SpringSecurityUtils.doWithAuth(username) {
                assertNotNull(runService.getRun(run.id))
            }
        }
        [USER, TESTUSER].each { String username ->
            shouldFail(AccessDeniedException) {
                SpringSecurityUtils.doWithAuth(username) {
                    runService.getRun(run.id)
                }
            }
        }
    }

    @Test
    void testGetRunByLongAndStringIdentifier() {
        Run run = mockRun("test")
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(run, runService.getRun(run.id))
            assertEquals(run, runService.getRun("${run.id}"))
        }
    }

    @Test
    void testGetRunByName() {
        Run run = mockRun("test")
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNotNull(runService.getRun(run.name))
        }
    }

    @Test
    void testGetRunByNameAsIdentifier() {
        Run run = mockRun("test")
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(run, runService.getRun("test"))
            run.name = run.id + 1
            assertNotNull(run.save(flush: true))
            assertEquals(run, runService.getRun(run.name))
            Run run2 = new Run(name: "foo", seqCenter: SeqCenter.list().first(), seqPlatform: SeqPlatform.list().first())
            assertNotNull(run2.save())
            assertEquals(run2, runService.getRun(run.name))
        }
    }

    @Test
    void testRetrieveProcessParametersPermission() {
        Run run = mockRun("test")
        [OPERATOR, ADMIN].each { String username ->
            SpringSecurityUtils.doWithAuth(username) {
                assertNotNull(runService.retrieveProcessParameters(run))
            }
        }
        [USER, TESTUSER].each { String username ->
            shouldFail(AccessDeniedException) {
                SpringSecurityUtils.doWithAuth(username) {
                    runService.retrieveProcessParameters(run)
                }
            }
        }
    }

    @Test
    void testRetrieveProcessParameterEmpty() {
        Run run = mockRun("test")
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(runService.retrieveProcessParameters(run).isEmpty())
        }
    }

    @Test
    void testRetrieveProcessParameter() {
        Run run = mockRun("test")
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assert jep.save()
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assert jep.save(flush: true)

        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assert process.save()
        ProcessParameter param = new ProcessParameter(value: run.id, className: Run.class.name, process: process)
        assert param.save()

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(1, runService.retrieveProcessParameters(run).size())
            assertEquals(param, runService.retrieveProcessParameters(run).first())
        }

        Process process2 = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assert process2.save()
        ProcessParameter param2 = new ProcessParameter(value: run.id, className: Run.class.name, process: process2)
        assert param2.save()

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(2, runService.retrieveProcessParameters(run).size())
            assertEquals(param, runService.retrieveProcessParameters(run).first())
            assertEquals(param2, runService.retrieveProcessParameters(run).last())
        }
    }


    @Ignore
    @Test
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
    @Test
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
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()
        SeqCenter seqCenter = SeqCenter.findOrCreateByNameAndDirName("test", "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: name, seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())
        return run
    }

    private SeqTrack mockSeqTrack(Run run, String pid, String laneId) {
        Realm realm = Realm.findOrCreateByNameAndHostAndPortAndTimeoutAndDefaultJobSubmissionOptions("test", "pbs", 1234, 0, "")
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
