package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import static org.springframework.util.Assert.notNull

class AbstractMergedBamFileService {

    /**
     * @param bamFile, the mergedBamFile which has to be copied
     * @return the final directory of the mergedBamFile after copying
     */
    public static String destinationDirectory (AbstractMergedBamFile bamFile) {
        notNull(bamFile, "the input of the method destinationDirectory is null")
        Project project = bamFile.project

        SeqType type = bamFile.seqType
        Sample sample = bamFile.sample

        String root = ConfigService.getProjectRootPath(project)
        String relative = MergedAlignmentDataFileService.buildRelativePath(type, sample)

        return "${root}/${relative}"
    }

    public void setSamplePairStatusToNeedProcessing(AbstractMergedBamFile finishedBamFile) {
        assert finishedBamFile: "The input bam file must not be null"
        SamplePair.createCriteria().list {
            eq('snvProcessingStatus', SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED)
            or {
                eq('mergingWorkPackage1', finishedBamFile.workPackage)
                eq('mergingWorkPackage2', finishedBamFile.workPackage)
            }
        }.each { SamplePair samplePair ->
            samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            samplePair.indelProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
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
