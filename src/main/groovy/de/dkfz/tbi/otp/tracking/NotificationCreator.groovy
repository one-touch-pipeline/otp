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
package de.dkfz.tbi.otp.tracking

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.tracking.ProcessingStatus.Done
import de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.BLACKLIST_IMPORT_SOURCE_NOTIFICATION
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.NOTHING
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.PARTLY
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

@Slf4j
@Component
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
    OtrsTicketService otrsTicketService

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

    void setStartedForSeqTracks(Collection<SeqTrack> seqTracks, OtrsTicket.ProcessingStep step) {
        setStarted(otrsTicketService.findAllOtrsTickets(seqTracks), step)
    }

    void setStarted(Collection<OtrsTicket> otrsTickets, OtrsTicket.ProcessingStep step) {
        otrsTickets.unique().each {
            otrsTicketService.saveStartTimeIfNeeded(it, step)
        }
    }

    void processFinished(Set<SeqTrack> seqTracks) {
        Set<OtrsTicket> otrsTickets = otrsTicketService.findAllOtrsTickets(seqTracks)
        LogThreadLocal.threadLog?.debug("evaluating processFinished for OtrsTickets: ${otrsTickets}; " +
                "SeqTracks: ${seqTracks*.id}")
        for (OtrsTicket ticket : otrsTickets) {
            setFinishedTimestampsAndNotify(ticket)
        }
    }

    void setFinishedTimestampsAndNotify(OtrsTicket ticket) {
        if (ticket.finalNotificationSent) {
            return
        }
        Set<SeqTrack> seqTracks = ticket.findAllSeqTracks()
        ProcessingStatus status = getProcessingStatus(seqTracks)
        List<OtrsTicket.ProcessingStep> justCompletedProcessingSteps = []
        boolean mightDoMore = false
        for (OtrsTicket.ProcessingStep step : OtrsTicket.ProcessingStep.values()) {
            WorkflowProcessingStatus stepStatus = status."${step}ProcessingStatus"
            boolean previousStepFinished = step.dependsOn ? (ticket."${step.dependsOn}Finished" != null) : true
            if (ticket."${step}Finished" == null && stepStatus.done != NOTHING && !stepStatus.mightDoMore && previousStepFinished) {
                justCompletedProcessingSteps.add(step)
                if (otrsTicketService.saveEndTimeIfNeeded(ticket, step)) {
                    sendCustomerNotification(ticket, status, step)
                    LogThreadLocal.threadLog?.info("sent customer notification for OTRS Ticket ${ticket.ticketNumber}: ${step}")
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
                otrsTicketService.markFinalNotificationSent(ticket)
            }
            if (justCompletedProcessingSteps.contains(OtrsTicket.ProcessingStep.INSTALLATION)) {
                LogThreadLocal.getThreadLog()?.debug("installation just completed")
                sendImportSourceOperatorNotification(ticket)
            }
        }
    }

    void sendCustomerNotification(OtrsTicket ticket, ProcessingStatus status, OtrsTicket.ProcessingStep notificationStep) {
        if (notificationStep.notificationSubject != null) {
            status.seqTrackProcessingStatuses.groupBy { it.seqTrack.project }.values().each {
                sendCustomerNotificationForOneProject(ticket, new ProcessingStatus(it), notificationStep)
            }
        }
    }

    void sendCustomerNotificationForOneProject(OtrsTicket ticket, ProcessingStatus status, OtrsTicket.ProcessingStep notificationStep) {
        if (notificationStep.notificationSubject != null) {
            Collection<SeqTrack> seqTracks = status.seqTrackProcessingStatuses*.seqTrack
            if (!seqTracks) {
                throw new IllegalArgumentException('ProcessingStatus contains no SeqTracks')
            }
            Project project = CollectionUtils.exactlyOneElement(seqTracks*.project.unique())
            Set<IlseSubmission> ilseSubmissions = seqTracks*.ilseSubmission.findAll() as Set

            List<String> recipients = []
            if (ticket.automaticNotification && project.processingNotification) {
                recipients = userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(project)
            }
            StringBuilder subject = new StringBuilder("[${ticket.prefixedTicketNumber}] ")
            if (!recipients) {
                subject.append('TO BE SENT: ')
            }
            recipients << processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION)

            if (ilseSubmissions) {
                subject.append("[S#${ilseSubmissions*.ilseNumber.sort().join(',')}] ")
            }
            subject.append("${project.name} sequencing data ${notificationStep.notificationSubject}")

            String content = createNotificationTextService.notification(ticket, status, notificationStep, project)
            if (!content) {
                log.debug("No Email was sent! Subject would have been '${subject.toString()}.")
                return
            }
            mailHelperService.sendEmail(subject.toString(), content, recipients)
        }
    }

    void sendProcessingStatusOperatorNotification(OtrsTicket ticket, Set<SeqTrack> seqTracks, ProcessingStatus status, boolean finalNotification) {
        StringBuilder subject = new StringBuilder()

        subject.append(ticket.prefixedTicketNumber)
        if (finalNotification) {
            subject.append(' Final')
        }
        subject.append(' Processing Status Update')

        List<String> recipients = [processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION)]

        List<Project> projects = seqTracks*.project.unique()

        if (finalNotification && projects.size() == 1 && projects.first().customFinalNotification) {
            List<IlseSubmission> ilseSubmissions = seqTracks*.ilseSubmission.findAll().unique()
            if (ilseSubmissions) {
                subject.append(" [S#${ilseSubmissions*.ilseNumber.sort().join(',')}]")
            }
            List<Individual> individuals = seqTracks*.individual.findAll().unique()
            subject.append(" ${individuals*.pid.sort().join(', ')} ")
            List<SeqType> seqTypes = seqTracks*.seqType.findAll().unique()
            subject.append("(${seqTypes*.displayName.sort().join(', ')})")
            if (ticket.automaticNotification) {
                recipients.addAll(userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(projects))
            }
        }

        StringBuilder content = new StringBuilder()
        content.append(status.toString())
        content.append('\n').append(seqTracks.size()).append(' SeqTrack(s) in ticket ').append(ticket.ticketNumber).append(':\n')
        for (SeqTrack seqTrack : seqTracks.sort { a, b -> a.ilseId <=> b.ilseId ?: a.run.name <=> b.run.name ?: a.laneId <=> b.laneId }) {
            appendSeqTrackString(content, seqTrack)
            content.append('\n')
        }
        mailHelperService.sendEmail(subject.toString(), content.toString(), recipients)
    }

    void sendImportSourceOperatorNotification(OtrsTicket ticket) {
        List<String> recipients = [processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION)]

        String prefixedTicketNumber = ticket.prefixedTicketNumber
        String subject = "Import source ready for deletion [${prefixedTicketNumber}]"

        String content = """\
                |Related Ticket: ${prefixedTicketNumber}
                |${ticket.url}
                |
                |Deletion Script:
                |
                |#!/bin/bash
                |
                |set -e
                |""".stripMargin()

        List<String> pathsToDelete = getPathsToDelete(ticket)
        content += pathsToDelete.collect { "rm ${it}" }.join("\n")

        if (pathsToDelete) {
            mailHelperService.sendEmail(subject, content, recipients)
        }
    }

    private List<String> getPathsToDelete(OtrsTicket otrsTicket) {
        List<String> allPaths = []
        otrsTicketService.getMetaDataFilesOfOtrsTicket(otrsTicket).each { MetaDataFile metaDataFile ->
            allPaths << metaDataFile.fullPath
            List<String> dataFilePaths = metaDataFile.runSegment.dataFiles.collect { DataFile dataFile ->
                dataFile.fullInitialPath
            }
            allPaths.addAll(dataFilePaths)
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
            SeqTrackService.mayAlign(it.seqTrack, false)
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
        AbstractMergedBamFile bamFile = mergingWorkPackage.bamFileThatIsReadyForFurtherAnalysis
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

    private static WorkflowProcessingStatus getAnalysisProcessingStatus(BamFilePairAnalysis analysis, SamplePair sp, BamFileAnalysisService service) {
        WorkflowProcessingStatus status
        if (analysis && !analysis.withdrawn && analysis.processingState == AnalysisProcessingStates.FINISHED && MERGING_WORK_PACKAGE_NUMBERS.every {
            AbstractMergedBamFile bamFile = analysis."sampleType${it}BamFile"
            return bamFile == bamFile.mergingWorkPackage.bamFileThatIsReadyForFurtherAnalysis
        }) {
            status = ALL_DONE
        } else if (analysis && !analysis.withdrawn && BamFileAnalysisService.processingStatesNotProcessable.contains(analysis.processingState)) {
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
}
