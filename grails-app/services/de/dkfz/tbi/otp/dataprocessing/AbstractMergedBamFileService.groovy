package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

import static org.springframework.util.Assert.*

class AbstractMergedBamFileService {

    /**
     * @deprecated Use bamFile.baseDirectory instead, which can also handle chipseq directories
     * @param bamFile, the mergedBamFile which has to be copied
     * @return the final directory of the mergedBamFile after copying
     */
    @Deprecated
    static String destinationDirectory (AbstractMergedBamFile bamFile) {
        notNull(bamFile, "the input of the method destinationDirectory is null")
        return bamFile.baseDirectory.absolutePath + '/'
    }

    void setSamplePairStatusToNeedProcessing(AbstractMergedBamFile finishedBamFile) {
        assert finishedBamFile: "The input bam file must not be null"
        SamplePair.createCriteria().list {
            or {
                eq('mergingWorkPackage1', finishedBamFile.workPackage)
                eq('mergingWorkPackage2', finishedBamFile.workPackage)
            }
        }.each { SamplePair samplePair ->
            SeqType seqType = samplePair.seqType
            if (samplePair.snvProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED && SeqTypeService.snvPipelineSeqTypes.contains(seqType)) {
                samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            if (samplePair.indelProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED && SeqTypeService.indelPipelineSeqTypes.contains(seqType)) {
                samplePair.indelProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            if (samplePair.aceseqProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED  && SeqTypeService.aceseqPipelineSeqTypes.contains(seqType)) {
                samplePair.aceseqProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            if (samplePair.sophiaProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED && SeqTypeService.sophiaPipelineSeqTypes.contains(seqType)) {
                samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            if (samplePair.runYapsaProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED && SeqTypeService.runYapsaPipelineSeqTypes.contains(seqType)) {
                samplePair.runYapsaProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            assert samplePair.save(flush: true)
        }
    }

    File getExistingBamFilePath(final AbstractMergedBamFile bamFile) {
        final File file = bamFile.getPathForFurtherProcessing()
        assert bamFile.getMd5sum() ==~ /^[0-9a-f]{32}$/
        assert bamFile.getFileSize() > 0L
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        assert file.length() == bamFile.getFileSize()
        return file
    }
}
