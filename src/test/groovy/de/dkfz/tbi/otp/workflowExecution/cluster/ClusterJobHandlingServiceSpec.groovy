/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.cluster

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.roddy.BEException
import de.dkfz.roddy.config.JobLog
import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.cluster.logs.ClusterLogDirectoryService
import de.dkfz.tbi.otp.workflowExecution.cluster.logs.JobStatusLoggingFileService

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

class ClusterJobHandlingServiceSpec extends Specification implements ServiceUnitTest<ClusterJobHandlingService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ClusterJob,
                Realm,
                WorkflowStep,
        ]
    }

    private WorkflowStep workflowStep

    private BEJob job1

    private BEJob job2

    private List<BEJob> jobs

    private void setupJobData() {
        workflowStep = createWorkflowStep()
        job1 = createBeJobs()
        job2 = createBeJobs()
        jobs = [
                job1,
                job2,
        ]
    }

    void "createBeJobsToSend, when all fine, then create BeJobs"() {
        given:
        workflowStep = createWorkflowStep()
        Realm realm = createRealm()

        Map<JobSubmissionOption, String> jobSubmissionOptionMapInitial = [(JobSubmissionOption.MEMORY): "${nextId}M",]
        Map<JobSubmissionOption, String> jobSubmissionOptionMapMerged = [(JobSubmissionOption.QUEUE): "queue${nextId}",] + jobSubmissionOptionMapInitial
        ResourceSet resourceSet = new ResourceSet(null, 1, 1, Duration.ofHours(5), null, 'queue', null)
        String jobName = "Name${nextId}"

        String logFileName = "File${nextId}"
        String logMessage = "Message${nextId}"
        Path clusterLogDirectory = Paths.get("/tmp/any${nextId}")

        String script1 = "script${nextId}"
        String script2 = "script${nextId}"

        String wrappedScript1 = "wrapped script${nextId}\n${script1}"
        String wrappedScript2 = "wrapped script${nextId}\n${script2}"

        List<String> scripts = [
                script1,
                script2,
        ]

        service.clusterJobHelperService = Mock(ClusterJobHelperService) {
            1 * mergeResources(workflowStep.workflowRun.priority, jobSubmissionOptionMapInitial) >> jobSubmissionOptionMapMerged
            1 * createResourceSet(jobSubmissionOptionMapMerged) >> resourceSet
            1 * constructJobName(workflowStep) >> jobName
            1 * wrapScript(script1, logFileName, logMessage) >> wrappedScript1
            1 * wrapScript(script2, logFileName, logMessage) >> wrappedScript2
        }
        service.jobStatusLoggingFileService = Mock(JobStatusLoggingFileService) {
            1 * constructLogFileLocation(realm, workflowStep) >> logFileName
            1 * constructMessage(realm, workflowStep) >> logMessage
        }
        service.clusterLogDirectoryService = Mock(ClusterLogDirectoryService) {
            1 * createAndGetLogDirectory(workflowStep) >> clusterLogDirectory
        }
        service.logService = Mock(LogService) {
            2 * addSimpleLogEntry(workflowStep, _)
        }
        service.fileService = Mock(FileService) {
            1 * toFile(_) >> { Path path ->
                path.toFile()
            }
        }

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager)

        when:
        List<BEJob> beJobs = service.createBeJobsToSend(jobManager, realm, workflowStep, scripts, jobSubmissionOptionMapInitial)

        then:
        beJobs.size() == 2
        beJobs[0].toolScript == wrappedScript1
        beJobs[1].toolScript == wrappedScript2
        beJobs.each {
            assert it.jobName == jobName
            assert it.resourceSet == resourceSet
            assert it.jobManager == jobManager
        }
    }

    void "sendJobs, when all fine, then start each job and no exception thrown"() {
        given:
        setupJobData()

        BEJobResult jobResult1 = createBEJobResult(job1)
        BEJobResult jobResult2 = createBEJobResult(job2)

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * submitJob(job1) >> jobResult1
            1 * submitJob(job2) >> jobResult2
            0 * _
        }

        service.logService = Mock(LogService) {
            2 * addSimpleLogEntry(workflowStep, _)
            0 * _
        }

        when:
        service.sendJobs(jobManager, workflowStep, [job1, job2])

        then:
        true
    }

    void "sendJobs, when submit fail on first, then call killJobs only for first and throw SubmitClusterJobException"() {
        given:
        setupJobData()

        BEJobResult jobResult1 = createBEJobResult(job1, false)

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * submitJob(job1) >> jobResult1
            1 * killJobs([job1])
            0 * _
        }

        service.logService = Mock(LogService)

        when:
        service.sendJobs(jobManager, workflowStep, [job1, job2])

        then:
        SubmitClusterJobException e = thrown()
        !e.message.contains(ClusterJobHandlingService.NESTED_FAIL_MESSAGE_FOR_KILL)
    }

    void "sendJobs, when submit fail on second, then call killJobs for both and throw SubmitClusterJobException"() {
        given:
        setupJobData()

        BEJobResult jobResult1 = createBEJobResult(job1)
        BEJobResult jobResult2 = createBEJobResult(job2, false)

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * submitJob(job1) >> jobResult1
            1 * submitJob(job2) >> jobResult2
            1 * killJobs([job1, job2])
            0 * _
        }

        service.logService = Mock(LogService)

        when:
        service.sendJobs(jobManager, workflowStep, [job1, job2])

        then:
        SubmitClusterJobException e = thrown()
        !e.message.contains(ClusterJobHandlingService.NESTED_FAIL_MESSAGE_FOR_KILL)
    }

    void "sendJobs, when submit throw exception on first, then call killJobs for empty list and throw SubmitClusterJobException"() {
        given:
        setupJobData()

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * submitJob(job1) >> { throw new BEException('test failure') }
            1 * killJobs([])
            0 * _
        }

        service.logService = Mock(LogService)

        when:
        service.sendJobs(jobManager, workflowStep, [job1, job2])

        then:
        SubmitClusterJobException e = thrown()
        !e.message.contains(ClusterJobHandlingService.NESTED_FAIL_MESSAGE_FOR_KILL)
    }

    void "sendJobs, when submit throw exception on second, then call killJobs for first and throw SubmitClusterJobException"() {
        given:
        setupJobData()

        BEJobResult jobResult1 = createBEJobResult(job1)

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * submitJob(job1) >> jobResult1
            1 * submitJob(job2) >> { throw new BEException('test failure') }
            1 * killJobs([job1])
            0 * _
        }

        service.logService = Mock(LogService)

        when:
        service.sendJobs(jobManager, workflowStep, [job1, job2])

        then:
        SubmitClusterJobException e = thrown()
        !e.message.contains(ClusterJobHandlingService.NESTED_FAIL_MESSAGE_FOR_KILL)
    }

    void "sendJobs, when submit fail and killJobs fail, then throw also the SubmitClusterJobException"() {
        given:
        setupJobData()

        BEJobResult jobResult1 = createBEJobResult(job1)
        BEJobResult jobResult2 = createBEJobResult(job2, false)

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * submitJob(job1) >> jobResult1
            1 * submitJob(job2) >> jobResult2
            1 * killJobs([job1, job2]) >> { throw new OtpRuntimeException() }
            0 * _
        }

        service.logService = Mock(LogService)

        when:
        service.sendJobs(jobManager, workflowStep, [job1, job2])

        then:
        KillClusterJobException e = thrown()
        e.message.contains(ClusterJobHandlingService.NESTED_FAIL_MESSAGE_FOR_KILL)
    }

    void "startJob, when all fine, then all jobs started and no exception thrown"() {
        given:
        setupJobData()

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * startHeldJobs(jobs)
            0 * _
        }

        service.logService = Mock(LogService) {
            2 * addSimpleLogEntry(workflowStep, _)
            0 * _
        }

        when:
        service.startJob(jobManager, workflowStep, [job1, job2])

        then:
        true
    }

    void "startJob, when start fail, then all jobs should be killed and throw StartClusterJobException"() {
        given:
        setupJobData()

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * startHeldJobs(jobs) >> { throw new OtpRuntimeException() }
            1 * killJobs([job1, job2])
            0 * _
        }

        service.logService = Mock(LogService)

        when:
        service.startJob(jobManager, workflowStep, [job1, job2])

        then:
        thrown(StartClusterJobException)
    }

    void "startJob, when start fail and kill fail, then throw also StartClusterJobException"() {
        given:
        setupJobData()

        BatchEuphoriaJobManager jobManager = Mock(BatchEuphoriaJobManager) {
            1 * startHeldJobs(jobs) >> { throw new OtpRuntimeException() }
            1 * killJobs([job1, job2]) >> { throw new OtpRuntimeException() }
            0 * _
        }

        service.logService = Mock(LogService)
        when:
        service.startJob(jobManager, workflowStep, [job1, job2])

        then:
        thrown(KillClusterJobException)
    }

    void "createAndSaveClusterJobs, when all fine, then create all cluster jobs and no exception thrown"() {
        given:
        setupJobData()
        Realm realm = createRealm()

        String sshUser = "sshuser${nextId}"

        service.configService = Mock(ConfigService) {
            1 * getSshUser() >> sshUser
        }
        service.clusterJobService = Mock(ClusterJobService) {
            2 * createClusterJob(realm, _, sshUser, workflowStep, _) >> { Realm realm2, String clusterJobId, String userName,
                                                                          WorkflowStep workflowStep, String clusterJobName ->
                new ClusterJob([clusterJobId: clusterJobId])
            }
        }
        service.logService = Mock(LogService) {
            2 * addSimpleLogEntry(workflowStep, _)
            0 * _
        }

        when:
        List<ClusterJob> clusterJobs = service.createAndSaveClusterJobs(realm, workflowStep, jobs)

        then:
        TestCase.assertContainSame(clusterJobs*.clusterJobId, jobs*.jobID*.shortID)
    }

    void "collectJobStatistics, when all fine, then statistics are retrieved for all cluster jobs"() {
        given:
        setupJobData()
        List<ClusterJob> clusterJobs = (1..3).collect {
            createClusterJob([
                    workflowStep: workflowStep,
                    checkStatus : ClusterJob.CheckStatus.CREATED,
            ])
        }

        service.clusterStatisticService = Mock(ClusterStatisticService) {
            3 * retrieveAndSaveJobInformationAfterJobStarted(_)
        }
        service.logService = Mock(LogService) {
            5 * addSimpleLogEntry(workflowStep, _)
            0 * _
        }

        when:
        service.collectJobStatistics(workflowStep, clusterJobs)

        then:
        noExceptionThrown()
    }

    void "startMonitorClusterJob, when all fine, then all jobs have the checkState CHECKING"() {
        given:
        workflowStep = createWorkflowStep()
        List<ClusterJob> clusterJobs = (1..3).collect {
            createClusterJob([
                    workflowStep: workflowStep,
                    checkStatus : ClusterJob.CheckStatus.CREATED,
            ])
        }

        service.logService = Mock(LogService) {
            2 * addSimpleLogEntry(workflowStep, _)
            0 * _
        }

        when:
        service.startMonitorClusterJob(workflowStep, clusterJobs)

        then:
        clusterJobs.each {
            assert it.checkStatus == ClusterJob.CheckStatus.CHECKING
        }
    }

    private BEJob createBeJobs() {
        return new BEJob(
                null,
                "job name ${nextId}",
                null,
                "script ${nextId}",
                null,
                null,
                [],
                [:],
                null,
                JobLog.none(),
                null,
        )
    }

    private BEJobResult createBEJobResult(BEJob job, boolean success = true) {
        BEJobResult jobResult = new BEJobResult(null, job, new ExecutionResult([], success, success ? 0 : 1, [], [], ''), null, [:], [])
        job.runResult = jobResult
        return jobResult
    }
}
