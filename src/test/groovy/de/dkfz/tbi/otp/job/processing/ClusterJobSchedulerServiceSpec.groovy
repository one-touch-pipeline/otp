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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.FileSystems

class ClusterJobSchedulerServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ClusterJob,
                JobDefinition,
                JobExecutionPlan,
                Process,
                ProcessingOption,
                ProcessingStep,
                ProcessingStepUpdate,
                Realm,
                SeqType,
        ]
    }

    static final String SSHUSER = "user"

    void "retrieveKnownJobsWithState, when qstat fails, throws exception"(String stderr, int exitCode) {
        given:
        Realm realm = DomainFactory.createRealm()
        ClusterJobSchedulerService service = new ClusterJobSchedulerService()
        service.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
        }

        when:
        service.retrieveKnownJobsWithState(realm)

        then:
        thrown(Exception)

        where:
        stderr  | exitCode
        ""      | 1
        "ERROR" | 1
    }

    void "retrieveKnownJobsWithState, when qstat output is empty, returns empty map"() {
        given:
        File logFolder = TestCase.uniqueNonExistentPath
        ClusterJobSchedulerService service = new ClusterJobSchedulerService()
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { command ->
                    new ProcessOutput(
                            stdout: command.tokenize().last(),
                            stderr: "",
                            exitCode: 0,
                    )
                },
        ] as RemoteShellHelper
        service.clusterJobManagerFactoryService.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
            getJobScheduler() >> JobScheduler.PBS
        }
        service.configService = Mock(ConfigService) {
            1 * getLoggingRootPath() >> logFolder
        }
        service.fileService = Mock(FileService) {
            1 * createFileWithContentOnDefaultRealm(_, _)
        }
        service.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem() >> FileSystems.default
        }

        when:
        Map<ClusterJobIdentifier, JobState> result = service.retrieveKnownJobsWithState()

        then:
        result.isEmpty() // can not written as .empty
    }

    void "test executeJob, succeeds"() {
        given:
        ClusterJobSchedulerService service = new ClusterJobSchedulerService()
        SeqType seqType = DomainFactory.createSeqType()
        Realm realm = DomainFactory.createRealm()
        String clusterJobId = "123"
        ProcessingPriority processingPriority = createProcessingPriority()

        ProcessParameterObject ppo = Stub(ProcessParameterObject) {
            getSeqType() >> seqType
            getProcessingPriority() >> processingPriority
        }
        ProcessingStep step = Stub(ProcessingStep) {
            getProcessParameterObject() >> ppo
        }
        Job job = Stub(Job) {
            getProcessingStep() >> step
        }
        service.schedulerService = Stub(SchedulerService) {
            getJobExecutedByCurrentThread() >> job
        }
        service.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
        }
        service.clusterJobSubmissionOptionsService = Stub(ClusterJobSubmissionOptionsService)
        service.jobStatusLoggingService = Stub(JobStatusLoggingService)
        service.clusterJobService = Mock(ClusterJobService)
        service.clusterJobLoggingService = Mock(ClusterJobLoggingService)
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.remoteShellHelper = Mock(RemoteShellHelper)
        service.clusterJobManagerFactoryService.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
            getJobScheduler() >> JobScheduler.PBS
        }

        ProcessOutput out = new ProcessOutput("${clusterJobId}.pbs", "", 0)
        ClusterJob clusterJob = DomainFactory.createClusterJob(clusterJobId: clusterJobId)
        DomainFactory.createProcessingStepUpdate(processingStep: clusterJob.processingStep)

        when:
        String result = service.executeJob(realm, "run the job")

        then:
        4 * service.clusterJobManagerFactoryService.remoteShellHelper.executeCommandReturnProcessOutput(_ as String) >> out
        1 * service.clusterJobService.createClusterJob(realm, clusterJobId, SSHUSER, step, seqType, _ as String) >> clusterJob
        1 * service.clusterJobLoggingService.createAndGetLogDirectory(_, _) >> { TestCase.uniqueNonExistentPath }
        result == clusterJobId
    }

    @Unroll
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    void "retrieveAndSaveJobInformationAfterJobStarted, #name"() {
        given:
        String clusterId = 1234
        ClusterJob job = DomainFactory.createClusterJob([
                clusterJobId: clusterId,
        ])

        int amendClusterJobCallCount = calledAmendClusterJob ? 1 : 0

        int counter = 0
        ClusterJobSchedulerService clusterJobSchedulerService = new ClusterJobSchedulerService([
                clusterJobManagerFactoryService: Mock(ClusterJobManagerFactoryService) {
                    1 * getJobManager() >> {
                        return Mock(BatchEuphoriaJobManager) {
                            queryExtendedJobStateByIdCallCount * queryExtendedJobStateById(_) >> { List<BEJobID> jobIds ->
                                assert jobIds.size() == 1
                                counter++
                                if (queryExtendedJobStateByIdCallCount == counter && calledAmendClusterJob) {
                                    return [(new BEJobID(clusterId)): new GenericJobInfo(null, null, null, null, null)]
                                }
                                throw new RuntimeException()
                            }
                        }
                    }
                },
                clusterJobService              : Mock(ClusterJobService) {
                    amendClusterJobCallCount * amendClusterJob(_, _)
                },
        ])

        expect:
        clusterJobSchedulerService.retrieveAndSaveJobInformationAfterJobStarted(job)

        where:
        name                        | queryExtendedJobStateByIdCallCount | calledAmendClusterJob
        'get value the first time'  | 1                                  | true
        'get value the second time' | 2                                  | true
        'do not get the'            | 2                                  | false
    }
}
