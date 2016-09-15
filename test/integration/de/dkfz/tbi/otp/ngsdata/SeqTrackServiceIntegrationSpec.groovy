package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import grails.test.spock.*
import spock.lang.*


class SeqTrackServiceIntegrationSpec extends IntegrationSpec {

    @Unroll
    void "test seqTracksReadyToInstall"() {
        given:
        SeqTrackService seqTrackService = new SeqTrackService()

        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        DomainFactory.createSeqTrack(
                seqType: seqType,
                dataInstallationState: state,
                laneId: "1"
        )

        SeqTrack seqTrack2 = DomainFactory.createSeqTrack(
                seqType: seqType,
                dataInstallationState: state,
                laneId: "2",

        )
        seqTrack2.project.processingPriority = priority

        when:
        List<String> laneIds = seqTrackService.seqTracksReadyToInstall(inputPriority)*.laneId

        then:
        expectedLaneIds == laneIds

        where:
        state                                    | priority                               | inputPriority                          || expectedLaneIds
        SeqTrack.DataProcessingState.UNKNOWN     | ProcessingPriority.NORMAL_PRIORITY     | ProcessingPriority.NORMAL_PRIORITY     || []
        SeqTrack.DataProcessingState.NOT_STARTED | ProcessingPriority.NORMAL_PRIORITY     | ProcessingPriority.NORMAL_PRIORITY     || ["1", "2"]
        SeqTrack.DataProcessingState.NOT_STARTED | ProcessingPriority.FAST_TRACK_PRIORITY | ProcessingPriority.NORMAL_PRIORITY     || ["2", "1"]
        SeqTrack.DataProcessingState.NOT_STARTED | ProcessingPriority.FAST_TRACK_PRIORITY | ProcessingPriority.FAST_TRACK_PRIORITY || ["2"]

    }
}
