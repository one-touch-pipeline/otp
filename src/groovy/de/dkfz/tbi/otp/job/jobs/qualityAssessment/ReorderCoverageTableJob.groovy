package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import org.springframework.beans.factory.annotation.Autowired

class ReorderCoverageTableJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ChromosomeIdentifierProcessingService chromosomeIdentifierProcessingService

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    /**
     * Method which calls several services to
     * -map the reference genome identifiers for the chromosomes to general identifiers
     * -to sort the identifier of the chromosomes
     * -to filter the identifier
     * -to write the filtered and sorted coverage data to a file
     */
    @Override
    public void execute() throws Exception {
        long processedBamFileId = Long.parseLong(getProcessParameterValue())
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(processedBamFileId)
        chromosomeIdentifierProcessingService.execute(processedBamFile)
    }
}
