package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.SavingException
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames

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
        Collection<String> missedElements = expectedChromosomeNames.findAll { String chromosomeName ->
            !chromosomeNames.contains(chromosomeName)
        }
        if (!missedElements.isEmpty()) {
            throw new RuntimeException("Missed chromosomes: ${missedElements.join(', ')} (expected: ${expectedChromosomeNames}; found ${chromosomeNames}).")
        }
    }

    private void parseRoddyQaStatistics(RoddyBamFile roddyBamFile, SeqTrack seqTrack, File qualityControlJsonFile, Closure qualityControlTargetExtractJsonFile) {
        boolean isExome = roddyBamFile.seqType.seqTypeName == SeqTypeNames.EXOME
        JSONObject qualityControlJson = JSON.parse(qualityControlJsonFile.text)
        JSONObject qualityControlTargetExtractJson
        if (isExome && qualityControlTargetExtractJsonFile != null) {
            qualityControlTargetExtractJson = JSON.parse(qualityControlTargetExtractJsonFile().text)
        }
        Iterator chromosomes = qualityControlJson.keys()
        Collection<String> allChromosomeNames = []
        chromosomes.each { String chromosome ->
            allChromosomeNames.add(chromosome)
            Map chromosomeValues = qualityControlJson.get(chromosome)
            if (isExome) {
                chromosomeValues = new HashMap(chromosomeValues)
                Object allBasesMapped = chromosomeValues.remove('qcBasesMapped')
                assert allBasesMapped != null
                Object previousAllBasesMapped = chromosomeValues.put('allBasesMapped', allBasesMapped)
                assert previousAllBasesMapped == null
                if (qualityControlTargetExtractJson != null) {
                    Object onTargetMappedBases = qualityControlTargetExtractJson.get(chromosome).get('qcBasesMapped')
                    assert onTargetMappedBases != null
                    Object previousOnTargetMappedBases = chromosomeValues.put('onTargetMappedBases', onTargetMappedBases)
                    assert previousOnTargetMappedBases == null
                }
            }
            RoddyQualityAssessment qa
            if (seqTrack) {
                qa = new RoddySingleLaneQa(chromosomeValues)
                qa.seqTrack = seqTrack
            } else {
                qa = new RoddyMergedBamQa(chromosomeValues)
            }
            assert qa.chromosome == chromosome
            qa.qualityAssessmentMergedPass = roddyBamFile.findOrSaveQaPass()
            assert qa.save(flush: true)
        }
        assertListContainsAllChromosomeNamesInReferenceGenome(allChromosomeNames, roddyBamFile.referenceGenome)
    }

    void parseRoddySingleLaneQaStatistics(RoddyBamFile roddyBamFile) {
        Map<SeqTrack, File> qaFilesPerSeqTrack = roddyBamFile.getWorkSingleLaneQAJsonFiles()
        qaFilesPerSeqTrack.each { seqTrack, qaFile ->
            parseRoddyQaStatistics(roddyBamFile, seqTrack, qaFile, null)
        }
    }


    void parseRoddyMergedBamQaStatistics(RoddyBamFile roddyBamFile) {
        File qaFile = roddyBamFile.getWorkMergedQAJsonFile()
        parseRoddyQaStatistics(roddyBamFile, null, qaFile, { roddyBamFile.workMergedQATargetExtractJsonFile })
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
