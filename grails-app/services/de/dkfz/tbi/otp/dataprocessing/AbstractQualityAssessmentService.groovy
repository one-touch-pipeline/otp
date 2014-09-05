package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SavingException
import org.codehaus.groovy.grails.web.json.JSONObject
import grails.converters.JSON

class AbstractQualityAssessmentService {

    AbstractBamFileService abstractBamFileService

    ProcessedBamFileQaFileService processedBamFileQaFileService

    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    void parseQaStatistics(QualityAssessmentPass qualityAssessmentPass) {
        String qualityAssessmentDataFilePath = processedBamFileQaFileService.qualityAssessmentDataFilePath(qualityAssessmentPass)
        File file = new File(qualityAssessmentDataFilePath)
        JSONObject json = JSON.parse(file.text)
        Iterator chromosomes = json.keys()
        AbstractQualityAssessment qualityAssessmentStatistics
        chromosomes.each { String chromosome ->
            if (chromosome == Chromosomes.overallChromosomesLabel()) {
                qualityAssessmentStatistics = new OverallQualityAssessment(json.get(chromosome))
            } else {
                qualityAssessmentStatistics = new ChromosomeQualityAssessment(json.get(chromosome))
            }
            qualityAssessmentStatistics.percentIncorrectPEorientation = safePercentCalculation(qualityAssessmentStatistics.referenceAgreementStrandConflict, qualityAssessmentStatistics.referenceAgreement)
            qualityAssessmentStatistics.percentReadPairsMapToDiffChrom = safePercentCalculation(qualityAssessmentStatistics.endReadAberration, qualityAssessmentStatistics.totalMappedReadCounter)
            qualityAssessmentStatistics.qualityAssessmentPass = qualityAssessmentPass
            assertSave(qualityAssessmentStatistics)
        }
    }

    void parseQaStatistics(QualityAssessmentMergedPass qualityAssessmentPass) {
        String qualityAssessmentDataFilePath = processedMergedBamFileQaFileService.qualityAssessmentDataFilePath(qualityAssessmentPass)
        File file = new File(qualityAssessmentDataFilePath)
        JSONObject json = JSON.parse(file.text)
        Iterator chromosomes = json.keys()
        AbstractQualityAssessment qualityAssessmentStatistics
        chromosomes.each { String chromosome ->
            if (chromosome == Chromosomes.overallChromosomesLabel()) {
                qualityAssessmentStatistics = new OverallQualityAssessmentMerged(json.get(chromosome))
            } else {
                qualityAssessmentStatistics = new ChromosomeQualityAssessmentMerged(json.get(chromosome))
            }
            qualityAssessmentStatistics.percentIncorrectPEorientation = safePercentCalculation(qualityAssessmentStatistics.referenceAgreementStrandConflict, qualityAssessmentStatistics.referenceAgreement)
            qualityAssessmentStatistics.percentReadPairsMapToDiffChrom = safePercentCalculation(qualityAssessmentStatistics.endReadAberration, qualityAssessmentStatistics.totalMappedReadCounter)
            qualityAssessmentStatistics.qualityAssessmentMergedPass = qualityAssessmentPass
            assertSave(qualityAssessmentStatistics)
        }
    }

    void saveCoverageToProcessedBamFile(QualityAssessmentPass qualityAssessmentPass) {
        ProcessedBamFile processedBamFile = qualityAssessmentPass.processedBamFile
        processedBamFile.coverage = abstractBamFileService.calculateCoverageWithoutN(processedBamFile)
        processedBamFile.coverageWithN = abstractBamFileService.calculateCoverageWithN(processedBamFile)
        assertSave(processedBamFile)
    }

    void saveCoverageToProcessedMergedBamFile(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        ProcessedMergedBamFile processedMergedBamFile = qualityAssessmentMergedPass.processedMergedBamFile
        processedMergedBamFile.coverage = abstractBamFileService.calculateCoverageWithoutN(processedMergedBamFile)
        processedMergedBamFile.coverageWithN = abstractBamFileService.calculateCoverageWithN(processedMergedBamFile)
        assertSave(processedMergedBamFile)
    }

    private double safePercentCalculation(double numerator, double denominator) {
        if (denominator == 0) {
            return Double.NaN
        } else {
            return ((numerator / denominator) * 100)
        }
    }

    private void assertSave(def obj) {
        if (!obj.save(flush: true)) {
            throw new SavingException(this.class.name)
        }
    }
}
