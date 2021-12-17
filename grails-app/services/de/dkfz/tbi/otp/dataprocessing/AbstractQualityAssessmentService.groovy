/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONObject

import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

import static de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification.CONTIG
import static de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification.UNDEFINED

@Transactional
class AbstractQualityAssessmentService {

    AbstractBamFileService abstractBamFileService

    ReferenceGenomeService referenceGenomeService

    void assertListContainsAllChromosomeNamesInReferenceGenome(Collection<String> chromosomeNames, ReferenceGenome referenceGenome) {
        Collection<String> expectedChromosomeNames = [RoddyQualityAssessment.ALL] +
                referenceGenomeService.chromosomesInReferenceGenome(referenceGenome)*.name
        Collection<String> missedElements = expectedChromosomeNames.findAll { String chromosomeName ->
            !chromosomeNames.contains(chromosomeName)
        }
        assert !missedElements: "Missed chromosomes: ${missedElements.join(', ')} (expected: ${expectedChromosomeNames}; found ${chromosomeNames})."
    }

    private Map<String, Map> parseRoddyQaStatistics(RoddyBamFile roddyBamFile, File qualityControlJsonFile, Closure qualityControlTargetExtractJsonFile) {
        Map<String, Map> chromosomeInformation = [:]

        boolean isExome = roddyBamFile.seqType.seqTypeName == SeqTypeNames.EXOME
        JSONObject qualityControlJson = JSON.parse(qualityControlJsonFile.text)
        JSONObject qualityControlTargetExtractJson
        if (isExome && qualityControlTargetExtractJsonFile != null) {
            qualityControlTargetExtractJson = JSON.parse(qualityControlTargetExtractJsonFile().text)
        }
        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(
                roddyBamFile.referenceGenome, [CONTIG, UNDEFINED])*.name
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
        if (roddyBamFile instanceof RnaRoddyBamFile) {
            assert chromosomeInformation.keySet().size() == 1 && chromosomeInformation.keySet().contains(RnaQualityAssessment.ALL)
        } else {
            assertListContainsAllChromosomeNamesInReferenceGenome(allChromosomeNames, roddyBamFile.referenceGenome)
        }
        return chromosomeInformation
    }

    void parseRoddySingleLaneQaStatistics(RoddyBamFile roddyBamFile) {
        Map<SeqTrack, File> qaFilesPerSeqTrack = roddyBamFile.workSingleLaneQAJsonFiles
        qaFilesPerSeqTrack.each { seqTrack, qaFile ->
            Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(roddyBamFile, qaFile, null)
            chromosomeInformation.each { chromosome, chromosomeValues ->
                RoddySingleLaneQa qa = new RoddySingleLaneQa(handleNaValue(chromosomeValues))
                qa.seqTrack = seqTrack
                assert qa.chromosome == chromosome
                qa.qualityAssessmentMergedPass = roddyBamFile.findOrSaveQaPass()
                assert qa.save(flush: true)
            }
        }
    }

    RoddyMergedBamQa parseRoddyMergedBamQaStatistics(RoddyBamFile roddyBamFile) {
        File qaFile = roddyBamFile.workMergedQAJsonFile
        Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(roddyBamFile, qaFile, { roddyBamFile.workMergedQATargetExtractJsonFile })

        List<RoddyMergedBamQa> chromosomeInformationQa = chromosomeInformation.collect { chromosome, chromosomeValues ->
            RoddyMergedBamQa qa = new RoddyMergedBamQa(handleNaValue(chromosomeValues))
            assert qa.chromosome == chromosome
            qa.qualityAssessmentMergedPass = roddyBamFile.findOrSaveQaPass()
            assert qa.save(flush: true)
            return qa
        }
        return chromosomeInformationQa.find {
            it.chromosome == RoddyQualityAssessment.ALL
        }
    }

    private Map handleNaValue(Map map) {
        if (map.containsKey('percentageMatesOnDifferentChr') && map.percentageMatesOnDifferentChr == 'NA') {
            map.percentageMatesOnDifferentChr = null
        }
        return map
    }

    RnaQualityAssessment parseRnaRoddyBamFileQaStatistics(RnaRoddyBamFile rnaRoddyBamFile) {
        File qaFile = rnaRoddyBamFile.workMergedQAJsonFile
        Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(rnaRoddyBamFile, qaFile, null)
        RnaQualityAssessment rnaQualityAssessment = new RnaQualityAssessment((chromosomeInformation.get(RnaQualityAssessment.ALL)))
        rnaQualityAssessment.chromosome = RnaQualityAssessment.ALL
        rnaQualityAssessment.qualityAssessmentMergedPass = rnaRoddyBamFile.findOrSaveQaPass()
        assert rnaQualityAssessment.save(flush: true)
        return rnaQualityAssessment
    }

    void parseRoddyLibraryQaStatistics(RoddyBamFile roddyBamFile) {
        Map<String, File> qaFilesPerLibrary = roddyBamFile.workLibraryQAJsonFiles

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
}
