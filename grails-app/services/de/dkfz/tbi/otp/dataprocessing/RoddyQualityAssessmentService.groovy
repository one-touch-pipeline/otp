/*
 * Copyright 2011-2024 The OTP authors
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
import groovy.transform.CompileDynamic
import org.grails.web.json.JSONObject

import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification.CONTIG
import static de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification.UNDEFINED

@CompileDynamic
@Transactional
class RoddyQualityAssessmentService {

    AbstractBamFileService abstractBamFileService
    RoddyBamFileService roddyBamFileService
    RnaRoddyBamFileService rnaRoddyBamFileService
    ReferenceGenomeService referenceGenomeService

    void parseRoddySingleLaneQaStatistics(RoddyBamFile roddyBamFile) {
        Map<SeqTrack, Path> qaFilesPerSeqTrack = roddyBamFileService.getWorkSingleLaneQAJsonFiles(roddyBamFile)
        qaFilesPerSeqTrack.each { seqTrack, qaFile ->
            Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(roddyBamFile, qaFile)
            chromosomeInformation.each { chromosome, chromosomeValues ->
                RoddySingleLaneQa qa = CollectionUtils.atMostOneElement(RoddySingleLaneQa.findAllByAbstractBamFileAndChromosomeAndSeqTrack(roddyBamFile, chromosome, seqTrack))
                if (qa) {
                    qa.properties = chromosomeValues
                } else {
                    qa = new RoddySingleLaneQa(chromosomeValues)
                    qa.abstractBamFile = roddyBamFile
                    qa.seqTrack = seqTrack
                }
                assert qa.chromosome == chromosome
                assert qa.save(flush: true)
            }
        }
    }

    RoddyMergedBamQa parseRoddyMergedBamQaStatistics(RoddyBamFile roddyBamFile) {
        Path qaFile = roddyBamFileService.getWorkMergedQAJsonFile(roddyBamFile)
        Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(roddyBamFile, qaFile,
                roddyBamFileService.getWorkMergedQATargetExtractJsonFile(roddyBamFile))

        List<RoddyMergedBamQa> chromosomeInformationQa = chromosomeInformation.collect { chromosome, chromosomeValues ->
            RoddyMergedBamQa qa = CollectionUtils.atMostOneElement(RoddyMergedBamQa.findAllByAbstractBamFileAndChromosome(roddyBamFile, chromosome))
            if (qa) {
                qa.properties = handleNaValue(chromosomeValues)
            } else {
                qa = new RoddyMergedBamQa(handleNaValue(chromosomeValues))
                qa.abstractBamFile = roddyBamFile
            }
            assert qa.chromosome == chromosome
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

    void parseRoddyLibraryQaStatistics(RoddyBamFile roddyBamFile) {
        Map<String, Path> qaFilesPerLibrary = roddyBamFileService.getWorkLibraryQAJsonFiles(roddyBamFile)

        qaFilesPerLibrary.each { lib, qaFile ->
            Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(roddyBamFile, qaFile)
            chromosomeInformation.each { chromosome, chromosomeValues ->
                RoddyLibraryQa qa = CollectionUtils.atMostOneElement(RoddyLibraryQa.findAllByAbstractBamFileAndChromosomeAndLibraryDirectoryName(roddyBamFile, chromosome, lib))
                if (qa) {
                    qa.properties = chromosomeValues
                } else {
                    qa = new RoddyLibraryQa(chromosomeValues)
                    qa.abstractBamFile = roddyBamFile
                    qa.libraryDirectoryName = lib
                }
                assert qa.chromosome == chromosome
                assert qa.save(flush: true)
            }
        }
    }

    RnaQualityAssessment parseRnaRoddyBamFileQaStatistics(RnaRoddyBamFile rnaRoddyBamFile) {
        Path qaFile = rnaRoddyBamFileService.getWorkMergedQAJsonFile(rnaRoddyBamFile)
        Map<String, Map> chromosomeInformation = parseRoddyQaStatistics(rnaRoddyBamFile, qaFile)
        RnaQualityAssessment qa = CollectionUtils.atMostOneElement(RnaQualityAssessment.findAllByAbstractBamFile(rnaRoddyBamFile))
        if (qa) {
            qa.properties = (chromosomeInformation.get(RnaQualityAssessment.ALL))
        } else {
            qa = new RnaQualityAssessment((chromosomeInformation.get(RnaQualityAssessment.ALL)))
            qa.abstractBamFile = rnaRoddyBamFile
            qa.chromosome = RnaQualityAssessment.ALL
        }
        assert qa.chromosome == RnaQualityAssessment.ALL
        assert qa.save(flush: true)
        return qa
    }

    private Map<String, Map> parseRoddyQaStatistics(RoddyBamFile roddyBamFile, Path qualityControlJsonFile, Path qualityControlTargetExtractJsonFile = null) {
        Map<String, Map> chromosomeInformation = [:]

        JSONObject qualityControlJson = JSON.parse(qualityControlJsonFile.text)
        JSONObject qualityControlTargetExtractJson
        if (roddyBamFile.seqType.needsBedFile && qualityControlTargetExtractJsonFile != null) {
            qualityControlTargetExtractJson = JSON.parse(qualityControlTargetExtractJsonFile.text)
        }
        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(
                roddyBamFile.referenceGenome, [CONTIG, UNDEFINED])*.name
        Iterator chromosomes = qualityControlJson.keys()
        Collection<String> allChromosomeNames = []
        chromosomes.findAll { !chromosomeNames.contains(it) } .each { String chromosome ->
            allChromosomeNames.add(chromosome)
            Map chromosomeValues = qualityControlJson.get(chromosome)
            if (roddyBamFile.seqType.needsBedFile) {
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

    protected void assertListContainsAllChromosomeNamesInReferenceGenome(Collection<String> chromosomeNames, ReferenceGenome referenceGenome) {
        Collection<String> expectedChromosomeNames = [RoddyQualityAssessment.ALL] +
                referenceGenomeService.chromosomesInReferenceGenome(referenceGenome)*.name
        Collection<String> missedElements = expectedChromosomeNames.findAll { String chromosomeName ->
            !chromosomeNames.contains(chromosomeName)
        }
        assert !missedElements: "Missed chromosomes: ${missedElements.join(', ')} (expected: ${expectedChromosomeNames}; found ${chromosomeNames})."
    }

    void saveCoverageToRoddyBamFile(RoddyBamFile roddyBamFile) {
        RoddyMergedBamQa mergedQa = roddyBamFile.qualityAssessment
        roddyBamFile.coverage = mergedQa.genomeWithoutNCoverageQcBases
        roddyBamFile.coverageWithN = abstractBamFileService.calculateCoverageWithN(roddyBamFile)
        assert roddyBamFile.save(flush: true)
    }
}
