package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import static org.springframework.util.Assert.notNull

/**
 */
class AbstractMergedBamFileService {

    ConfigService configService

    MergedAlignmentDataFileService mergedAlignmentDataFileService

    /**
     * @param bamFile, the mergedBamFile which has to be copied
     * @return the final directory of the mergedBamFile after copying
     */
    public String destinationDirectory (AbstractMergedBamFile bamFile) {
        notNull(bamFile, "the input of the method destinationDirectory is null")
        Project project = bamFile.project

        SeqType type = bamFile.seqType
        Sample sample = bamFile.sample

        String root = configService.getProjectRootPath(project)
        String relative = mergedAlignmentDataFileService.buildRelativePath(type, sample)

        return "${root}/${relative}"
    }
}
