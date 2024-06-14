/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.tracking

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.ProcessingStatus.Done
import de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.time.Instant

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.BLACKLIST_IMPORT_SOURCE_NOTIFICATION
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.NOTHING
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.PARTLY
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

@CompileDynamic
@Slf4j
@Component
@Transactional
class NotificationCreator {

    private static final List<Integer> MERGING_WORK_PACKAGE_NUMBERS = [1, 2].asImmutable()

    @Autowired
    AceseqService aceseqService

    @Autowired
    CreateNotificationTextService createNotificationTextService

    @Autowired
    IndelCallingService indelCallingService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    TicketService ticketService

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    RunYapsaService runYapsaService

    @Autowired
    SnvCallingService snvCallingService

    @Autowired
    SophiaService sophiaService

    @Autowired
    UserProjectRoleService userProjectRoleService

    @Autowired
    GrailsApplication grailsApplication

    void setStartedForSeqTracks(Collection<SeqTrack> seqTracks, Ticket.ProcessingStep step) {
        setStarted(ticketService.findAllTickets(seqTracks), step)
    }

    void setStarted(Collection<Ticket> tickets, Ticket.ProcessingStep step) {
        tickets.unique().each {
            ticketService.saveStartTimeIfNeeded(it, step)
        }
    }

    void processFinished(Set<SeqTrack> seqTracks) {
        Set<Ticket> tickets = ticketService.findAllTickets(seqTracks)
        LogThreadLocal.threadLog?.debug("evaluating processFinished for tickets: ${tickets}; " +
                "SeqTracks: ${seqTracks*.id}")
        for (Ticket ticket : tickets) {
            notifyAndSetFinishedTimestamps(ticket)
        }
    }

    void notifyAndSetFinishedTimestamps(Ticket ticket) {
        if (ticket.finalNotificationSent) {
            return
        }
        Set<SeqTrack> seqTracks = ticketService.findAllSeqTracks(ticket)
        ProcessingStatus status = getProcessingStatus(seqTracks)
        List<Ticket.ProcessingStep> justCompletedProcessingSteps = []
        boolean mightDoMore = false
        for (Ticket.ProcessingStep step : Ticket.ProcessingStep.values()) {
            WorkflowProcessingStatus stepStatus = status."${step}ProcessingStatus"
            boolean previousStepFinished = step.dependsOn ? (ticket."${step.dependsOn}Finished" != null) : true
            if (ticket."${step}Finished" == null && stepStatus.done != NOTHING && !stepStatus.mightDoMore && previousStepFinished) {
                justCompletedProcessingSteps.add(step)
                if (ticketService.saveEndTimeIfNeeded(ticket, step)) {
                    sendCustomerNotification(ticket, status, step)
                    LogThreadLocal.threadLog?.info("sent customer notification for Ticket ${ticket.ticketNumber}: ${step}")
                }
            }
            if (stepStatus.mightDoMore) {
                mightDoMore = true
            }
        }
        if (justCompletedProcessingSteps) {
            LogThreadLocal.threadLog?.debug("inside if anythingJustCompleted, sending operator notification")
            sendProcessingStatusOperatorNotification(ticket, seqTracks, status, !mightDoMore)
            if (!mightDoMore) {
                LogThreadLocal.threadLog?.debug("!mightDoMore: marking as finalNotificationSent")
                ticketService.markFinalNotificationSent(ticket)
            }
            if (justCompletedProcessingSteps.contains(Ticket.ProcessingStep.INSTALLATION)) {
                LogThreadLocal.threadLog?.debug("installation just completed")
                sendImportSourceOperatorNotification(ticket)
            }
        }
    }

    void sendCustomerNotification(Ticket ticket, ProcessingStatus status, Ticket.ProcessingStep notificationStep) {
        if (notificationStep.notificationSubject != null) {
            status.seqTrackProcessingStatuses.groupBy { it.seqTrack.project }.values().each {
                sendCustomerNotificationForOneProject(ticket, new ProcessingStatus(it), notificationStep)
            }
        }
    }

    void sendCustomerNotificationForOneProject(Ticket ticket, ProcessingStatus status, Ticket.ProcessingStep notificationStep) {
        if (notificationStep.notificationSubject != null) {
            Collection<SeqTrack> seqTracks = status.seqTrackProcessingStatuses*.seqTrack
            if (!seqTracks) {
                throw new IllegalArgumentException('ProcessingStatus contains no SeqTracks')
            }
            Project project = CollectionUtils.exactlyOneElement(seqTracks*.project.unique())
            Set<IlseSubmission> ilseSubmissions = seqTracks*.ilseSubmission.findAll() as Set

            List<String> recipients = (ticket.automaticNotification && project.processingNotification) ?
                userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([project]) : []

            StringBuilder subject = new StringBuilder("[${ticketService.getPrefixedTicketNumber(ticket)}] ")
            if (!recipients) {
                subject.append('TO BE SENT: ')
            }

            if (ilseSubmissions) {
                subject.append("[S#${ilseSubmissions*.ilseNumber.sort().join(',')}] ")
            }
            subject.append("${project.name} sequencing data ${notificationStep.notificationSubject}")

            String content = createNotificationTextService.notification(ticket, status, notificationStep, project)
            if (!content) {
                log.debug("No Email was sent! Subject would have been '${subject}.")
                return
            }

            mailHelperService.saveMail(subject.toString(), content, recipients)
        }
    }

    void sendProcessingStatusOperatorNotification(Ticket ticket, Set<SeqTrack> seqTracks, ProcessingStatus status, boolean finalNotification) {
        StringBuilder subject = new StringBuilder()

        subject.append(ticketService.getPrefixedTicketNumber(ticket))
        if (finalNotification) {
            subject.append(' Final')
        }
        subject.append(' Processing Status Update')

        StringBuilder content = new StringBuilder()
        content.append("""\
        |${status}
        |
        |${seqTracks.size()} SeqTrack(s) in ticket ${ticket.ticketNumber}:
        |""".stripMargin())
        for (SeqTrack seqTrack : seqTracks.sort { a, b -> a.ilseId <=> b.ilseId ?: a.run.name <=> b.run.name ?: a.laneId <=> b.laneId }) {
            appendSeqTrackString(content, seqTrack)
            content.append('\n')
        }
        content.append('\n')

        // include the links to import detail pages
        content.append(createNotificationTextService.messageSourceService.createMessage("notification.import.detail.link"))
        content.append(createLinksToImportDetailPage(ticket))

        mailHelperService.saveMail(subject.toString(), content.toString())
    }

    private StringBuilder createLinksToImportDetailPage(Ticket ticket) {
        StringBuilder content = new StringBuilder()

        final String metadataImportController = "metadataImport"
        final String metadataImportAction = "details"

        GrailsArtefactCheckHelper.check(grailsApplication, metadataImportController, metadataImportAction)

        ticketService.getAllFastqImportInstances(ticket).each {
            content.append('\n')
            content.append(createNotificationTextService.linkGenerator.link([
                    (LinkGenerator.ATTRIBUTE_CONTROLLER): metadataImportController,
                    (LinkGenerator.ATTRIBUTE_ACTION)    : metadataImportAction,
                    (LinkGenerator.ATTRIBUTE_ID)        : it.id,
                    (LinkGenerator.ATTRIBUTE_ABSOLUTE)  : true,
            ]))
        }

        return content
    }

    void sendImportSourceOperatorNotification(Ticket ticket) {
        String prefixedTicketNumber = ticketService.getPrefixedTicketNumber(ticket)
        String subject = "Import source ready for deletion [${prefixedTicketNumber}]"
        String ticketUrl = ticketService.buildTicketDirectLink(ticket)

        String content = """\
                |Related Ticket: ${prefixedTicketNumber}
                |${ticketUrl}
                |
                |Deletion Script:
                |
                |#!/bin/bash
                |
                |set -e
                |""".stripMargin()

        List<String> pathsToDelete = getPathsToDelete(ticket)
        content += pathsToDelete.collect { "rm -f ${it}" }.join("\n")

        if (pathsToDelete) {
            mailHelperService.saveMail(subject, content)
        }
    }

    private List<String> getPathsToDelete(Ticket ticket) {
        List<String> allPaths = []
        ticketService.getMetaDataFilesOfTicket(ticket).each { MetaDataFile metaDataFile ->
            List<String> initialPaths = metaDataFile.fastqImportInstance.sequenceFiles*.fullInitialPath
            allPaths.addAll(initialPaths)
        }
        return getPrefixBlacklistFilteredStrings(allPaths)
    }

    private List<String> getPrefixBlacklistFilteredStrings(List<String> strings) {
        List<String> blacklist = processingOptionService.findOptionAsList(BLACKLIST_IMPORT_SOURCE_NOTIFICATION)
        return strings.findAll { String path ->
            blacklist.every { it == "" || !path.startsWith(it) }
        }
    }

    void appendSeqTrackString(StringBuilder sb, SeqTrack seqTrack) {
        if (seqTrack.ilseId) {
            sb.append("ILSe ${seqTrack.ilseId}, ")
        }
        sb.append("${seqTrack.run.name}, lane ${seqTrack.laneId}, ${seqTrack.project.name}, ${seqTrack.individual.pid}, " +
                "${seqTrack.sampleType.name}, ${seqTrack.seqType.name} ${seqTrack.seqType.libraryLayout}")
    }

    ProcessingStatus getProcessingStatus(Collection<SeqTrack> seqTracks) {
        ProcessingStatus status = new ProcessingStatus(seqTracks.collect {
            new SeqTrackProcessingStatus(it,
                    it.dataInstallationState == SeqTrack.DataProcessingState.FINISHED ? ALL_DONE : NOTHING_DONE_MIGHT_DO,
                    it.fastqcState == SeqTrack.DataProcessingState.FINISHED ? ALL_DONE : NOTHING_DONE_MIGHT_DO, [])
        })
        List<MergingWorkPackageProcessingStatus> allMwpStatuses = fillInMergingWorkPackageProcessingStatuses(status.seqTrackProcessingStatuses)
        fillInSamplePairStatuses(allMwpStatuses)

        return status
    }

    List<MergingWorkPackageProcessingStatus> fillInMergingWorkPackageProcessingStatuses(Collection<SeqTrackProcessingStatus> seqTrackProcessingStatuses) {
        List<SeqTrackProcessingStatus> alignableSeqTrackProcessingStatuses = findAlignableSeqtracks(seqTrackProcessingStatuses)
        List<MergingWorkPackage> mwps = mergingWorkPackageContainingSeqTracks(alignableSeqTrackProcessingStatuses)
        return createAndConnectMergingWorkPackageProcessingStatus(mwps, alignableSeqTrackProcessingStatuses)
    }

    private List<SeqTrackProcessingStatus> findAlignableSeqtracks(Collection<SeqTrackProcessingStatus> seqTrackProcessingStatuses) {
        Collection<SeqTrackProcessingStatus> alignableSeqTrackProcessingStatuses = seqTrackProcessingStatuses.findAll {
            SeqTrackService.mayAlign(it.seqTrack)
        }
        return alignableSeqTrackProcessingStatuses
    }

    private List<MergingWorkPackage> mergingWorkPackageContainingSeqTracks(List<SeqTrackProcessingStatus> alignableSeqTrackProcessingStatuses) {
        List<SeqTrack> tracks = alignableSeqTrackProcessingStatuses*.seqTrack

        if (tracks.empty) {
            // avoid Postgres syntax errors when 'in'-query gets an empty list.
            // "in ()" works in H2 in-memory SQL, but not in PostGreSQL
            return []
        }

        return MergingWorkPackage.createCriteria().listDistinct {
            seqTracks {
                'in'('id', tracks*.id)
            }
        }
    }

    private List<MergingWorkPackageProcessingStatus> createAndConnectMergingWorkPackageProcessingStatus(
            List<MergingWorkPackage> mergingWorkPackages, List<SeqTrackProcessingStatus> alignableSeqTrackProcessingStatuses) {
        return mergingWorkPackages.collect { MergingWorkPackage mergingWorkPackage ->
            MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(mergingWorkPackage)
            connectSeqTrackProcessingStatusAndMergingWorkPackageProcessingStatus(alignableSeqTrackProcessingStatuses, mergingWorkPackage, mwpStatus)
            return mwpStatus
        }
    }

    private MergingWorkPackageProcessingStatus createMergingWorkPackageProcessingStatus(MergingWorkPackage mergingWorkPackage) {
        AbstractBamFile bamFile = mergingWorkPackage.bamFileThatIsReadyForFurtherAnalysis
        MergingWorkPackageProcessingStatus mwpStatus = new MergingWorkPackageProcessingStatus(
                mergingWorkPackage,
                bamFile ? ALL_DONE : NOTHING_DONE_MIGHT_DO,
                bamFile,
                [],
        )
        return mwpStatus
    }

    private void connectSeqTrackProcessingStatusAndMergingWorkPackageProcessingStatus(
            List<SeqTrackProcessingStatus> alignableSeqTrackProcessingStatuses, MergingWorkPackage mergingWorkPackage,
            MergingWorkPackageProcessingStatus mwpStatus) {
        alignableSeqTrackProcessingStatuses.findAll {
            it.seqTrack in mergingWorkPackage.seqTracks
        }.each { SeqTrackProcessingStatus it ->
            it.mergingWorkPackageProcessingStatuses.add(mwpStatus)
        }
    }

    void fillInSamplePairStatuses(Collection<MergingWorkPackageProcessingStatus> mwpStatuses) {
        if (mwpStatuses.empty) {
            return
        }

        Map<MergingWorkPackage, MergingWorkPackageProcessingStatus> mwpStatusByMwp =
                mwpStatuses.collectEntries { [it.mergingWorkPackage, it] }

        SamplePair.createCriteria().list {
            or {
                'in'('mergingWorkPackage1', mwpStatusByMwp.keySet())
                'in'('mergingWorkPackage2', mwpStatusByMwp.keySet())
            }
        }.each { SamplePair samplePair ->
            SamplePairProcessingStatus samplePairStatus = getSamplePairProcessingStatus(samplePair)
            MERGING_WORK_PACKAGE_NUMBERS.each {
                MergingWorkPackageProcessingStatus mwpStatus = mwpStatusByMwp.get(samplePair."mergingWorkPackage${it}")
                if (mwpStatus) {
                    mwpStatus.samplePairProcessingStatuses.add(samplePairStatus)
                }
            }
        }
    }

    private static WorkflowProcessingStatus getAnalysisProcessingStatus(BamFilePairAnalysis analysis, SamplePair sp, AbstractBamFileAnalysisService service) {
        WorkflowProcessingStatus status
        if (analysis && !analysis.withdrawn && analysis.processingState == AnalysisProcessingStates.FINISHED && MERGING_WORK_PACKAGE_NUMBERS.every {
            AbstractBamFile bamFile = analysis."sampleType${it}BamFile"
            return bamFile == bamFile.mergingWorkPackage.bamFileThatIsReadyForFurtherAnalysis
        }) {
            status = ALL_DONE
        } else if (analysis && !analysis.withdrawn && AbstractBamFileAnalysisService.PROCESSING_STATES_NOT_PROCESSABLE.contains(analysis.processingState)) {
            status = NOTHING_DONE_MIGHT_DO
        } else if (MERGING_WORK_PACKAGE_NUMBERS.every {
            sp."mergingWorkPackage${it}".bamFileThatIsReadyForFurtherAnalysis
        }) {
            if (service.samplePairForProcessing(ProcessingPriority.MINIMUM, sp)) {
                status = NOTHING_DONE_MIGHT_DO
            } else {
                status = NOTHING_DONE_WONT_DO
            }
        } else {
            status = NOTHING_DONE_MIGHT_DO
        }
        return status
    }

    SamplePairProcessingStatus getSamplePairProcessingStatus(SamplePair sp) {
        AbstractSnvCallingInstance sci = sp.findLatestSnvCallingInstance()
        IndelCallingInstance ici = sp.findLatestIndelCallingInstance()
        AceseqInstance ai = sp.findLatestAceseqInstance()
        SophiaInstance si = sp.findLatestSophiaInstance()
        RunYapsaInstance ryi = sp.findLatestRunYapsaInstance()

        WorkflowProcessingStatus snvStatus = getAnalysisProcessingStatus(sci, sp, snvCallingService)
        WorkflowProcessingStatus indelStatus = getAnalysisProcessingStatus(ici, sp, indelCallingService)
        WorkflowProcessingStatus sophiaStatus = getAnalysisProcessingStatus(si, sp, sophiaService)
        WorkflowProcessingStatus aceseqStatus = getAnalysisProcessingStatus(ai, sp, aceseqService)
        WorkflowProcessingStatus runYapsaStatus = getAnalysisProcessingStatus(ryi, sp, runYapsaService)

        return new SamplePairProcessingStatus(
                sp,
                snvStatus,
                snvStatus == ALL_DONE ? sci : null,
                indelStatus,
                indelStatus == ALL_DONE ? ici : null,
                sophiaStatus,
                sophiaStatus == ALL_DONE ? si : null,
                aceseqStatus,
                aceseqStatus == ALL_DONE ? ai : null,
                runYapsaStatus,
                runYapsaStatus == ALL_DONE ? ryi : null,
        )
    }

    static <O> WorkflowProcessingStatus combineStatuses(Iterable<O> objects,
                                                        Closure<WorkflowProcessingStatus> getObjectStatus,
                                                        WorkflowProcessingStatus additionalStatus = null) {
        Done done = additionalStatus?.done
        boolean mightDoMore = additionalStatus?.mightDoMore ?: false
        for (O object : objects) {
            WorkflowProcessingStatus objectStatus = getObjectStatus(object)
            done = (objectStatus.done == done || done == null) ? objectStatus.done : PARTLY
            mightDoMore |= objectStatus.mightDoMore
            if (done == PARTLY && mightDoMore) {
                // No matter what status would be added, it has no effect on the combined status
                break
            }
        }
        if (done == null) {
            return NOTHING_DONE_WONT_DO
        }
        return WorkflowProcessingStatus.values().find { it.done == done && it.mightDoMore == mightDoMore }
    }

    void sendWorkflowCreateSuccessMail(MetaDataFile metaDataFile, String message) {
        metaDataFile.refresh()
        long id = metaDataFile.fastqImportInstance.id

        String subject = "[${ticketService.getPrefixedTicketNumber(metaDataFile.fastqImportInstance.ticket)}] " +
                "Workflow created successfully for ${metaDataFile.fileNameSource}"

        String body = [
                "The workflow creation succeeded:",
                "Import id: ${id}",
                "File: ${metaDataFile.fileNameSource}",
                "",
                message,
        ].join('\n')

        mailHelperService.saveMail(subject, body)
    }

    void sendWorkflowCreateErrorMail(MetaDataFile metaDataFile, Throwable throwable) {
        metaDataFile.refresh()
        long id = metaDataFile.fastqImportInstance.id
        List<Long> seqTrackIds = metaDataFile.fastqImportInstance.sequenceFiles*.seqTrack*.id.unique().sort()

        String subject = "[${ticketService.getPrefixedTicketNumber(metaDataFile.fastqImportInstance.ticket)}] " +
                "Failed to create workflows for ${metaDataFile.fileNameSource}"

        // Retrieve the names for the code generated in the body
        String className = metaDataFile.fastqImportInstance.class.simpleName
        String propertyName = metaDataFile.fastqImportInstance.state.class.simpleName

        String body = [
                "The workflow creation failed:",
                "Import id: ${id}",
                "File: ${metaDataFile.fileNameSource}",
                "Exception:",
                StackTraceUtils.getStackTrace(throwable),
                "",
                "To retry the workflow creation, please use the following command in the groovy web console:",
                "ctx.fastqImportInstanceService.updateState(${className}.get(${id}), ${propertyName}.WAITING)",
                "The console won't wait but another email will be sent.",
                "",
                "To delete the import, please run the script 'DeleteSeqtracks':",
                "https://gitlab.com/one-touch-pipeline/otp/-/blob/master/scripts/operations/dataCleanup/DeleteSeqtracks.groovy",
                "with the following seqTrack ids as input for the variable seqTracksIdList in the script: ",
                seqTrackIds.join(', '),
        ].join('\n')
        mailHelperService.saveErrorMailInNewTransaction(subject, body)
    }

    void sendBamImportWorkflowCreateSuccessMail(Ticket ticket, Long importId, Instant instant, String message) {
        String subject = ticket ? "[${ticketService.getPrefixedTicketNumber(ticket)}] " : ""
        subject += "Workflow created successfully at ${TimeFormats.DATE_TIME.getFormattedInstant(instant)} for BamImport with ID: ${importId}"

        String body = [
                "The workflow creation succeeded:",
                "Import id: ${importId}",
                "",
                message,
        ].join('\n')
        mailHelperService.saveMail(subject, body)
    }

    void sendBamImportWorkflowCreateErrorMail(Ticket ticket, Long importId, Instant instant, Throwable throwable) {
        String subject = ticket ? "[${ticketService.getPrefixedTicketNumber(ticket)}] " : ""
        subject += "Failed to create workflows at ${TimeFormats.DATE_TIME.getFormattedInstant(instant)} for BamImport with ID: ${importId}"

        BamImportInstance importInstance = BamImportInstance.get(importId)
        List<Long> bamIds = importInstance.externallyProcessedBamFiles*.id
        String propertyName = WorkflowCreateState.simpleName

        String body = [
                "The workflow creation failed:",
                "Import id: ${importId}",
                "Exception:",
                StackTraceUtils.getStackTrace(throwable),
                "To retry the workflow creation, please use the following command in the groovy web console:",
                "ctx.bamImportService.updateState(${importId}, ${propertyName}.WAITING)",
                "The console won't wait but another email will be sent.",
                "",
                "To delete the import, please ask the OTP Maintainer and provide them the following bam ids:",
                bamIds.join(', '),
        ].join('\n')
        mailHelperService.saveErrorMailInNewTransaction(subject, body)
    }
}
