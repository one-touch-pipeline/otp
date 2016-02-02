package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.notNull
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.FastqcBasicStatistics
import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.ngsdata.QualityAssessmentException
import grails.orm.HibernateCriteriaBuilder

class QualityAssessmentPassService {

    ConfigService configService
    ProcessingOptionService processingOptionService
    FastqcResultsService fastqcResultsService

    public QualityAssessmentPass createPass() {
        HibernateCriteriaBuilder c = ProcessedBamFile.createCriteria()
        ProcessedBamFile processedBamFile = c.get {
            eq("qualityAssessmentStatus", AbstractBamFile.QaProcessingStatus.NOT_STARTED)
            eq("type", AbstractBamFile.BamType.SORTED)
            eq("withdrawn", false)
            alignmentPass {
                seqTrack {
                    eq("fastqcState", SeqTrack.DataProcessingState.FINISHED)
                }
            }
            maxResults(1)
        }
        if (!processedBamFile) {
            return null
        }
        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass(
                processedBamFile: processedBamFile,
                identifier: QualityAssessmentPass.nextIdentifier(processedBamFile),
        )
        assertSave(qualityAssessmentPass)
        return qualityAssessmentPass
    }

    public void notStarted(ProcessedBamFile bamFile) {
        bamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.NOT_STARTED
        assertSave(bamFile)
    }

    public void passStarted(QualityAssessmentPass qualityAssessmentPass) {
        notNull(qualityAssessmentPass, "The input qualityAssessmentPass of the method passStarted is null")
        update(qualityAssessmentPass, AbstractBamFile.QaProcessingStatus.IN_PROGRESS)
    }

    public void passFinished(QualityAssessmentPass qualityAssessmentPass) {
        notNull(qualityAssessmentPass, "The input qualityAssessmentPass of the method passFinished is null")
        update(qualityAssessmentPass, AbstractBamFile.QaProcessingStatus.FINISHED)
    }

    private void update(QualityAssessmentPass qualityAssessmentPass, AbstractBamFile.QaProcessingStatus state) {
        notNull(qualityAssessmentPass, "The input qualityAssessmentPass of the method update is null")
        notNull(state, "The input state of the method update is null")
        ProcessedBamFile processedBamFile = qualityAssessmentPass.processedBamFile
        processedBamFile.qualityAssessmentStatus = state
        assertSave(processedBamFile)
    }

    public Realm realmForDataProcessing(QualityAssessmentPass qualityAssessmentPass) {
        notNull(qualityAssessmentPass, "The input qualityAssessmentPass of the method realmForDataProcessing is null")
        return configService.getRealmDataProcessing(project(qualityAssessmentPass))
    }

    public Project project(QualityAssessmentPass qualityAssessmentPass) {
        notNull(qualityAssessmentPass, "The input qualityAssessmentPass of the method project is null")
        return qualityAssessmentPass.project
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }

    /**
     * @param bamFile, for which the qa results were calculated
     * @return a sorted List in descending order (by identifier) of all QualityAssessmentPass, which were created for this ProcessedBamFile
     */
    public List<QualityAssessmentPass> allQualityAssessmentPasses(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input for the method allQualityAssessmentPasses is null")
        return QualityAssessmentPass.findAllByProcessedBamFile(bamFile, [sort: "id", order: "desc"])
    }

    /**
     * @param bamFile, for which the qa results were calculated
     * @return the latest QualityAssessmentPass which was created for this ProcessedBamFile
     */
    public QualityAssessmentPass latestQualityAssessmentPass(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input for the method latestQualityAssessmentPasses is null")
        //the output of allQualityAssessmentPasses needs to be sorted in descending order
        return allQualityAssessmentPasses(bamFile).first()
    }

    /**
     * Validates the number of reads from the Bam file against the total read count from the two fastq files (obtained through fastqc)
     *
     * @param qualityAssessmentPass
     * @return <code>true</code>, if the count is the same, <code>false</code> otherwise
     */
    public void assertNumberOfReadsIsTheSameAsCalculatedWithFastqc(QualityAssessmentPass qualityAssessmentPass) throws QualityAssessmentException {
        notNull(qualityAssessmentPass, "the quality assessment pass is null")
        long numberOfReadsInBAM = OverallQualityAssessment.findByQualityAssessmentPass(qualityAssessmentPass).totalReadCounter
        SeqTrack seqTrack = qualityAssessmentPass.processedBamFile.alignmentPass.seqTrack
        long numberOfReadsInFASTQC = seqTrack.getNReads()
        if (numberOfReadsInBAM != numberOfReadsInFASTQC) {
            throw new QualityAssessmentException("Different count of reads for pass ${qualityAssessmentPass} :  Fastqc : ${numberOfReadsInFASTQC} and BAM : ${numberOfReadsInBAM}.")
        }
    }
}
