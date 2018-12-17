package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.monitor.PipelinesChecker
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

class AllRoddyAlignmentsChecker extends PipelinesChecker<SeqTrack> {


    static final String HEADER_NOT_SUPPORTED_SEQTYPES =
            'The following SeqTypes are unsupported by any Roddy alignment workflow supported by OTP'


    @Override
    List handle(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        if (!seqTracks) {
            return []
        }

        seqTracks.unique()
        Map<SeqType, List<SeqTrack>> seqTracksBySeqType = seqTracks.groupBy {
            it.seqType
        }

        List<AbstractRoddyAlignmentChecker> checkers = [
                new PanCanAlignmentChecker(),
                new WgbsRoddyAlignmentChecker(),
                new RnaRoddyAlignmentChecker(),
        ]

        Map<AbstractRoddyAlignmentChecker, List<SeqTrack>> seqTracksPerChecker = checkers.collectEntries { AbstractRoddyAlignmentChecker checker ->
            [
                    (checker): checker.seqTypes.collect { SeqType seqType ->
                        seqTracksBySeqType.remove(seqType)
                    }.flatten().unique().findAll()
            ]
        }

        List<SeqTrack> unsupportedSeqTracks = seqTracksBySeqType.values().flatten()

        output.showUniqueList(HEADER_NOT_SUPPORTED_SEQTYPES, unsupportedSeqTracks) { SeqTrack seqTrack ->
            "${seqTrack.seqType.displayNameWithLibraryLayout}"
        }

        return seqTracksPerChecker.collect { AbstractRoddyAlignmentChecker checker, List<SeqTrack> seqTrackList ->
            checker.handle(seqTrackList, output)
        }.flatten().findAll()
    }
}
