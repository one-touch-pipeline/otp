package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.monitor.*
import de.dkfz.tbi.otp.ngsdata.*

class PanCanAlignmentChecker extends AbstractRoddyAlignmentChecker {

    static final String HEADER_EXOME_WITHOUT_LIBRARY_PREPERATION_KIT =
            'The following SeqTracks have no library preperation kit'

    static final String HEADER_EXOME_NO_BEDFILE =
            'The following SeqTracks have no bedfile kit'

    @Override
    String getWorkflowName() {
        return 'PanCanWorkflow'
    }

    @Override
    Pipeline.Name getPipeLineName() {
        return Pipeline.Name.PANCAN_ALIGNMENT
    }

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqTypeService.wholeGenomePairedSeqType, SeqTypeService.exomePairedSeqType, SeqTypeService.chipSeqPairedSeqType]
    }

    @Override
    List<SeqTrack> filter(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        String libraryPreperationKitMissing = 'libraryPreperationKitMissing'
        String bedFileMissing = 'bedFileMissing'
        String ok = 'ok'

        Map groupedSeqTracks = seqTracks.groupBy { SeqTrack seqTrack ->
            if (seqTrack.seqType.isExome()) {
                if (!seqTrack.libraryPreparationKit) {
                    return libraryPreperationKitMissing
                }
                ReferenceGenome referenceGenome = seqTrack.configuredReferenceGenome
                BedFile bedFile = BedFile.findByReferenceGenomeAndLibraryPreparationKit(referenceGenome, seqTrack.libraryPreparationKit)
                if (!bedFile) {
                    return bedFileMissing
                }
            }
            return ok
        }
        output.showList(HEADER_EXOME_WITHOUT_LIBRARY_PREPERATION_KIT, groupedSeqTracks[libraryPreperationKitMissing])
        output.showList(HEADER_EXOME_NO_BEDFILE, groupedSeqTracks[bedFileMissing])

        return groupedSeqTracks[ok]
    }
}
