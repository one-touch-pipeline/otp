
package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*

@Mock([SeqTypeService])
@Build([SeqTrack])
class SeqTrackServiceUnitTests {


    SeqTrackService seqTrackService

    SeqType alignableSeqType



    void setUp() throws Exception {
        seqTrackService = new SeqTrackService()
        alignableSeqType = DomainFactory.createAlignableSeqTypes().first()
    }



    void testGetSeqTrackReadyForFastqcProcessing_getAll_noReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build(
                        fastqcState: SeqTrack.DataProcessingState.UNKNOWN
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing()
        assert null == ret
    }

    void testGetSeqTrackReadyForFastqcProcessing_getAll_oneReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing()
        assert seqTrack == ret
    }




    void testGetSeqTrackReadyForFastqcProcessing_getAlignable_noReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.UNKNOWN,
                        seqType: alignableSeqType
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing(true)
        assert null == ret
    }

    void testGetSeqTrackReadyForFastqcProcessing_getAlignable_onlyNotAlignableSeqTracksAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing(true)
        assert null == ret
    }

    void testGetSeqTrackReadyForFastqcProcessing_getAlignable_withReadyAlignableSeqTrack() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                        seqType: alignableSeqType
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing(true)
        assert seqTrack == ret
    }



    void testGetSeqTrackReadyForFastqcProcessingPreferAlignable_NoReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.UNKNOWN
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessingPreferAlignable()
        assert null == ret
    }

    void testGetSeqTrackReadyForFastqcProcessingPreferAlignable_SeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessingPreferAlignable()
        assert seqTrack == ret
    }

    void testGetSeqTrackReadyForFastqcProcessingPreferAlignable_TakeFirstAlignableSeqTrack() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )
        seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                        seqType: alignableSeqType
                        )
        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessingPreferAlignable()
        assert seqTrack == ret
    }

    void testGetSeqTrackReadyForFastqcProcessingPreferAlignable_TakeOlderSeqTrack() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )
        SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )
        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessingPreferAlignable()
        assert seqTrack == ret
    }

}
