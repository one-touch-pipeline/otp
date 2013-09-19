package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*

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
}
