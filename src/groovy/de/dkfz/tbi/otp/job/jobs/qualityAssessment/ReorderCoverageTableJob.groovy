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
     * ENUM for the chromosome names
     */
    /*
     public enum Chromosomes {
     CHROMOSOME_1(1), CHROMOSOME_2(2), CHROMOSOME_3(3), CHROMOSOME_4(4), CHROMOSOME_5(5), CHROMOSOME_6(6), CHROMOSOME_7(7),
     CHROMOSOME_8(8), CHROMOSOME_9(9), CHROMOSOME_10(10), CHROMOSOME_11(11), CHROMOSOME_12(12), CHROMOSOME_13(13), CHROMOSOME_14(14),
     CHROMOSOME_15(15), CHROMOSOME_16(16), CHROMOSOME_17(17), CHROMOSOME_18(18), CHROMOSOME_19(19), CHROMOSOME_20(20), CHROMOSOME_21(21),
     CHROMOSOME_22(22), CHROMOSOME_X("X"), CHROMOSOME_Y("Y"), CHROMOSOME_M("M"), CHROMOSOME_ASTERISK("*"),
     CHROMOSOME_ALL([1..22, "X", "Y", "M", "*"]), CHROMOSOME_NUMERIC(1..22), CHROMOSOME_CHARACTER(["X", "Y", "M", "*"])
     }
     */

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
