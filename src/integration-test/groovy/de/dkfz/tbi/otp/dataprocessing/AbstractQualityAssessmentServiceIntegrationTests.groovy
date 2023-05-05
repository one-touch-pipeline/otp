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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class AbstractQualityAssessmentServiceIntegrationTests {

    AbstractQualityAssessmentService abstractQualityAssessmentService
    TestConfigService configService

    final static REFERENCE_GENOME_LENGTH = 80
    final static REFERENCE_GENOME_LENGTH_WITH_N = 40
    final static long SOME_VALUE_1 = 1111111
    final static long SOME_VALUE_2 = 2222222
    final static long SOME_VALUE_3 = 3333333
    final static long SOME_VALUE_4 = 4444444

    TestData data = new TestData()

    @SuppressWarnings("PublicInstanceField") // must be public in JUnit tests
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    void setupData() {
        data.createObjects()

        data.referenceGenome.with {
            length = REFERENCE_GENOME_LENGTH_WITH_N
            lengthWithoutN = REFERENCE_GENOME_LENGTH
        }
        assert data.referenceGenome.save([flush: true])
        DomainFactory.createAllAlignableSeqTypes()
        configService.addOtpProperties(temporaryFolder.newFolder().toPath())
    }

    @After
    void tearDown() {
        configService.clean()
    }

    @Test
    void test_saveCoverageToRoddyBamFile_ShouldCalculateAndSetCoverage() {
        setupData()
        final QC_BASES_MAPPED = 320
        final EXPECTED_COVERAGE = 4
        final EXPECTED_COVERAGE_WITH_N = 8

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        final Long ARBITRARY_UNUSED_VALUE = 1
        RoddyMergedBamQa mergedQa = new RoddyMergedBamQa([
                qualityAssessmentMergedPass : DomainFactory.createQualityAssessmentMergedPass(abstractBamFile: roddyBamFile),
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

        abstractQualityAssessmentService.saveCoverageToRoddyBamFile(roddyBamFile)

        assert roddyBamFile.coverage == EXPECTED_COVERAGE
        assert roddyBamFile.coverageWithN == EXPECTED_COVERAGE_WITH_N
    }

    RoddyBamFile setUpForParseRoddyQaStatistics(SeqType seqType) {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile(
                workPackage: DomainFactory.createMergingWorkPackage(
                        seqType: seqType,
                        pipeline: DomainFactory.createPanCanPipeline(),
                )
        )
        createReferenceGenomeEntries(roddyBamFile.referenceGenome)
        DomainFactory.createQaFileOnFileSystem(roddyBamFile.workMergedQAJsonFile, SOME_VALUE_1)
        DomainFactory.createQaFileOnFileSystem(roddyBamFile.workMergedQATargetExtractJsonFile, SOME_VALUE_2)
        DomainFactory.createQaFileOnFileSystem(exactlyOneElement(roddyBamFile.workSingleLaneQAJsonFiles.values()), SOME_VALUE_3)
        DomainFactory.createQaFileOnFileSystem(exactlyOneElement(roddyBamFile.workLibraryQAJsonFiles.values()), SOME_VALUE_4)
        assert RoddyMergedBamQa.list().empty
        assert RoddySingleLaneQa.list().empty
        assert RoddyLibraryQa.list().empty
        return roddyBamFile
    }

    private void testParseRoddyMergedBamQaStatistics_allFine(String mergedBamOrSingleLane, SeqType seqType, Long qcBasesMappedExpected, Long allBasesMappedExpected, Long onTargetMappedBasesExpected) {
        RoddyBamFile roddyBamFile = setUpForParseRoddyQaStatistics(seqType)

        abstractQualityAssessmentService."parseRoddy${mergedBamOrSingleLane}QaStatistics"(roddyBamFile)

        Collection<RoddyQualityAssessment> qas = RoddyQualityAssessment.list()
        assert TestCase.containSame(qas*.class*.simpleName.unique(), ["Roddy${mergedBamOrSingleLane}Qa"])
        assert TestCase.containSame(qas*.chromosome, ["8", "all", "7"])
        assert qas.find { it.chromosome == '8' }.qcBasesMapped == qcBasesMappedExpected
        assert qas.find { it.chromosome == '8' }.allBasesMapped == allBasesMappedExpected
        assert qas.find { it.chromosome == '8' }.onTargetMappedBases == onTargetMappedBasesExpected
    }

    @Test
    void testParseRoddyMergedBamQaStatistics_WholeGenome_allFine() {
        setupData()
        testParseRoddyMergedBamQaStatistics_allFine("MergedBam", DomainFactory.createWholeGenomeSeqType(), SOME_VALUE_1, null, null)
    }

    @Test
    void testParseRoddyMergedBamQaStatistics_Exome_allFine() {
        setupData()
        testParseRoddyMergedBamQaStatistics_allFine("MergedBam", DomainFactory.createExomeSeqType(), null, SOME_VALUE_1, SOME_VALUE_2)
    }

    @Test
    void testParseRoddyQaStatistics_missingChromosome() {
        setupData()
        RoddyBamFile roddyBamFile = setUpForParseRoddyQaStatistics(DomainFactory.createWholeGenomeSeqType())
        DomainFactory.createReferenceGenomeEntry(
                referenceGenome: roddyBamFile.referenceGenome,
                classification: Classification.CHROMOSOME,
                name: '9',
                alias: '9',
        )
        TestCase.shouldFailWithMessage(AssertionError, /^Missed chromosomes: .+ \(expected: .*; found .*\).*/, {
            abstractQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)
        })
    }

    @Test
    void testParseRoddySingleLaneQaStatistics_WholeGenome_allFine() {
        setupData()
        testParseRoddyMergedBamQaStatistics_allFine("SingleLane", DomainFactory.createWholeGenomeSeqType(), SOME_VALUE_3, null, null)
    }

    @Test
    void testParseRoddySingleLaneQaStatistics_Exome_allFine() {
        setupData()
        testParseRoddyMergedBamQaStatistics_allFine("SingleLane", DomainFactory.createExomeSeqType(), null, SOME_VALUE_3, null)
    }

    @Test
    void testParseRoddyLibraryQaStatistics_WGBS_allFine() {
        setupData()
        testParseRoddyMergedBamQaStatistics_allFine("Library", DomainFactory.createWholeGenomeBisulfiteSeqType(), SOME_VALUE_4, null, null)
    }

    @Test
    void testAssertListContainsAllChromosomeNamesInReferenceGenome_listAreSame_shouldBeFine() {
        setupData()
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3']
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        abstractQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)
    }

    @Test
    void testAssertListContainsAllChromosomeNamesInReferenceGenome_OtpListIsSubsetOfRoddyList_shouldBeFine() {
        setupData()
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3', '4', '5']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3']
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        abstractQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)
    }

    @Test
    void testAssertListContainsAllChromosomeNamesInReferenceGenome_OtpListIsBiggerThenRoddyList_shouldFail() {
        setupData()
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3', '4', '5']
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        TestCase.shouldFailWithMessage(AssertionError, /^Missed chromosomes: .+ \(expected: .*; found .*\).*/, {
            abstractQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)
        })
    }

    static void createReferenceGenomeEntries(ReferenceGenome referenceGenome) {
        ['7', '8'].each {
            DomainFactory.createReferenceGenomeEntry(
                    referenceGenome: referenceGenome,
                    classification: Classification.CHROMOSOME,
                    name: it,
            )
        }
    }
}
