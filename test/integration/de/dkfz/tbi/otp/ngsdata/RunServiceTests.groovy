package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission

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
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(runService.getRun(null))
            assertNull(runService.getRun(""))
            assertNull(runService.getRun(0))
            assertNull(runService.getRun("test"))
        }
    }

    @Test
    void testGetRunByIdentifier() {
        Run run = mockRun("test")
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                runService.getRun(run.id)
                runService.getRun("${run.id}")
            }
        }
        // Role Operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(run, runService.getRun(run.id))
            assertEquals(run, runService.getRun("${run.id}"))
        }
        // Admin should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(run, runService.getRun(run.id))
            assertEquals(run, runService.getRun("${run.id}"))
            // grant read permission to testuser
            aclUtilService.addPermission(run.seqCenter, "testuser", BasePermission.READ)
        }
        // now testuser should succeed
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(run, runService.getRun(run.id))
            assertEquals(run, runService.getRun("${run.id}"))
        }
        // but other user should still fail
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                runService.getRun(run.id)
                runService.getRun("${run.id}")
            }
        }
    }

    @Test
    void testGetRunByName() {
        Run run = mockRun("test")
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                runService.getRun("test")
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(run, runService.getRun("test"))
        }
        // admin should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(run, runService.getRun("test"))
            // grant read permission
            aclUtilService.addPermission(run.seqCenter, "testuser", BasePermission.READ)
        }
        // now testuser should succeed
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(run, runService.getRun("test"))
        }
        // but other user should still fail
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                runService.getRun("test")
            }
        }
    }

    @Test
    void testGetRunByNameAsIdentifier() {
        Run run = mockRun("2")
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(run, runService.getRun("2"))
            run.name = run.id + 1
            assertNotNull(run.save(flush: true))
            assertEquals(run, runService.getRun(run.name))
            Run run2 = new Run(name: "foo", seqCenter: SeqCenter.list().first(), seqPlatform: SeqPlatform.list().first())
            assertNotNull(run2.save())
            assertEquals(run2, runService.getRun(run.name))
        }
    }

    @Test
    void testRetrieveProcessParameterEmpty() {
        SpringSecurityUtils.doWithAuth("testuser") {
            // any user has access with no run
            assertTrue(runService.retrieveProcessParameters(null).isEmpty())
        }
        Run run = mockRun("test")
        // without ACL accessing a run should fail
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                runService.retrieveProcessParameters(run).isEmpty()
            }
        }
        // but not for an operator
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(runService.retrieveProcessParameters(run).isEmpty())
        }
        // neither for an admin
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(runService.retrieveProcessParameters(run).isEmpty())
            // grant read permission
            aclUtilService.addPermission(run.seqCenter, "testuser", BasePermission.READ)
        }
        // and not for the testuser after granting permission
        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(runService.retrieveProcessParameters(run).isEmpty())
        }
        // but still for any other user
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                runService.retrieveProcessParameters(run).isEmpty()
            }
        }
    }

    @Test
    void testRetrieveProcessParameter() {
        // create JobExecutionPlan
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())

        // create Run
        Run run = mockRun("test")

        ProcessParameter param = new ProcessParameter(value: run.id, className: Run.class.name, process: process)
        assertNotNull(param.save())

        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                runService.retrieveProcessParameters(run).size()
                runService.retrieveProcessParameters(run).first()
            }
        }
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(1, runService.retrieveProcessParameters(run).size())
            assertEquals(param, runService.retrieveProcessParameters(run).first())
        }
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(1, runService.retrieveProcessParameters(run).size())
            assertEquals(param, runService.retrieveProcessParameters(run).first())
            // grant read permission
            aclUtilService.addPermission(run.seqCenter, "testuser", BasePermission.READ)
        }
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(1, runService.retrieveProcessParameters(run).size())
            assertEquals(param, runService.retrieveProcessParameters(run).first())
        }
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                runService.retrieveProcessParameters(run).size()
                runService.retrieveProcessParameters(run).first()
            }
        }

        // create second Process
        Process process2 = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process2.save())
        ProcessParameter param2 = new ProcessParameter(value: run.id, className: Run.class.name, process: process2)
        assertNotNull(param2.save())

        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(2, runService.retrieveProcessParameters(run).size())
            assertEquals(param, runService.retrieveProcessParameters(run).first())
            assertEquals(param2, runService.retrieveProcessParameters(run).last())
        }
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(2, runService.retrieveProcessParameters(run).size())
            assertEquals(param, runService.retrieveProcessParameters(run).first())
            assertEquals(param2, runService.retrieveProcessParameters(run).last())
        }
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                runService.retrieveProcessParameters(run).size()
                runService.retrieveProcessParameters(run).first()
                runService.retrieveProcessParameters(run).last()
            }
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
