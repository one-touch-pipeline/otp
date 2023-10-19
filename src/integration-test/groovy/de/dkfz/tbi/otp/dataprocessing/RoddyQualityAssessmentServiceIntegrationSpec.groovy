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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

import java.nio.file.Path

@Rollback
@Integration
class RoddyQualityAssessmentServiceIntegrationSpec extends Specification implements DomainFactoryCore, IsRoddy {

    RoddyQualityAssessmentService roddyQualityAssessmentService

    final static REFERENCE_GENOME_LENGTH = 80
    final static REFERENCE_GENOME_LENGTH_WITH_N = 40
    final static long SOME_VALUE_1 = 1111111
    final static long SOME_VALUE_2 = 2222222
    final static long SOME_VALUE_3 = 3333333
    final static long SOME_VALUE_4 = 4444444

    @TempDir
    Path temporaryFolder

    void setupData() {
        createReferenceGenome(
                length: REFERENCE_GENOME_LENGTH_WITH_N,
                lengthWithoutN: REFERENCE_GENOME_LENGTH,
        )
        DomainFactory.createAllAlignableSeqTypes()
    }

    private RoddyBamFile setupForParseRoddyQaStatistics(SeqType seqType) {
        setupData()
        RoddyBamFile roddyBamFile = createBamFile(
                workPackage: createMergingWorkPackage(
                        seqType: seqType,
                        pipeline: DomainFactory.createPanCanPipeline(),
                )
        )
        ['7', '8'].each {
            DomainFactory.createReferenceGenomeEntry(
                    referenceGenome: roddyBamFile.referenceGenome,
                    classification: Classification.CHROMOSOME,
                    name: it,
            )
        }

        Path workMergedQAJson = temporaryFolder.resolve("workMergedQA.json")
        Path workMergedQATargetExtractJson = temporaryFolder.resolve("workMergedQATargetExtract.json")
        Path workSingleLaneQAJson = temporaryFolder.resolve("workSingleLaneQA.json")
        Path workLibraryQAJson = temporaryFolder.resolve("workLibraryQA.json")
        workMergedQAJson.text = DomainFactory.getQaFileContent([chromosome8QcBasesMapped: SOME_VALUE_1])
        workMergedQATargetExtractJson.text = DomainFactory.getQaFileContent([chromosome8QcBasesMapped: SOME_VALUE_2])
        workSingleLaneQAJson.text = DomainFactory.getQaFileContent([chromosome8QcBasesMapped: SOME_VALUE_3])
        workLibraryQAJson.text = DomainFactory.getQaFileContent([chromosome8QcBasesMapped: SOME_VALUE_4])

        roddyQualityAssessmentService = new RoddyQualityAssessmentService()
        roddyQualityAssessmentService.referenceGenomeService = new ReferenceGenomeService()
        roddyQualityAssessmentService.roddyBamFileService = Mock(RoddyBamFileService) {
            getWorkMergedQAJsonFile(_) >> workMergedQAJson
            getWorkMergedQATargetExtractJsonFile(_) >> workMergedQATargetExtractJson
            getWorkSingleLaneQAJsonFiles(_) >> { RoddyBamFile bamFile -> [(bamFile.seqTracks.first()): workSingleLaneQAJson] }
            getWorkLibraryQAJsonFiles(_) >> { RoddyBamFile bamFile -> [(bamFile.seqTracks.first().libraryDirectoryName): workLibraryQAJson] }
        }

        assert RoddyMergedBamQa.list().empty
        assert RoddySingleLaneQa.list().empty
        assert RoddyLibraryQa.list().empty
        return roddyBamFile
    }

    private void checkForParseRoddyQaStatistics(Class<? extends RoddyQualityAssessment> qaClass, Long qcBasesMappedExpected, Long allBasesMappedExpected, Long onTargetMappedBasesExpected) {
        Collection<RoddyQualityAssessment> qas = RoddyQualityAssessment.list()
        assert qas*.class.every { it.simpleName == qaClass.simpleName }
        TestCase.assertContainSame(qas*.chromosome, ["8", "all", "7"])
        assert qas.find { it.chromosome == '8' }.qcBasesMapped == qcBasesMappedExpected
        assert qas.find { it.chromosome == '8' }.allBasesMapped == allBasesMappedExpected
        assert qas.find { it.chromosome == '8' }.onTargetMappedBases == onTargetMappedBasesExpected
    }

    void "test parseRoddyMergedBamQaStatistics, whole genome"() {
        given:
        RoddyBamFile roddyBamFile = setupForParseRoddyQaStatistics(DomainFactory.createWholeGenomeSeqType())

        when:
        roddyQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)

        then:
        checkForParseRoddyQaStatistics(RoddyMergedBamQa, SOME_VALUE_1, null, null)
    }

    void "test parseRoddyMergedBamQaStatistics, exome"() {
        given:
        RoddyBamFile roddyBamFile = setupForParseRoddyQaStatistics(DomainFactory.createExomeSeqType())

        when:
        roddyQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)

        then:
        checkForParseRoddyQaStatistics(RoddyMergedBamQa, null, SOME_VALUE_1, SOME_VALUE_2)
    }

    void "test parseRoddyQaStatistics, missing chromosome, should fail"() {
        given:
        RoddyBamFile roddyBamFile = setupForParseRoddyQaStatistics(DomainFactory.createWholeGenomeSeqType())
        DomainFactory.createReferenceGenomeEntry(
                referenceGenome: roddyBamFile.referenceGenome,
                classification: Classification.CHROMOSOME,
                name: '9',
                alias: '9',
        )

        when:
        roddyQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)

        then:
        AssertionError e = thrown(AssertionError)
        e.message =~ /^Missed chromosomes: .+ \(expected: .*; found .*\).*/
    }

    void "test parseRoddySingleLaneQaStatistics, whole genome"() {
        given:
        RoddyBamFile roddyBamFile = setupForParseRoddyQaStatistics(DomainFactory.createWholeGenomeSeqType())

        when:
        roddyQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)

        then:
        checkForParseRoddyQaStatistics(RoddySingleLaneQa, SOME_VALUE_3, null, null)
    }

    void "test parseRoddySingleLaneQaStatistics, exome"() {
        given:
        RoddyBamFile roddyBamFile = setupForParseRoddyQaStatistics(DomainFactory.createExomeSeqType())

        when:
        roddyQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)

        then:
        checkForParseRoddyQaStatistics(RoddySingleLaneQa, null, SOME_VALUE_3, null)
    }

    void "test parseRoddyLibraryQaStatistics, WGBS"() {
        given:
        RoddyBamFile roddyBamFile = setupForParseRoddyQaStatistics(DomainFactory.createWholeGenomeBisulfiteSeqType())

        when:
        roddyQualityAssessmentService.parseRoddyLibraryQaStatistics(roddyBamFile)

        then:
        checkForParseRoddyQaStatistics(RoddyLibraryQa, SOME_VALUE_4, null, null)
    }

    void "test assertListContainsAllChromosomeNamesInReferenceGenome, lists are the same, should be fine"() {
        given:
        setupData()
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3']
        ReferenceGenome referenceGenome = createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        when:
        roddyQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)

        then:
        noExceptionThrown()
    }

    void "test assertListContainsAllChromosomeNamesInReferenceGenome, OTP list is subset of Roddy list, should be fine"() {
        given:
        setupData()
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3', '4', '5']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3']
        ReferenceGenome referenceGenome = createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        when:
        roddyQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)

        then:
        noExceptionThrown()
    }

    void "test assertListContainsAllChromosomeNamesInReferenceGenome, OTP list is bigger than Roddy list, should fail"() {
        given:
        setupData()
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3', '4', '5']
        ReferenceGenome referenceGenome = createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        when:
        roddyQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)

        then:
        AssertionError e = thrown(AssertionError)
        e.message =~ /^Missed chromosomes: .+ \(expected: .*; found .*\).*/
    }

    void "test saveCoverageToRoddyBamFile, should calculate and set coverage"() {
        given:
        setupData()
        final QC_BASES_MAPPED = 320
        final EXPECTED_COVERAGE = 4
        final EXPECTED_COVERAGE_WITH_N = 8

        RoddyBamFile roddyBamFile = createBamFile()
        final Long ARBITRARY_UNUSED_VALUE = 1
        RoddyMergedBamQa mergedQa = new RoddyMergedBamQa([
                abstractBamFile: roddyBamFile,
                qcBasesMapped : QC_BASES_MAPPED,
                genomeWithoutNCoverageQcBases: EXPECTED_COVERAGE,
                chromosome: RoddyQualityAssessment.ALL,
                insertSizeCV: 123,
                percentageMatesOnDifferentChr: 0.123,
                totalReadCounter: ARBITRARY_UNUSED_VALUE,
                qcFailedReads: ARBITRARY_UNUSED_VALUE,
                duplicates: ARBITRARY_UNUSED_VALUE,
                totalMappedReadCounter: ARBITRARY_UNUSED_VALUE,
                pairedInSequencing: ARBITRARY_UNUSED_VALUE,
                pairedRead2: ARBITRARY_UNUSED_VALUE,
                pairedRead1: ARBITRARY_UNUSED_VALUE,
                properlyPaired: ARBITRARY_UNUSED_VALUE,
                withItselfAndMateMapped: ARBITRARY_UNUSED_VALUE,
                withMateMappedToDifferentChr: ARBITRARY_UNUSED_VALUE,
                withMateMappedToDifferentChrMaq: ARBITRARY_UNUSED_VALUE,
                singletons: ARBITRARY_UNUSED_VALUE,
                insertSizeMedian: ARBITRARY_UNUSED_VALUE,
                insertSizeSD: ARBITRARY_UNUSED_VALUE,
                referenceLength: ARBITRARY_UNUSED_VALUE,
        ])
        assert mergedQa.save(flush: true)

        roddyBamFile.referenceGenome.length = REFERENCE_GENOME_LENGTH_WITH_N
        roddyBamFile.referenceGenome.lengthWithoutN = REFERENCE_GENOME_LENGTH
        assert roddyBamFile.referenceGenome.save(flush: true)

        when:
        roddyQualityAssessmentService.saveCoverageToRoddyBamFile(roddyBamFile)

        then:
        roddyBamFile.coverage == EXPECTED_COVERAGE
        roddyBamFile.coverageWithN == EXPECTED_COVERAGE_WITH_N
    }
}
