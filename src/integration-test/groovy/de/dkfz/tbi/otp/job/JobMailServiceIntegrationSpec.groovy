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
package de.dkfz.tbi.otp.job

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.MailHelperService

import java.nio.file.Path

@Rollback
@Integration
class JobMailServiceIntegrationSpec extends Specification implements DomainFactoryCore, DomainFactoryProcessingPriority {

    TestConfigService configService

    TicketService ticketService

    @TempDir
    Path tempDir

    @Unroll
    void "sendErrorNotification, when with #completedCount completed cluster jobs and with #failedCount failed cluster job, send expected email"() {
        given:
        JobStatusLoggingService jobStatusLoggingService = new JobStatusLoggingService()
        configService.addOtpProperties(tempDir)
        jobStatusLoggingService.configService = configService

        Ticket ticket = createTicket()
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.ilseSubmission = createIlseSubmission()
        seqTrack.save(flush: true)
        seqTrack.project.processingPriority = fasttrack ? findOrCreateProcessingPriorityFastrack() : findOrCreateProcessingPriorityNormal()
        seqTrack.project.save(flush: true)

        DomainFactory.createFastqFile([
                seqTrack: seqTrack,
                fastqImportInstance: createFastqImportInstance([
                        ticket: ticket,
                ]),
        ])

        Realm realm = DomainFactory.createRealm()
        seqTrack.project.realm = realm

        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.TICKET_SYSTEM_URL,
                type: null,
                value: "http:/localhost:8080",
        ])
        String url = ticketService.buildTicketDirectLink(ticket)

        ProcessingStep step = DomainFactory.createProcessingStepUpdate().processingStep

        List<ClusterJob> completedClusterJobs = []
        completedCount.times {
            ClusterJob clusterJob = DomainFactory.createClusterJob([
                    processingStep: step,
                    exitStatus    : ClusterJob.Status.COMPLETED,
            ])
            completedClusterJobs << clusterJob
            File logFile = new File(jobStatusLoggingService.constructLogFileLocation(realm, step, clusterJob.clusterJobId))
            logFile.parentFile.mkdirs()
            logFile << jobStatusLoggingService.constructMessage(realm, step, clusterJob.clusterJobId) << '\n'
        }

        List<ClusterJob> failedClusterJobs = []
        failedCount.times {
            failedClusterJobs << DomainFactory.createClusterJob([
                    processingStep: step,
                    exitStatus    : ClusterJob.Status.FAILED,
            ])
        }

        DomainFactory.createProcessParameter(step.process, seqTrack)
        assert step.processParameterObject

        AbstractJobImpl job = Mock(AbstractJobImpl) {
            _ * processingStep >> step
            0 * _
        }

        JobMailService jobMailService = new JobMailService([
                mailHelperService      : Mock(MailHelperService) {
                    1 * sendEmailToTicketSystem(_, _) >> { String emailSubject, String content ->
                        assert emailSubject.startsWith(fasttrack ? "FASTTRACK ERROR:" : "NORMAL ERROR:")
                        assert emailSubject.contains("${step.jobExecutionPlan.name} ${step.processParameterObject.individual.displayName} ${step.processParameterObject.project.name}")
                        assert content.contains('\nWorkflow:\n')
                        assert content.contains('\nOTP Job:\n')
                        assert content.contains("\n  ilseNumbers: ${seqTrack.ilseSubmission.ilseNumber}\n")
                        assert content.contains("\n  openTickets: ${url}\n")
                        assert (failedCount > 0) == content.contains('\nCluster Job:\n')
                        completedClusterJobs.each {
                            assert !content.contains("clusterId: ${it.clusterJobId}")
                        }
                        failedClusterJobs.each {
                            assert content.contains("clusterId: ${it.clusterJobId}")
                        }
                    }
                    0 * _
                },
                processService         : Mock(ProcessService) {
                    processUrl(_) >> 'url'
                },
                jobStatusLoggingService: jobStatusLoggingService,
                ticketService      : new TicketService(),
        ])

        when:
        jobMailService.sendErrorNotification(job, "No Exception message")

        then:
        noExceptionThrown()

        cleanup:
        configService.clean()

        where:
        completedCount | failedCount | fasttrack
        0              | 0           | false
        5              | 0           | false
        0              | 5           | false
        2              | 3           | false
        0              | 0           | true
        5              | 0           | true
    }

    void "restartCount, when not restarted, return 0"() {
        given:
        ProcessingStep step = DomainFactory.createProcessingStep()

        expect:
        new JobMailService().restartCount(step) == 0
    }

    void "restartCount, when one time restarted, return 1"() {
        given:
        ProcessingStep step = DomainFactory.createRestartedProcessingStep()

        expect:
        new JobMailService().restartCount(step) == 1
    }

    void "restartCount, when two time restarted, return 2"() {
        given:
        ProcessingStep step = DomainFactory.createRestartedProcessingStep(original: DomainFactory.createRestartedProcessingStep())

        expect:
        new JobMailService().restartCount(step) == 2
    }
}
