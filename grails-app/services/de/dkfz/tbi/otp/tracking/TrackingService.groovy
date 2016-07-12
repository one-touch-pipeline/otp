package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.ProcessingStatus.Done
import de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.*
import org.springframework.security.access.prepost.PreAuthorize

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.*
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

class TrackingService {

    MailHelperService mailHelperService
    SnvCallingService snvCallingService

    public static final String TICKET_NUMBER_PREFIX = "otrsTicketNumberPrefix"

    public OtrsTicket createOtrsTicket(String ticketNumber) {
        OtrsTicket otrsTicket = new OtrsTicket(ticketNumber: ticketNumber)
        assert otrsTicket.save(flush: true, failOnError: true)
        return otrsTicket
    }

    public OtrsTicket createOrResetOtrsTicket(String ticketNumber) {
        OtrsTicket otrsTicket = CollectionUtils.atMostOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber))
        if (!otrsTicket) {
            return createOtrsTicket(ticketNumber)
        } else {
            otrsTicket.installationFinished = null
            otrsTicket.fastqcFinished = null
            otrsTicket.alignmentFinished = null
            otrsTicket.snvFinished = null
            otrsTicket.finalNotificationSent = false
            assert otrsTicket.save(flush: true, failOnError: true)
            return otrsTicket
        }
    }

    public void setStartedForSeqTracks(Collection<SeqTrack> seqTracks, OtrsTicket.ProcessingStep step) {
        setStarted(findAllOtrsTickets(seqTracks), step)
    }

    public Set<OtrsTicket> findAllOtrsTickets(Collection<SeqTrack> seqTracks) {
        Set<OtrsTicket> otrsTickets = [] as Set
        //Doesn't work as a single Query, probably a Unit test problem
        DataFile.withCriteria {
            'in' ('seqTrack', seqTracks)
            projections {
                distinct('runSegment')
            }
        }.each {
            if (it?.otrsTicket) {
                otrsTickets.add(it.otrsTicket)
            }
        }
        return otrsTickets
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
            }
            if (stepStatus.mightDoMore) {
                mightDoMore = true
            }
        }
        if (anythingJustCompleted) {
            sendNotification(ticket, seqTracks, status, !mightDoMore)
            if (!mightDoMore) {
                ticket.finalNotificationSent = true
            }
            assert ticket.save(flush: true)
        }
    }

    void sendNotification(OtrsTicket ticket, Set<SeqTrack> seqTracks, ProcessingStatus status, boolean finalNotification) {
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
            newOtrsTicket = createOtrsTicket(ticketNumber)
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

    ProcessingStatus getProcessingStatus(Set<SeqTrack> seqTracks, SamplePairDiscovery samplePairDiscovery = new SamplePairDiscovery(), ProcessingStatus status = new ProcessingStatus()) {
        status = getInstallationProcessingStatus(seqTracks, status)
        status = getFastqcProcessingStatus(seqTracks, status)
        status = getAlignmentAndDownstreamProcessingStatus(seqTracks, samplePairDiscovery, status)
        return status
    }

    ProcessingStatus getInstallationProcessingStatus(Set<SeqTrack> seqTracks, ProcessingStatus status = new ProcessingStatus()) {
        status.installationProcessingStatus = combineStatuses(DataFile.findAllBySeqTrackInList(seqTracks as List),
                { DataFile it -> it.fileLinked ? ALL_DONE : NOTHING_DONE_MIGHT_DO })
        return status
    }

    ProcessingStatus getFastqcProcessingStatus(Set<SeqTrack> seqTracks, ProcessingStatus status = new ProcessingStatus()) {
        status.fastqcProcessingStatus = combineStatuses(seqTracks,
                { SeqTrack it -> it.fastqcState == SeqTrack.DataProcessingState.FINISHED ? ALL_DONE : NOTHING_DONE_MIGHT_DO })
        return status
    }

    ProcessingStatus getAlignmentAndDownstreamProcessingStatus(Set<SeqTrack> seqTracks, SamplePairDiscovery samplePairDiscovery, ProcessingStatus status = new ProcessingStatus()) {

        Collection<SeqTrack> alignableSeqTracks = seqTracks.findAll { SeqTrackService.mayAlign(it, false) }
        if (alignableSeqTracks.isEmpty()) {
            status.alignmentProcessingStatus = NOTHING_DONE_WONT_DO
            status.snvProcessingStatus = NOTHING_DONE_WONT_DO
            return status
        }

        boolean atLeastOneWontBeDone = false

        if (alignableSeqTracks.size() < seqTracks.size()) {
            atLeastOneWontBeDone = true
        }

        Map<Map, Collection<SeqTrack>> seqTracksByMergingProperties =
                alignableSeqTracks.groupBy { MergingWorkPackage.getMergingProperties(it) }
        Set<MergingWorkPackage> mergingWorkPackages = seqTracksByMergingProperties.keySet().sum {
            Collection<MergingWorkPackage> mwps = MergingWorkPackage.findAllWhere(it)
            if (mwps.isEmpty()) {
                atLeastOneWontBeDone = true
            }
            return mwps
        } as Set

        status.alignmentProcessingStatus = combineStatuses(mergingWorkPackages,
                { MergingWorkPackage mwp -> mwp.completeProcessableBamFileInProjectFolder ? ALL_DONE : NOTHING_DONE_MIGHT_DO },
                atLeastOneWontBeDone ? NOTHING_DONE_WONT_DO : null
        )

        getSnvProcessingStatus(mergingWorkPackages, atLeastOneWontBeDone, samplePairDiscovery, status)

        return status
    }

    ProcessingStatus getSnvProcessingStatus(Set<MergingWorkPackage> mergingWorkPackages, boolean atLeastOneWontBeDone, SamplePairDiscovery samplePairDiscovery, ProcessingStatus status = new ProcessingStatus()) {

        if (mergingWorkPackages.empty) {
            status.snvProcessingStatus = NOTHING_DONE_WONT_DO
            return status
        }

        samplePairDiscovery.createMissingDiseaseControlSamplePairs()

        Collection<SamplePair> samplePairs = (Collection<SamplePair>) SamplePair.createCriteria().list {
            or {
                'in'('mergingWorkPackage1', mergingWorkPackages)
                'in'('mergingWorkPackage2', mergingWorkPackages)
            }
        }

        if (mergingWorkPackages.any { mwp -> !samplePairs.any { it.mergingWorkPackage1 == mwp || it.mergingWorkPackage2 == mwp }}) {
            atLeastOneWontBeDone = true
        }
        status.snvProcessingStatus = combineStatuses(samplePairs,
                { SamplePair samplePair -> getSnvProcessingStatus(samplePair, samplePairDiscovery) },
                atLeastOneWontBeDone ? NOTHING_DONE_WONT_DO : null
        )
        return status
    }

    WorkflowProcessingStatus getSnvProcessingStatus(SamplePair sp, SamplePairDiscovery samplePairDiscovery) {
        SnvCallingInstance sci = sp.findLatestSnvCallingInstance()
        if (sci) {
            if (sci.processingState == SnvProcessingStates.FINISHED && [1, 2].every {
                AbstractMergedBamFile bamFile = sci."sampleType${it}BamFile"
                return bamFile == bamFile.mergingWorkPackage.completeProcessableBamFileInProjectFolder
            }) {
                return ALL_DONE
            } else if (SnvCallingService.processingStatesNotProcessable.contains(sci.processingState)) {
                return NOTHING_DONE_MIGHT_DO
            }
        }
        if ([1, 2].every { sp."mergingWorkPackage${it}".completeProcessableBamFileInProjectFolder }) {
            samplePairDiscovery.setExistingSamplePairsToNeedsProcessing()
            if (snvCallingService.samplePairForSnvProcessing(ProcessingPriority.MINIMUM_PRIORITY, sp)) {
                return NOTHING_DONE_MIGHT_DO
            } else {
                return NOTHING_DONE_WONT_DO
            }
        } else {
            return NOTHING_DONE_MIGHT_DO
        }
    }

    def <O> WorkflowProcessingStatus combineStatuses(Iterable<O> objects,
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

        boolean calledSetExistingSamplePairsToNeedsProcessing = false

        void setExistingSamplePairsToNeedsProcessing() {
            if (!calledSetExistingSamplePairsToNeedsProcessing) {
                new SamplePairDiscoveryJob().setExistingSamplePairsToNeedsProcessing()
                calledSetExistingSamplePairsToNeedsProcessing = true
            }
        }
    }
}
