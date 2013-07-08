package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*

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
}
