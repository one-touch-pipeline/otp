package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.notNull
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class QualityAssessmentPassService {

    ConfigService configService
    ProcessingOptionService processingOptionService

    public QualityAssessmentPass createPass() {
        ProcessedBamFile processedBamFile = ProcessedBamFile.findByQualityAssessmentStatusAndType(AbstractBamFile.QaProcessingStatus.NOT_STARTED, AbstractBamFile.BamType.SORTED)
        if (!processedBamFile) {
            return null
        }
        int numberOfpass = QualityAssessmentPass.countByProcessedBamFile(processedBamFile)
        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass(identifier: numberOfpass, processedBamFile: processedBamFile)
        assertSave(qualityAssessmentPass)
        return qualityAssessmentPass
    }

    public void passStarted(QualityAssessmentPass qualityAssessmentPass) {
        update(qualityAssessmentPass, AbstractBamFile.QaProcessingStatus.IN_PROGRESS)
    }

    public void passFinished(QualityAssessmentPass qualityAssessmentPass) {
        update(qualityAssessmentPass, AbstractBamFile.QaProcessingStatus.FINISHED)
    }

    private void update(QualityAssessmentPass qualityAssessmentPass, AbstractBamFile.QaProcessingStatus state) {
        ProcessedBamFile processedBamFile = qualityAssessmentPass.processedBamFile
        processedBamFile.qualityAssessmentStatus = state
        assertSave(processedBamFile)
    }

    public Realm realmForDataProcessing(QualityAssessmentPass qualityAssessmentPass) {
        return configService.getRealmDataProcessing(project(qualityAssessmentPass))
    }

    public Project project(QualityAssessmentPass qualityAssessmentPass) {
        return qualityAssessmentPass.processedBamFile.alignmentPass.seqTrack.sample.individual.project
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
}
