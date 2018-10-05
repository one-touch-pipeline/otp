package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*

import static org.springframework.util.Assert.notNull

class QualityAssessmentMergedPassService {

    ConfigService configService
    ProcessingOptionService processingOptionService

    QualityAssessmentMergedPass createPass(ProcessingPriority minPriority) {
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.createCriteria().get {
            eq ("qualityAssessmentStatus", AbstractBamFile.QaProcessingStatus.NOT_STARTED)
            eq ("type", AbstractBamFile.BamType.MDUP)
            eq ("withdrawn", false)

            mergingPass {
                mergingSet {
                    mergingWorkPackage {
                        sample {
                            individual {
                                project {
                                    ge('processingPriority', minPriority.priority)
                                    order("processingPriority", "desc")
                                }
                            }
                        }
                    }
                }
            }
            order("id", "asc")
            maxResults(1)
        }


        if (!processedMergedBamFile) {
            return null
        }
        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass(
                abstractMergedBamFile: processedMergedBamFile,
                identifier: QualityAssessmentMergedPass.nextIdentifier(processedMergedBamFile),
        )
        assertSave(qualityAssessmentMergedPass)
        return qualityAssessmentMergedPass
    }

    void passStarted(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        notNull(qualityAssessmentMergedPass, "The input qualityAssessmentMergedPass of the method passStarted is null")
        update(qualityAssessmentMergedPass, AbstractBamFile.QaProcessingStatus.IN_PROGRESS)
    }

    /**
     * When the quality assessment of the merged bam files is done, the qaProcessingStatus is set to finished.
     * Furthermore the fileOperationStatus is set to NEEDS_PROCESSING to trigger the Transfer-Workflow
     */
    void passFinished(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        notNull(qualityAssessmentMergedPass, "The input qualityAssessmentMergedPass of the method passFinished is null")
        update(qualityAssessmentMergedPass, AbstractBamFile.QaProcessingStatus.FINISHED)
        qualityAssessmentMergedPass.abstractMergedBamFile.updateFileOperationStatus(AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING)
        assert qualityAssessmentMergedPass.abstractMergedBamFile.save(flush: true)
    }

    private void update(QualityAssessmentMergedPass qualityAssessmentMergedPass, AbstractBamFile.QaProcessingStatus state) {
        notNull(qualityAssessmentMergedPass, "The input qualityAssessmentMergedPass of the method update is null")
        notNull(state, "The input state of the method update is null")
        AbstractMergedBamFile abstractMergedBamFile = qualityAssessmentMergedPass.abstractMergedBamFile
        abstractMergedBamFile.qualityAssessmentStatus = state
        assertSave(abstractMergedBamFile)
    }

    Realm realmForDataProcessing(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        notNull(qualityAssessmentMergedPass, "The input qualityAssessmentMergedPass of the method realmForDataProcessing is null")
        return project(qualityAssessmentMergedPass).realm
    }

    Project project(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        notNull(qualityAssessmentMergedPass, "The input qualityAssessmentMergedPass of the method project is null")
        return qualityAssessmentMergedPass.project
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
    List<QualityAssessmentMergedPass> allQualityAssessmentMergedPasses(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the input for the method allQualityAssessmentPasses is null")
        return QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(bamFile, [sort: "id", order: "desc"])
    }

    /**
     * @param bamFile, for which the qa results were calculated
     * @return the latest QualityAssessmentPass which was created for this ProcessedMergedBamFile
     */
    QualityAssessmentMergedPass latestQualityAssessmentMergedPass(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the input for the method latestQualityAssessmentPasses is null")
        //the output of allQualityAssessmentPasses needs to be sorted in descending order
        return allQualityAssessmentMergedPasses(bamFile).first()
    }
}
