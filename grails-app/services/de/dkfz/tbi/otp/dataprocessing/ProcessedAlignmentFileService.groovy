package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * This service implements alignment files organization convention
 *
 *
 */
class ProcessedAlignmentFileService {

    def dataProcessingFilesService

    public String getDirectory(AlignmentPass alignmentPass) {
        Individual ind = alignmentPass.seqTrack.sample.individual
        def dirType = DataProcessingFilesService.OutputDirectories.ALIGNMENT
        String baseDir = dataProcessingFilesService.getOutputDirectory(ind, dirType)
        String middleDir = getRunLaneDirectory(alignmentPass.seqTrack)
        return "${baseDir}/${middleDir}/${alignmentPass.getDirectory()}"
    }

    public String getRunLaneDirectory(SeqTrack seqTrack) {
        String runName = seqTrack.run.name
        String lane = seqTrack.laneId
        return "${runName}_${lane}"
    }
}
