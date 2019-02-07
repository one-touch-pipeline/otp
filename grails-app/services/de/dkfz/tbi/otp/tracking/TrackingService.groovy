package de.dkfz.tbi.otp.tracking

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.tracking.ProcessingStatus.Done
import de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.NOTHING
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.PARTLY
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

class TrackingService {

    private static final List<Integer> MERGING_WORK_PACKAGE_NUMBERS = [1, 2].asImmutable()

    MailHelperService mailHelperService

    IndelCallingService indelCallingService
    SnvCallingService snvCallingService
    SophiaService sophiaService
    AceseqService aceseqService
    RunYapsaService runYapsaService
    ProcessingOptionService processingOptionService
    UserProjectRoleService userProjectRoleService

    CreateNotificationTextService createNotificationTextService

    OtrsTicket createOtrsTicket(String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        OtrsTicket otrsTicket = new OtrsTicket(
                ticketNumber: ticketNumber,
                seqCenterComment: seqCenterComment,
                automaticNotification: automaticNotification,
        )
        assert otrsTicket.save(flush: true, failOnError: true)
        return otrsTicket
    }

    OtrsTicket createOrResetOtrsTicket(String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        OtrsTicket otrsTicket = CollectionUtils.atMostOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber))
        if (otrsTicket) {
            otrsTicket.installationFinished = null
            otrsTicket.fastqcFinished = null
            otrsTicket.alignmentFinished = null
            otrsTicket.snvFinished = null
            otrsTicket.indelFinished = null
            otrsTicket.sophiaFinished = null
            otrsTicket.aceseqFinished = null
            otrsTicket.runYapsaFinished = null
            otrsTicket.finalNotificationSent = false
            otrsTicket.automaticNotification = automaticNotification
            if (!otrsTicket.seqCenterComment) {
                otrsTicket.seqCenterComment = seqCenterComment
            } else if (seqCenterComment && !otrsTicket.seqCenterComment.contains(seqCenterComment)) {
                otrsTicket.seqCenterComment += '\n\n' + seqCenterComment
            }
            assert otrsTicket.save(flush: true, failOnError: true)
            return otrsTicket
        } else {
            return createOtrsTicket(ticketNumber, seqCenterComment, automaticNotification)
        }
    }

    void resetAnalysisNotification(OtrsTicket otrsTicket) {
        otrsTicket.snvFinished = null
        otrsTicket.indelFinished = null
        otrsTicket.sophiaFinished = null
        otrsTicket.aceseqFinished = null
        otrsTicket.runYapsaFinished = null
        otrsTicket.finalNotificationSent = false
        assert otrsTicket.save(flush: true, failOnError: true)
    }

    void setStartedForSeqTracks(Collection<SeqTrack> seqTracks, OtrsTicket.ProcessingStep step) {
        setStarted(findAllOtrsTickets(seqTracks), step)
    }

    Set<OtrsTicket> findAllOtrsTickets(Collection<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return [] as Set
        }
        //set pessimistic lock does not work together with projection, therefore 2 queries used
        List<Long> otrsIds = DataFile.createCriteria().listDistinct {
            'in'('seqTrack', seqTracks)
            runSegment {
                isNotNull('otrsTicket')
                otrsTicket {
                    projections {
                        property('id')
                    }
                }
            }
        }
        return OtrsTicket.findAllByIdInList(otrsIds, [lock: true]) as Set
    }

    void setStarted(Collection<OtrsTicket> otrsTickets, OtrsTicket.ProcessingStep step) {
        otrsTickets.unique().each {
            if (it."${step}Started" == null) {
                it."${step}Started" = new Date()
                assert it.save(flush: true)
            }
        }
    }

    void processFinished(Set<SeqTrack> seqTracks) {
        SamplePairCreation samplePairCreation = new SamplePairCreation()
        Set<OtrsTicket> otrsTickets = findAllOtrsTickets(seqTracks)
        LogThreadLocal.getThreadLog()?.trace("evaluating processFinished for SPD: ${samplePairCreation}; OtrsTickets: ${otrsTickets}; SeqTracks: ${seqTracks*.id}")
        for (OtrsTicket ticket : otrsTickets) {
            setFinishedTimestampsAndNotify(ticket, samplePairCreation)
        }
    }

    void setFinishedTimestampsAndNotify(OtrsTicket ticket, SamplePairCreation samplePairCreation = new SamplePairCreation()) {
        if (ticket.finalNotificationSent) {
            return
        }
        Date now = new Date()
        Set<SeqTrack> seqTracks = ticket.findAllSeqTracks()
        ProcessingStatus status = getProcessingStatus(seqTracks, samplePairCreation)
        boolean anythingJustCompleted = false
        boolean mightDoMore = false
        for (OtrsTicket.ProcessingStep step : OtrsTicket.ProcessingStep.values()) {
            WorkflowProcessingStatus stepStatus = status."${step}ProcessingStatus"
            boolean previousStepFinished = step.dependsOn ? (ticket."${step.dependsOn}Finished" != null) : true
            if (ticket."${step}Finished" == null && stepStatus.done != NOTHING && !stepStatus.mightDoMore && previousStepFinished) {
                anythingJustCompleted = true
                ticket."${step}Finished" = now
                sendCustomerNotification(ticket, status, step)
                LogThreadLocal.getThreadLog()?.info("sent customer notification for OTRS Ticket ${ticket.ticketNumber}: ${step}")
            }
            if (stepStatus.mightDoMore) {
                mightDoMore = true
            }
        }
        if (anythingJustCompleted) {
            LogThreadLocal.getThreadLog()?.trace("inside if anythingJustCompleted, sending operator notification")
            sendOperatorNotification(ticket, seqTracks, status, !mightDoMore)
            if (!mightDoMore) {
                LogThreadLocal.getThreadLog()?.trace("!mightDoMore: marking as finalNotificationSent")
                ticket.finalNotificationSent = true
            }
            assert ticket.save(flush: true)
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
            if (ticket.automaticNotification) {
                recipients = userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(project)
            }
            StringBuilder subject = new StringBuilder("[${processingOptionService.findOptionAsString(TICKET_SYSTEM_NUMBER_PREFIX)}#${ticket.ticketNumber}] ")
            if (!recipients) {
                subject.append('TO BE SENT: ')
            }
            recipients << processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION)

            if (ilseSubmissions) {
                subject.append("[S#${ilseSubmissions*.ilseNumber.sort().join(',')}] ")
            }
            subject.append("${project.name} sequencing data ${notificationStep.notificationSubject}")

            String content = createNotificationTextService.notification(ticket, status, notificationStep, project)
            mailHelperService.sendEmail(subject.toString(), content, recipients)
        }
    }

    void sendOperatorNotification(OtrsTicket ticket, Set<SeqTrack> seqTracks, ProcessingStatus status, boolean finalNotification) {
        StringBuilder subject = new StringBuilder()

        String prefix = processingOptionService.findOptionAsString(TICKET_SYSTEM_NUMBER_PREFIX)
        subject.append("$prefix#").append(ticket.ticketNumber)
        if (finalNotification) {
            subject.append(' Final')
        }
        subject.append(' Processing Status Update')

        List<String> recipients = []

        List projects = seqTracks*.project.unique()

        if (finalNotification && projects.size() == 1 && projects.first().customFinalNotification) {
            List ilseSubmissions = seqTracks*.ilseSubmission.findAll().unique()
            if (ilseSubmissions) {
                subject.append(" [S#${ilseSubmissions*.ilseNumber.sort().join(',')}]")
            }
            List individuals = seqTracks*.individual.findAll().unique()
            subject.append(" ${individuals*.pid.sort().join(', ')} ")
            List seqTypes = seqTracks*.seqType.findAll().unique()
            subject.append("(${seqTypes*.displayName.sort().join(', ')})")
            if (ticket.automaticNotification) {
                recipients = userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(projects)
            }
        }
        recipients << processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION)

        StringBuilder content = new StringBuilder()
        content.append(status.toString())
        content.append('\n').append(seqTracks.size()).append(' SeqTrack(s) in ticket ').append(ticket.ticketNumber).append(':\n')
        for (SeqTrack seqTrack : seqTracks.sort { a, b -> a.ilseId <=> b.ilseId ?: a.run.name <=> b.run.name ?: a.laneId <=> b.laneId }) {
            appendSeqTrackString(content, seqTrack)
            content.append('\n')
        }
        mailHelperService.sendEmail(subject.toString(), content.toString(), recipients)
    }

    void appendSeqTrackString(StringBuilder sb, SeqTrack seqTrack) {
        if (seqTrack.ilseId) {
            sb.append("ILSe ${seqTrack.ilseId}, ")
        }
        sb.append("${seqTrack.run.name}, lane ${seqTrack.laneId}, ${seqTrack.project.name}, ${seqTrack.individual.pid}, " +
                "${seqTrack.sampleType.name}, ${seqTrack.seqType.name} ${seqTrack.seqType.libraryLayout}")
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void assignOtrsTicketToRunSegment(String ticketNumber, Long runSegmentId) {
        RunSegment runSegment = RunSegment.get(runSegmentId)
        assert runSegment: "No RunSegment found for ${runSegmentId}."

        OtrsTicket oldOtrsTicket = runSegment.otrsTicket

        if (oldOtrsTicket && oldOtrsTicket.ticketNumber == ticketNumber) {
            return
        }

        if (OtrsTicket.ticketNumberConstraint(ticketNumber)) {
            throw new UserException("Ticket number ${ticketNumber} does not pass validation or error while saving. " +
                    "An OTRS ticket must consist of 16 successive digits.")
        }

        // assigning a runSegment that belongs to an otrsTicket which consists of several other runSegments is not allowed,
        // because it is not possible to calculate the right "Started"/"Finished" dates
        if (oldOtrsTicket && oldOtrsTicket.runSegments.size() != 1) {
            throw new UserException("Assigning a runSegment that belongs to an OTRS-Ticket which consists of several other runSegments is not allowed.")
        }

        OtrsTicket newOtrsTicket = CollectionUtils.atMostOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber)) ?:
                createOtrsTicket(ticketNumber, null, true)

        if (newOtrsTicket.finalNotificationSent) {
            throw new UserException("It is not allowed to assign to an finally notified OTRS-Ticket.")
        }

        runSegment.otrsTicket = newOtrsTicket
        assert runSegment.save(flush: true, failOnError: true)

        ProcessingStatus status = getProcessingStatus(newOtrsTicket.findAllSeqTracks())
        for (OtrsTicket.ProcessingStep step : OtrsTicket.ProcessingStep.values()) {
            WorkflowProcessingStatus stepStatus = status."${step}ProcessingStatus"
            if (stepStatus.mightDoMore) {
                newOtrsTicket."${step}Finished" = null
            } else {
                newOtrsTicket."${step}Finished" = [oldOtrsTicket?."${step}Finished", newOtrsTicket."${step}Finished"].max()
            }
            newOtrsTicket."${step}Started" = [oldOtrsTicket?."${step}Started", newOtrsTicket."${step}Started"].min()
        }

        assert newOtrsTicket.save(flush: true, failOnError: true)
    }

    ProcessingStatus getProcessingStatus(Collection<SeqTrack> seqTracks, SamplePairCreation samplePairCreation = new SamplePairCreation()) {
        ProcessingStatus status = new ProcessingStatus(seqTracks.collect {
            new SeqTrackProcessingStatus(it,
                    it.dataInstallationState == SeqTrack.DataProcessingState.FINISHED ? ALL_DONE : NOTHING_DONE_MIGHT_DO,
                    it.fastqcState == SeqTrack.DataProcessingState.FINISHED ? ALL_DONE : NOTHING_DONE_MIGHT_DO, [])
        })
        List<MergingWorkPackageProcessingStatus> allMwpStatuses = fillInMergingWorkPackageProcessingStatuses(status.seqTrackProcessingStatuses)
        fillInSamplePairStatuses(allMwpStatuses, samplePairCreation)

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


    void fillInSamplePairStatuses(Collection<MergingWorkPackageProcessingStatus> mwpStatuses, SamplePairCreation samplePairCreation) {
        if (mwpStatuses.isEmpty()) {
            return
        }

        samplePairCreation.createMissingDiseaseControlSamplePairs()

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

    static def <O> WorkflowProcessingStatus combineStatuses(Iterable<O> objects,
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

    static class SamplePairCreation {

        boolean calledCreateMissingDiseaseControlSamplePairs = false

        void createMissingDiseaseControlSamplePairs() {
            if (!calledCreateMissingDiseaseControlSamplePairs) {
                final Collection<SamplePair> samplePairs = SamplePair.findMissingDiseaseControlSamplePairs()
                samplePairs*.save(flush: true)
                calledCreateMissingDiseaseControlSamplePairs = true
            }
        }
    }
}
