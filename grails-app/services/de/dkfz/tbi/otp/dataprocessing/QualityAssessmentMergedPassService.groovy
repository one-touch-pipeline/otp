package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.notNull
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class QualityAssessmentMergedPassService {

    ConfigService configService
    ProcessingOptionService processingOptionService

    public QualityAssessmentMergedPass createPass() {
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.findByQualityAssessmentStatusAndTypeAndWithdrawn(
                        AbstractBamFile.QaProcessingStatus.NOT_STARTED, AbstractBamFile.BamType.MDUP, false)
        if (!processedMergedBamFile) {
            return null
        }
        int numberOfpass = QualityAssessmentMergedPass.countByProcessedMergedBamFile(processedMergedBamFile)
        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass(identifier: numberOfpass, processedMergedBamFile: processedMergedBamFile)
        assertSave(qualityAssessmentMergedPass)
        return qualityAssessmentMergedPass
    }

    public void passStarted(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        update(qualityAssessmentMergedPass, AbstractBamFile.QaProcessingStatus.IN_PROGRESS)
    }

    public void passFinished(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        update(qualityAssessmentMergedPass, AbstractBamFile.QaProcessingStatus.FINISHED)
    }

    private void update(QualityAssessmentMergedPass qualityAssessmentMergedPass, AbstractBamFile.QaProcessingStatus state) {
        ProcessedMergedBamFile processedMergedBamFile = qualityAssessmentMergedPass.processedMergedBamFile
        processedMergedBamFile.qualityAssessmentStatus = state
        assertSave(processedMergedBamFile)
    }

    public Realm realmForDataProcessing(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        return configService.getRealmDataProcessing(project(qualityAssessmentMergedPass))
    }

    public Project project(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        return qualityAssessmentMergedPass.processedMergedBamFile.mergingPass.mergingSet.mergingWorkPackage.sample.individual.project
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
     * @return a sorted List (by identifier, in descending order) of all QualityAssessmentPass, which were created for this ProcessedMergedBamFile
     */
    public List<QualityAssessmentMergedPass> allQualityAssessmentMergedPasses(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the input for the method allQualityAssessmentPasses is null")
        return QualityAssessmentMergedPass.findAllByProcessedMergedBamFile(bamFile, [sort: "id", order: "desc"])
    }

    /**
     * @param bamFile, for which the qa results were calculated
     * @return the latest QualityAssessmentPass which was created for this ProcessedMergedBamFile
     */
    public QualityAssessmentMergedPass latestQualityAssessmentMergedPass(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the input for the method latestQualityAssessmentPasses is null")
        //the output of allQualityAssessmentPasses needs to be sorted in descending order
        return allQualityAssessmentMergedPasses(bamFile).first()
    }
}
