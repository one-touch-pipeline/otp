package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.tracking.ProcessingStatus.Done
import de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import de.dkfz.tbi.otp.user.*
import de.dkfz.tbi.otp.utils.*
import org.hibernate.*
import org.springframework.security.access.prepost.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.*
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

class TrackingService {

    MailHelperService mailHelperService

    IndelCallingService indelCallingService
    SnvCallingService snvCallingService

    CreateNotificationTextService createNotificationTextService


    public static final String TICKET_NUMBER_PREFIX = "otrsTicketNumberPrefix"


    public OtrsTicket createOtrsTicket(String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        OtrsTicket otrsTicket = new OtrsTicket(
                ticketNumber: ticketNumber,
                seqCenterComment: seqCenterComment,
                automaticNotification: automaticNotification,
        )
        assert otrsTicket.save(flush: true, failOnError: true)
        return otrsTicket
    }

    public OtrsTicket createOrResetOtrsTicket(String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        OtrsTicket otrsTicket = CollectionUtils.atMostOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber))
        if (!otrsTicket) {
            return createOtrsTicket(ticketNumber, seqCenterComment, automaticNotification)
        } else {
            otrsTicket.installationFinished = null
            otrsTicket.fastqcFinished = null
            otrsTicket.alignmentFinished = null
            otrsTicket.snvFinished = null
            otrsTicket.finalNotificationSent = false
            otrsTicket.automaticNotification = automaticNotification
            if (!otrsTicket.seqCenterComment) {
                otrsTicket.seqCenterComment = seqCenterComment
            } else if (seqCenterComment && !otrsTicket.seqCenterComment.contains(seqCenterComment)) {
                otrsTicket.seqCenterComment += '\n\n' + seqCenterComment
            }
            assert otrsTicket.save(flush: true, failOnError: true)
            return otrsTicket
        }
    }

    public void setStartedForSeqTracks(Collection<SeqTrack> seqTracks, OtrsTicket.ProcessingStep step) {
        setStarted(findAllOtrsTickets(seqTracks), step)
    }

    public Set<OtrsTicket> findAllOtrsTickets(Collection<SeqTrack> seqTracks) {
        OtrsTicket.withSession { Session session ->
            session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)) //set pessimistic lock for session
            return OtrsTicket.executeQuery("""
                select distinct
                    otrsTicket
                from
                    DataFile datafile
                    join datafile.runSegment.otrsTicket otrsTicket
                where
                    datafile.seqTrack in (:seqTracks)
                    and otrsTicket is not null
            """, [
                    seqTracks: seqTracks
            ]) as Set
        }
    }

    public void setStarted(Collection<OtrsTicket> otrsTickets, OtrsTicket.ProcessingStep step) {
        otrsTickets.unique().each {
            if (it."${step}Started" == null) {
                it."${step}Started" = new Date()
                assert it.save(flush: true)
            }
        }
    }

    public void processFinished(Set<SeqTrack> seqTracks, OtrsTicket.ProcessingStep step) {
        SamplePairDiscovery samplePairDiscovery = new SamplePairDiscovery()
        for (OtrsTicket ticket : findAllOtrsTickets(seqTracks)) {
            setFinishedTimestampsAndNotify(ticket, samplePairDiscovery)
        }
    }

    void setFinishedTimestampsAndNotify(OtrsTicket ticket, SamplePairDiscovery samplePairDiscovery = new SamplePairDiscovery()) {
        if (ticket.finalNotificationSent) {
            return
        }
        Date now = new Date()
        Set<SeqTrack> seqTracks = ticket.findAllSeqTracks()
        ProcessingStatus status = getProcessingStatus(seqTracks, samplePairDiscovery)
        boolean anythingJustCompleted = false
        boolean mightDoMore = false
        for (OtrsTicket.ProcessingStep step : OtrsTicket.ProcessingStep.values()) {
            WorkflowProcessingStatus stepStatus = status."${step}ProcessingStatus"
            if (ticket."${step}Finished" == null && stepStatus.done != NOTHING && !stepStatus.mightDoMore) {
                anythingJustCompleted = true
                ticket."${step}Finished" = now
                sendCustomerNotification(ticket, status, step)
            }
            if (stepStatus.mightDoMore) {
                mightDoMore = true
            }
        }
        if (anythingJustCompleted) {
            sendOperatorNotification(ticket, seqTracks, status, !mightDoMore)
            if (!mightDoMore) {
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

            String mailingList = null
            if (ticket.automaticNotification) {
                mailingList = project.mailingListName
            }

            List<String> recipients = []
            if (mailingList) {
                recipients.add(mailingList)
            }
            recipients.add(mailHelperService.otrsRecipient)

            StringBuilder subject = new StringBuilder("[${ProcessingOptionService.getValueOfProcessingOption(TICKET_NUMBER_PREFIX)}#${ticket.ticketNumber}] ")
            if (!mailingList) {
                subject.append('TO BE SENT: ')
            }
            if (ilseSubmissions) {
                subject.append("[S#${ilseSubmissions*.ilseNumber.sort().join(',')}] ")
            }
            subject.append("${project.name} sequencing data ${notificationStep.notificationSubject}")

            String content = createNotificationTextService.notification(ticket, status, notificationStep)
            mailHelperService.sendEmail(subject.toString(), content, recipients)
        }
    }

    void sendOperatorNotification(OtrsTicket ticket, Set<SeqTrack> seqTracks, ProcessingStatus status, boolean finalNotification) {
        StringBuilder subject = new StringBuilder()

        String prefix = ProcessingOptionService.getValueOfProcessingOption(TICKET_NUMBER_PREFIX)
        subject.append("$prefix#").append(ticket.ticketNumber)
        if (finalNotification) {
            subject.append(' Final')
        }
        subject.append(' Processing Status Update')

        StringBuilder content = new StringBuilder()
        content.append(status.toString())
        content.append('\n').append(seqTracks.size()).append(' SeqTrack(s) in ticket ').append(ticket.ticketNumber).append(':\n')
        for (SeqTrack seqTrack : seqTracks.sort { a, b -> a.ilseId <=> b.ilseId ?: a.run.name <=> b.run.name ?: a.laneId <=> b.laneId }) {
            appendSeqTrackString(content, seqTrack)
            content.append('\n')
        }

        mailHelperService.sendEmail(subject.toString(), content.toString(), mailHelperService.otrsRecipient)
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
        assert runSegment : "No RunSegment found for ${runSegmentId}."

        OtrsTicket oldOtrsTicket = runSegment.otrsTicket

        if (oldOtrsTicket && oldOtrsTicket.ticketNumber == ticketNumber) {
            return
        }

        if (OtrsTicket.ticketNumberConstraint(ticketNumber)) {
            throw new UserException("Ticket number ${ticketNumber} does not pass validation or error while saving. An OTRS ticket must consist of 16 successive digits.")
        }

        // assigning a runSegment that belongs to an otrsTicket which consists of several other runSegments is not allowed,
        // because it is not possible to calculate the right "Started"/"Finished" dates
        if (oldOtrsTicket && oldOtrsTicket.runSegments.size() != 1) {
            throw new UserException("Assigning a runSegment that belongs to an OTRS-Ticket which consists of several other runSegments is not allowed.")
        }

        OtrsTicket newOtrsTicket = CollectionUtils.atMostOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber))
        if (!newOtrsTicket) {
            newOtrsTicket = createOtrsTicket(ticketNumber, null, true)
        }

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

    ProcessingStatus getProcessingStatus(Collection<SeqTrack> seqTracks, SamplePairDiscovery samplePairDiscovery = new SamplePairDiscovery()) {
        ProcessingStatus status = new ProcessingStatus(seqTracks.collect {
            new SeqTrackProcessingStatus(it,
                    it.dataInstallationState == SeqTrack.DataProcessingState.FINISHED ? ALL_DONE : NOTHING_DONE_MIGHT_DO,
                    it.fastqcState == SeqTrack.DataProcessingState.FINISHED ? ALL_DONE : NOTHING_DONE_MIGHT_DO, [])
        })
        fillInMergingWorkPackageProcessingStatuses(status.seqTrackProcessingStatuses, samplePairDiscovery)
        return status
    }

    void fillInMergingWorkPackageProcessingStatuses(Collection<SeqTrackProcessingStatus> seqTrackProcessingStatuses, SamplePairDiscovery samplePairDiscovery) {

        Collection<SeqTrackProcessingStatus> alignableSeqTracks =
                seqTrackProcessingStatuses.findAll { SeqTrackService.mayAlign(it.seqTrack, false) }

        Collection<MergingWorkPackageProcessingStatus> allMwpStatuses = []

        Map<Map, Collection<SeqTrackProcessingStatus>> seqTracksByMergingProperties =
                alignableSeqTracks.groupBy { MergingWorkPackage.getMergingProperties(it.seqTrack) }
        seqTracksByMergingProperties.each { Map mergingProperties, List<SeqTrackProcessingStatus> seqTracksWithProperties ->
            MergingWorkPackage.findAllWhere(mergingProperties).each { MergingWorkPackage mwp ->
                AbstractMergedBamFile bamFile = mwp.completeProcessableBamFileInProjectFolder
                MergingWorkPackageProcessingStatus mwpStatus =
                        new MergingWorkPackageProcessingStatus(mwp, bamFile ? ALL_DONE : NOTHING_DONE_MIGHT_DO, bamFile, [])
                allMwpStatuses.add(mwpStatus)
                seqTracksWithProperties.each {
                    it.mergingWorkPackageProcessingStatuses.add(mwpStatus)
                }
            }
        }

        fillInSamplePairStatuses(allMwpStatuses, samplePairDiscovery)
    }

    void fillInSamplePairStatuses(Collection<MergingWorkPackageProcessingStatus> mwpStatuses, SamplePairDiscovery samplePairDiscovery) {

        if (mwpStatuses.isEmpty()) {
            return
        }

        samplePairDiscovery.createMissingDiseaseControlSamplePairs()

        Map<MergingWorkPackage, MergingWorkPackageProcessingStatus> mwpStatusByMwp =
                mwpStatuses.collectEntries { [it.mergingWorkPackage, it] }

        SamplePair.createCriteria().list {
            or {
                'in'('mergingWorkPackage1', mwpStatusByMwp.keySet())
                'in'('mergingWorkPackage2', mwpStatusByMwp.keySet())
            }
        }.each { SamplePair samplePair ->
            SamplePairProcessingStatus samplePairStatus = getSamplePairProcessingStatus(samplePair)
            [1, 2].each {
                MergingWorkPackageProcessingStatus mwpStatus = mwpStatusByMwp.get(samplePair."mergingWorkPackage${it}")
                if (mwpStatus) {
                    mwpStatus.samplePairProcessingStatuses.add(samplePairStatus)
                }
            }
        }
    }

    private static WorkflowProcessingStatus getAnalysisProcessingStatus(BamFilePairAnalysis analysis, SamplePair sp, BamFileAnalysisService service) {
        WorkflowProcessingStatus status
        if (analysis && !analysis.withdrawn && analysis.processingState == AnalysisProcessingStates.FINISHED && [1, 2].every {
            AbstractMergedBamFile bamFile = analysis."sampleType${it}BamFile"
            return bamFile == bamFile.mergingWorkPackage.completeProcessableBamFileInProjectFolder
        }) {
            status = ALL_DONE
        } else if (analysis && !analysis.withdrawn && BamFileAnalysisService.processingStatesNotProcessable.contains(analysis.processingState)) {
            status = NOTHING_DONE_MIGHT_DO
        } else if ([1, 2].every { sp."mergingWorkPackage${it}".completeProcessableBamFileInProjectFolder }) {
            if (BamFileAnalysisService.ANALYSIS_CONFIG_CLASSES.any {
                service.samplePairForProcessing(ProcessingPriority.MINIMUM_PRIORITY, it, sp)
            }) {
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
        SnvCallingInstance sci = sp.findLatestSnvCallingInstance()
        IndelCallingInstance ici = sp.findLatestIndelCallingInstance()
        WorkflowProcessingStatus snvStatus = getAnalysisProcessingStatus(sci, sp, snvCallingService)
        WorkflowProcessingStatus indelStatus = getAnalysisProcessingStatus(ici, sp, indelCallingService)
        return new SamplePairProcessingStatus(
                sp,
                snvStatus,
                snvStatus == ALL_DONE ? sci : null,
                indelStatus,
                indelStatus == ALL_DONE ? ici : null,
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

    static class SamplePairDiscovery {

        boolean calledCreateMissingDiseaseControlSamplePairs = false

        void createMissingDiseaseControlSamplePairs() {
            if (!calledCreateMissingDiseaseControlSamplePairs) {
                new SamplePairDiscoveryJob().createMissingDiseaseControlSamplePairs()
                calledCreateMissingDiseaseControlSamplePairs = true
            }
        }
    }
}
