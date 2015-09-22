package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.SavingException
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils

import org.codehaus.groovy.grails.web.json.JSONObject

import grails.converters.JSON

class AbstractQualityAssessmentService {

    AbstractBamFileService abstractBamFileService

    ProcessedBamFileQaFileService processedBamFileQaFileService

    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    ReferenceGenomeService referenceGenomeService

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
                assert qualityAssessmentStatistics.chromosomeName == chromosome
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
                assert qualityAssessmentStatistics.chromosomeName == chromosome
            }
            qualityAssessmentStatistics.percentIncorrectPEorientation = safePercentCalculation(qualityAssessmentStatistics.referenceAgreementStrandConflict, qualityAssessmentStatistics.referenceAgreement)
            qualityAssessmentStatistics.percentReadPairsMapToDiffChrom = safePercentCalculation(qualityAssessmentStatistics.endReadAberration, qualityAssessmentStatistics.totalMappedReadCounter)
            qualityAssessmentStatistics.qualityAssessmentMergedPass = qualityAssessmentPass
            assertSave(qualityAssessmentStatistics)
        }
    }

    void assertListContainsAllChromosomeNamesInReferenceGenome(Collection<String> chromosomeNames, ReferenceGenome referenceGenome) {
        Collection<String> expectedChromosomeNames = [RoddyQualityAssessment.ALL] +
                referenceGenomeService.chromosomesInReferenceGenome(referenceGenome)*.name
        if (!CollectionUtils.containSame(chromosomeNames, expectedChromosomeNames)) {
            throw new RuntimeException("Expected chromosomes ${expectedChromosomeNames}, but found ${chromosomeNames}.")
        }
    }

    void parseRoddySingleLaneQaStatistics(RoddyBamFile roddyBamFile) {
        Map<SeqTrack, File> qaFilesPerSeqTrack
         if (roddyBamFile.isOldStructureUsed()) {
            //TODO: OTP-1734 delete the if part
            qaFilesPerSeqTrack = roddyBamFile.getTmpRoddySingleLaneQAJsonFiles()
        } else {
             qaFilesPerSeqTrack = roddyBamFile.getWorkSingleLaneQAJsonFiles()
        }
        qaFilesPerSeqTrack.each { seqTrack, qaFile ->
            JSONObject json = JSON.parse(qaFile.text)
            Iterator chromosomes = json.keys()
            Collection<String> allChromosomeNames = []
            chromosomes.each { String chromosome ->
                allChromosomeNames.add(chromosome)
                RoddySingleLaneQa singleLaneQa = new RoddySingleLaneQa(json.get(chromosome))
                assert singleLaneQa.chromosome == chromosome
                singleLaneQa.seqTrack = seqTrack
                singleLaneQa.qualityAssessmentMergedPass = roddyBamFile.findOrSaveQaPass()
                assert singleLaneQa.save(flush: true)
            }
            assertListContainsAllChromosomeNamesInReferenceGenome(allChromosomeNames, roddyBamFile.referenceGenome)
        }
    }


    void parseRoddyBamFileQaStatistics(RoddyBamFile roddyBamFile) {
        File qaFile
        if (roddyBamFile.isOldStructureUsed()) {
            //TODO: OTP-1734 delete the if part
            qaFile = roddyBamFile.getTmpRoddyMergedQAJsonFile()
        } else {
            qaFile = roddyBamFile.getWorkMergedQAJsonFile()
        }
        JSONObject json = JSON.parse(qaFile.text)
        Iterator chromosomes = json.keys()
        Collection<String> allChromosomeNames = []
        chromosomes.each { String chromosome ->
            allChromosomeNames.add(chromosome)
            RoddyMergedBamQa mergedQa = new RoddyMergedBamQa(json.get(chromosome))
            assert mergedQa.chromosome == chromosome
            mergedQa.qualityAssessmentMergedPass = roddyBamFile.findOrSaveQaPass()
            assert mergedQa.save(flush: true)
        }
        assertListContainsAllChromosomeNamesInReferenceGenome(allChromosomeNames, roddyBamFile.referenceGenome)
    }

    void saveCoverageToRoddyBamFile(RoddyBamFile roddyBamFile) {
        RoddyMergedBamQa mergedQa = roddyBamFile.overallQualityAssessment
        roddyBamFile.coverage = mergedQa.genomeWithoutNCoverageQcBases
        roddyBamFile.coverageWithN = abstractBamFileService.calculateCoverageWithN(roddyBamFile)
        assert roddyBamFile.save(flush: true)
    }


    void saveCoverageToProcessedBamFile(QualityAssessmentPass qualityAssessmentPass) {
        ProcessedBamFile processedBamFile = qualityAssessmentPass.processedBamFile
        processedBamFile.coverage = abstractBamFileService.calculateCoverageWithoutN(processedBamFile)
        processedBamFile.coverageWithN = abstractBamFileService.calculateCoverageWithN(processedBamFile)
        assertSave(processedBamFile)
    }

    void saveCoverageToAbstractMergedBamFile(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        AbstractMergedBamFile abstractMergedBamFile = qualityAssessmentMergedPass.abstractMergedBamFile
        abstractMergedBamFile.coverage = abstractBamFileService.calculateCoverageWithoutN(abstractMergedBamFile)
        abstractMergedBamFile.coverageWithN = abstractBamFileService.calculateCoverageWithN(abstractMergedBamFile)
        assertSave(abstractMergedBamFile)
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
