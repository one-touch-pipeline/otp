package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*
import org.codehaus.groovy.grails.web.json.*

import static de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification.*


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

    private Map<String, Map> parseRoddyQaStatistics(RoddyBamFile roddyBamFile, File qualityControlJsonFile, Closure qualityControlTargetExtractJsonFile) {
        Map<String, Map> chromosomeInformation = [:]

        boolean isExome = roddyBamFile.seqType.seqTypeName == SeqTypeNames.EXOME
        JSONObject qualityControlJson = JSON.parse(qualityControlJsonFile.text)
        JSONObject qualityControlTargetExtractJson
        if (isExome && qualityControlTargetExtractJsonFile != null) {
            qualityControlTargetExtractJson = JSON.parse(qualityControlTargetExtractJsonFile().text)
        }
        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(roddyBamFile.referenceGenome, [CONTIG, UNDEFINED])*.name
        Iterator chromosomes = qualityControlJson.keys()
        Collection<String> allChromosomeNames = []
        chromosomes.findAll { !chromosomeNames.contains(it) } .each { String chromosome ->
            allChromosomeNames.add(chromosome)
            Map chromosomeValues = qualityControlJson.get(chromosome)
            if (isExome) {
                chromosomeValues = new HashMap(chromosomeValues)
                Object allBasesMapped = chromosomeValues.remove('qcBasesMapped')
                assert allBasesMapped != null
                Object previousAllBasesMapped = chromosomeValues.put('allBasesMapped', allBasesMapped)
                assert previousAllBasesMapped == null
                if (qualityControlTargetExtractJson != null) {
                    chromosomeValues.remove('genomeWithoutNCoverageQcBases')
                    chromosomeValues.put('genomeWithoutNCoverageQcBases', qualityControlTargetExtractJson.get(chromosome).get('genomeWithoutNCoverageQcBases'))
                    Object onTargetMappedBases = qualityControlTargetExtractJson.get(chromosome).get('qcBasesMapped')
                    assert onTargetMappedBases != null
                    Object previousOnTargetMappedBases = chromosomeValues.put('onTargetMappedBases', onTargetMappedBases)
                    assert previousOnTargetMappedBases == null
                }
            }
            chromosomeInformation.put(chromosome, chromosomeValues)
        }
        assertListContainsAllChromosomeNamesInReferenceGenome(allChromosomeNames, roddyBamFile.referenceGenome)
        return chromosomeInformation
    }

    void parseRoddySingleLaneQaStatistics(RoddyBamFile roddyBamFile) {
        Map<SeqTrack, File> qaFilesPerSeqTrack = roddyBamFile.getWorkSingleLaneQAJsonFiles()
        qaFilesPerSeqTrack.each { seqTrack, qaFile ->
            Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(roddyBamFile, qaFile, null)
            chromosomeInformation.each { chromosome, chromosomeValues ->
                RoddySingleLaneQa qa = new RoddySingleLaneQa(chromosomeValues)
                qa.seqTrack = seqTrack
                assert qa.chromosome == chromosome
                qa.qualityAssessmentMergedPass = roddyBamFile.findOrSaveQaPass()
                assert qa.save(flush: true)
            }
        }
    }


    void parseRoddyMergedBamQaStatistics(RoddyBamFile roddyBamFile) {
        File qaFile = roddyBamFile.getWorkMergedQAJsonFile()
        Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(roddyBamFile, qaFile, { roddyBamFile.workMergedQATargetExtractJsonFile })

        chromosomeInformation.each { chromosome, chromosomeValues ->
            RoddyMergedBamQa qa = new RoddyMergedBamQa(chromosomeValues)
            assert qa.chromosome == chromosome
            qa.qualityAssessmentMergedPass = roddyBamFile.findOrSaveQaPass()
            assert qa.save(flush: true)
        }
    }

    void parseRoddyLibraryQaStatistics(RoddyBamFile roddyBamFile) {
        Map<String, File> qaFilesPerLibrary = roddyBamFile.getWorkLibraryQAJsonFiles()

        qaFilesPerLibrary.each { lib, qaFile ->
            Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(roddyBamFile, qaFile, null)
            chromosomeInformation.each { chromosome, chromosomeValues ->
                RoddyLibraryQa qa = new RoddyLibraryQa(chromosomeValues)
                qa.libraryDirectoryName = lib
                assert qa.chromosome == chromosome
                qa.qualityAssessmentMergedPass = roddyBamFile.findOrSaveQaPass()
                assert qa.save(flush: true)
            }
        }
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
