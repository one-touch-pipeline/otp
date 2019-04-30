/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.security.UserAndRoles

import static org.junit.Assert.*

@Rollback
@Integration
class RunServiceTests implements UserAndRoles {
    RunService runService

    void setupData() {
        createUserAndRoles()
    }

    @Test
    void testGetRunWithoutRun() {
        setupData()
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(runService.getRun(null))
            assertNull(runService.getRun(""))
            assertNull(runService.getRun(0))
            assertNull(runService.getRun("test"))
        }
    }

    @Test
    void testGetRunPermission() {
        setupData()
        Run run = createRun("testRun")
        [OPERATOR, ADMIN].each { String username ->
            SpringSecurityUtils.doWithAuth(username) {
                assertNotNull(runService.getRun(run.id))
            }
        }
        [USER, TESTUSER].each { String username ->
            TestCase.shouldFail(AccessDeniedException) {
                SpringSecurityUtils.doWithAuth(username) {
                    runService.getRun(run.id)
                }
            }
        }
    }

    @Test
    void testGetRunByLongAndStringIdentifier() {
        setupData()
        Run run = createRun("test")
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(run, runService.getRun(run.id))
            assertEquals(run, runService.getRun("${run.id}"))
        }
    }

    @Test
    void testGetRunByName() {
        setupData()
        Run run = createRun("test")
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNotNull(runService.getRun(run.name))
        }
    }

    @Test
    void testGetRunByNameAsIdentifier() {
        setupData()
        Run run = createRun("test")
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
        setupData()
        Run run = createRun("test")
        [OPERATOR, ADMIN].each { String username ->
            SpringSecurityUtils.doWithAuth(username) {
                assertNotNull(runService.retrieveProcessParameters(run))
            }
        }
        [USER, TESTUSER].each { String username ->
            TestCase.shouldFail(AccessDeniedException) {
                SpringSecurityUtils.doWithAuth(username) {
                    runService.retrieveProcessParameters(run)
                }
            }
        }
    }

    @Test
    void testRetrieveProcessParameterEmpty() {
        setupData()
        Run run = createRun("test")
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(runService.retrieveProcessParameters(run).isEmpty())
        }
    }

    @Test
    void testRetrieveProcessParameter() {
        setupData()
        Run run = createRun("test")
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

    private Run createRun(String name) {
        return DomainFactory.createRun(
                name: name,
                seqCenter: DomainFactory.createSeqCenter(name: "test", dirName: "directory"),
        )
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

    /**
     * Creates a JobDefinition for the testJob.
     * @param name Name of the JobDefinition
     * @param jep The JobExecutionPlan this JobDefinition will belong to
     * @param previous The previous Job Execution plan (optional)
     * @return Created JobDefinition
     * @deprecated this was copied here to be able to delete AbstractIntegrationTest. Don't use it, refactor it.
     */
    @Deprecated
    private JobDefinition createTestJob(String name, JobExecutionPlan jep, JobDefinition previous = null) {
        JobDefinition jobDefinition = new JobDefinition(name: name, bean: "testJob", plan: jep, previous: previous)
        assertNotNull(jobDefinition.save())
        ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType test2 = new ParameterType(name: "test2", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType input = new ParameterType(name: "input", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        ParameterType input2 = new ParameterType(name: "input2", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        assertNotNull(test.save())
        assertNotNull(test2.save())
        assertNotNull(input.save())
        assertNotNull(input2.save(flush: true))
        return jobDefinition
    }
}
