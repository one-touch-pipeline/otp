package de.dkfz.tbi.ngstools.qualityAssessment

import org.junit.*
import groovy.mock.interceptor.StubFor
import net.sf.samtools.*
import de.dkfz.tbi.ngstools.bedUtils.*

class SAMCountingStatisticWorkerUnitTests extends GroovyTestCase {

    private ChromosomeStatistic chromosome

    private SAMCountingStatisticWorker samCountingStatisticWorker

    private SAMRecord record

    private SAMRecord exomeRecord

    private Parameters parameters

    private FileParameters fileParameters

    @Before
    public void setUp() throws Exception {
        chromosome = new ChromosomeStatistic("TEST", 0)
        parameters = new Parameters(winSize: 10, mappingQuality: 5, coverageMappingQualityThreshold: 5, minAlignedRecordLength: 16, minMeanBaseQuality: 4)
        record = new SAMRecord()
        record.setReadPairedFlag(true) //need to be true for read firstOfPairFlag
        exomeRecord = new SAMRecord()
        exomeRecord.alignmentStart = 2l
        exomeRecord.cigarString = "10M"
        exomeRecord.readUnmappedFlag = false
        exomeRecord.mappingQuality = 10
        exomeRecord.duplicateReadFlag = false
        exomeRecord.baseQualityString = "9999999999"
        fileParameters = new FileParameters()
        samCountingStatisticWorker = new SAMCountingStatisticWorker()
        samCountingStatisticWorker.setParameters(parameters)
        samCountingStatisticWorker.setFileParameters(fileParameters)
    }

    @After
    public void tearDown() throws Exception {
        chromosome = null
        samCountingStatisticWorker = null
        record = null
        exomeRecord = null
        parameters = null
    }

    @Test
    public void testInitCorrectExome() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = '/tmp/test-init-bedFile.bed'
        fileParameters.refGenMetaInfoFilePath = '/tmp/test-init-getMetaInfo.info'
        File bedFile = new File(fileParameters.bedFilePath)
        File refGenMetaFile = new File(fileParameters.refGenMetaInfoFilePath)
        bedFile << 'chr17\t1600\t2000'
        refGenMetaFile << 'chr17\t3000\t3000'
        samCountingStatisticWorker.targetIntervals = null
        try {
            samCountingStatisticWorker.init()
            assertNotNull samCountingStatisticWorker.targetIntervals
        } finally {
            bedFile.delete()
            refGenMetaFile.delete()
        }
    }

    @Test
    public void testInitCorrectNotExome() {
        fileParameters.inputMode = Mode.WGS
        samCountingStatisticWorker.targetIntervals = null
        samCountingStatisticWorker.init()
        assertNull samCountingStatisticWorker.targetIntervals
    }

    /**
     * Verifies if the worker under teest correctly detects
     * when our dependencies (bedUtils/TargetIntervals) are borked.
     * (so that we quickly know the problem is elsewhere --> failfast!)
     *
     * Note: does not actually depend on the dependency (we inject a borken Mock)
    */
    @Test
    public void testInitBrockenFactory() {
        String refGenomeEntry = 'chr1\t1234'
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = 'bedFilePath'
        fileParameters.refGenMetaInfoFilePath = 'refGenMetaInfoFilePath'
        def fileMock = new StubFor(File)
        fileMock.demand.eachLine { closure -> closure(refGenomeEntry) }
        TargetIntervalsFactory.metaClass.static.create = { bedFilePath, refGenEntryNames -> null }
        samCountingStatisticWorker.targetIntervals = null
        shouldFail { fileMock.use { samCountingStatisticWorker.init() } }
    }

    @Test
    public void testCountBasesMappedNoAlignmentBlocks() {
        samCountingStatisticWorker.metaClass.init = {}
        samCountingStatisticWorker.targetIntervals = {} as TargetIntervals
        exomeRecord.readUnmappedFlag = false
        exomeRecord.readString = "TAGATAATTGAATTG"
        exomeRecord.cigarString = "15D"
        samCountingStatisticWorker.countBasesMapped(chromosome, exomeRecord)
        assertEquals(0, chromosome.allBasesMapped)
        assertEquals(0, chromosome.onTargetMappedBases)
    }

    // case when all the read pass all the filters
    @Test
    public void testCountBasesMappedCorrectExon() {
        samCountingStatisticWorker.metaClass.init = {}
        samCountingStatisticWorker.targetIntervals = { name, start, end ->
            assertEquals('TEST', name)
            assertEquals(1, start)
            assertEquals(11, end)
            return start + end
        } as TargetIntervals
        samCountingStatisticWorker.countBasesMapped(chromosome, exomeRecord)
        assertEquals(10, chromosome.allBasesMapped)
        assertEquals(12, chromosome.onTargetMappedBases)
    }

    // case when all the read pass all the filters
    @Test
    public void testCountBasesMappedCorrectWgs() {
        samCountingStatisticWorker.metaClass.init = {}
        samCountingStatisticWorker.targetIntervals = null
        samCountingStatisticWorker.countBasesMapped(chromosome, exomeRecord)
        assertEquals(10, chromosome.allBasesMapped)
        assertEquals(0, chromosome.onTargetMappedBases)
    }

     // countBasesMapped(...): cases when read does not fit the filter
     // failed to create record with alignmentEnd == 0 in the reasonable time, skip this case;
     // the function calling this methid has been already tested:
     // 1) see tests for SAMCountingStatisticWorker.coverageQc in this class
     // 2) comparison tests agains result of running Roddy: see build.gradle: comparisonTest task

    @Test
    public void testCountBasesMappedReadUnmappedFlagTrue() {
        exomeRecord.readUnmappedFlag = true
        runTestReadFilteredOut()
    }

    @Test
    public void testCountBasesMappedFilterByMappingQuality() {
        exomeRecord.mappingQuality = 1
        runTestReadFilteredOut()
    }

    @Test
    public void testCountBasesMappedReadIsDuplicated() {
        exomeRecord.duplicateReadFlag = true
        runTestReadFilteredOut()
    }

    @Test
    public void testCountBasesMappedFilterByMeanBaseQuality() {
        parameters.minMeanBaseQuality = 100
        runTestReadFilteredOut()
    }

    private void runTestReadFilteredOut() {
        samCountingStatisticWorker.metaClass.init = {}
        samCountingStatisticWorker.targetIntervals = {} as TargetIntervals
        samCountingStatisticWorker.countBasesMapped(chromosome, exomeRecord)
        assertEquals(0, chromosome.allBasesMapped)
        assertEquals(0, chromosome.onTargetMappedBases)
    }

    // other tests for the worker

    @Test
    public void testCountIncorrectProperPairs_ProperPairsFlag_False() {
        record.setReadPairedFlag(true)
        record.setProperPairFlag(false)
        chromosome.properPairStrandConflict = 0
        samCountingStatisticWorker.countIncorrectProperPairs(chromosome, record)
        assertEquals(0, chromosome.properPairStrandConflict)
    }

    @Test
    public void testCountIncorrectProperPairs_NotSameReference() {
        record.setReadPairedFlag(true)
        record.setProperPairFlag(true)
        record.setReferenceName("Chr1")
        record.setMateReferenceName("Chr2")
        chromosome.properPairStrandConflict = 0
        samCountingStatisticWorker.countIncorrectProperPairs(chromosome, record)
        assertEquals(0, chromosome.properPairStrandConflict)
    }

    @Test
    public void testCountIncorrectProperPairs_DifferentNegativeStrangFlag() {
        record.setReadPairedFlag(true)
        record.setProperPairFlag(true)
        record.setReferenceName("Chr1")
        record.setMateReferenceName("Chr1")
        record.setReadNegativeStrandFlag(true)
        record.setMateNegativeStrandFlag(false)
        chromosome.properPairStrandConflict = 0
        samCountingStatisticWorker.countIncorrectProperPairs(chromosome, record)
        assertEquals(0, chromosome.properPairStrandConflict)
    }

    @Test
    public void testCountIncorrectProperPairs_PlusOne() {
        record.setReadPairedFlag(true)
        record.setProperPairFlag(true)
        record.setReferenceName("Chr1")
        record.setMateReferenceName("Chr1")
        record.setReadNegativeStrandFlag(true)
        record.setMateNegativeStrandFlag(true)
        chromosome.properPairStrandConflict = 0
        samCountingStatisticWorker.countIncorrectProperPairs(chromosome, record)
        assertEquals(1, chromosome.properPairStrandConflict)
    }

    @Test
    public void testOrientationCounter_ReadUnmapped_True() {
        record.setReadPairedFlag(true)
        record.setReadUnmappedFlag(true)
        record.setMateUnmappedFlag(false)
        CigarElement cigarElement1 = new CigarElement(15, CigarOperator.MATCH_OR_MISMATCH)
        CigarElement cigarElement2 = new CigarElement(10, CigarOperator.DELETION)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        cigar.add(cigarElement2)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        chromosome.referenceAgreement = 0
        chromosome.referenceAgreementStrandConflict = 0
        samCountingStatisticWorker.orientationCounter(chromosome, record)
        assertEquals(0, chromosome.referenceAgreement)
        assertEquals(0, chromosome.referenceAgreementStrandConflict)
    }

    @Test
    public void testOrientationCounter_ReadMateUnmapped_True() {
        record.setReadPairedFlag(true)
        record.setReadUnmappedFlag(false)
        record.setMateUnmappedFlag(true)
        CigarElement cigarElement1 = new CigarElement(15, CigarOperator.MATCH_OR_MISMATCH)
        CigarElement cigarElement2 = new CigarElement(10, CigarOperator.DELETION)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        cigar.add(cigarElement2)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        chromosome.referenceAgreement = 0
        chromosome.referenceAgreementStrandConflict = 0
        samCountingStatisticWorker.orientationCounter(chromosome, record)
        assertEquals(0, chromosome.referenceAgreement)
        assertEquals(0, chromosome.referenceAgreementStrandConflict)
    }

    @Test
    public void testOrientationCounter_AlignmentEnd_Zero() {
        record.setReadPairedFlag(true)
        record.setReadUnmappedFlag(false)
        record.setMateUnmappedFlag(false)
        //CigarElement cigarElement = new CigarElement()
        Cigar cigar = new Cigar()
        //since alignmentEnd is calculate by AlignmentStart + CigarLength - 1
        record.setAlignmentStart(1)
        chromosome.referenceAgreement = 0
        chromosome.referenceAgreementStrandConflict = 0
        samCountingStatisticWorker.orientationCounter(chromosome, record)
        assertEquals(0, chromosome.referenceAgreement)
        assertEquals(0, chromosome.referenceAgreementStrandConflict)
    }

    @Test
    public void testOrientationCounter_IncreaseReferenceAgreement() {
        record.setReadPairedFlag(true)
        record.setReadUnmappedFlag(false)
        record.setMateUnmappedFlag(false)
        CigarElement cigarElement1 = new CigarElement(15, CigarOperator.MATCH_OR_MISMATCH)
        CigarElement cigarElement2 = new CigarElement(10, CigarOperator.DELETION)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        cigar.add(cigarElement2)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        chromosome.referenceAgreement = 0
        chromosome.referenceAgreementStrandConflict = 0
        record.setReferenceName("Chr1")
        record.setMateReferenceName("Chr1")
        record.setReadNegativeStrandFlag(true)
        record.setMateNegativeStrandFlag(false)
        samCountingStatisticWorker.orientationCounter(chromosome, record)
        assertEquals(1, chromosome.referenceAgreement)
        assertEquals(0, chromosome.referenceAgreementStrandConflict)
    }

    @Test
    public void testOrientationCounter_IncreaseBoth() {
        record.setReadPairedFlag(true)
        record.setReadUnmappedFlag(false)
        record.setMateUnmappedFlag(false)
        CigarElement cigarElement1 = new CigarElement(15, CigarOperator.MATCH_OR_MISMATCH)
        CigarElement cigarElement2 = new CigarElement(10, CigarOperator.DELETION)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        cigar.add(cigarElement2)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        chromosome.referenceAgreement = 0
        chromosome.referenceAgreementStrandConflict = 0
        record.setReferenceName("Chr1")
        record.setMateReferenceName("Chr1")
        record.setReadNegativeStrandFlag(true)
        record.setMateNegativeStrandFlag(true)
        samCountingStatisticWorker.orientationCounter(chromosome, record)
        assertEquals(1, chromosome.referenceAgreement)
        assertEquals(1, chromosome.referenceAgreementStrandConflict)
    }

    @Test
    public void testMeanBaseQualityCheck_True() {
        CigarElement cigarElement1 = new CigarElement(2, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        def qualityMapping = [30, 20, 0, 0, 0] as byte[]
        record.setBaseQualities(qualityMapping)
        assertTrue(samCountingStatisticWorker.meanBaseQualityCheck(record))
    }

    @Test
    public void testMeanBaseQualityCheck_False() {
        CigarElement cigarElement1 = new CigarElement(2, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        byte[] qualityMapping = [4, 2.6, 0, 0, 0]
        record.setBaseQualities(qualityMapping)
        assertFalse(samCountingStatisticWorker.meanBaseQualityCheck(record))
    }

    @Test
    public void testMeanBaseQualPlusGaps() {
        CigarElement cigarElement1 = new CigarElement(1, CigarOperator.M)
        CigarElement cigarElement2 = new CigarElement(6, CigarOperator.DELETION)
        CigarElement cigarElement3 = new CigarElement(1, CigarOperator.INSERTION)
        CigarElement cigarElement4 = new CigarElement(2, CigarOperator.M)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        cigar.add(cigarElement2)
        cigar.add(cigarElement3)
        cigar.add(cigarElement4)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        byte[] qualityMapping = [5, 10, 10, 1, 0, 0, 0]
        record.setBaseQualities(qualityMapping)
        assertEquals(6.5, samCountingStatisticWorker.meanBaseQualPlusGaps(record))
    }

    @Test
    public void testMeanBaseQualPlusGaps_Zero() {
        CigarElement cigarElement1 = new CigarElement(3, CigarOperator.M)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        def qualityMapping = [0, 0, 0, 0, 0, 0, 0] as byte[]
        record.setBaseQualities(qualityMapping)
        assertEquals(0, samCountingStatisticWorker.meanBaseQualPlusGaps(record))
    }

    @Test
    public void testGetLengthOfAligmentsBlocksPlusGaps() {
        CigarElement cigarElement1 = new CigarElement(2, CigarOperator.M)
        CigarElement cigarElement2 = new CigarElement(1, CigarOperator.DELETION)
        CigarElement cigarElement3 = new CigarElement(10, CigarOperator.INSERTION)
        CigarElement cigarElement4 = new CigarElement(1, CigarOperator.M)
        CigarElement cigarElement5 = new CigarElement(1, CigarOperator.D)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        cigar.add(cigarElement2)
        cigar.add(cigarElement3)
        cigar.add(cigarElement4)
        cigar.add(cigarElement5)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        assertEquals(13, samCountingStatisticWorker.getLengthOfAligmentsBlocksPlusGaps(record))
    }

    @Test
    public void testGetLengthOfAligmentsBlocksPlusGaps_NoBlocks() {
        CigarElement cigarElement1 = new CigarElement(0, CigarOperator.M)
        Cigar cigar = new Cigar()
        cigar.add(cigarElement1)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        assertEquals(0, samCountingStatisticWorker.getLengthOfAligmentsBlocksPlusGaps(record))
    }

    /**
     * lengthOfAligmentPlusGaps shorter than parameters.minAlignedRecordLength -> else case
     * getSecondOfPairFlag -> true
     */
    @Test
    void testRecQualityAssessmentAligmentPlusGapsLongerSecondTrue() {
        record.setAlignmentStart(0)
        record.setSecondOfPairFlag(true)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(10, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)
        chromosome.mappedShortR1 = 0
        chromosome.mappedShortR2 = 0
        long mappedShortR1Exp = 0
        long mappedShortR2Exp = 1
        samCountingStatisticWorker.recQualityAssessment(chromosome, record)
        long mappedShortR1Act = chromosome.mappedShortR1
        long mappedShortR2Act = chromosome.mappedShortR2
        assertEquals(mappedShortR1Exp, mappedShortR1Act)
        assertEquals(mappedShortR2Exp, mappedShortR2Act)
    }

    /**
     * lengthOfAligmentPlusGaps shorter than parameters.minAlignedRecordLength -> else case
     * getSecondOfPairFlag -> false
     */
    @Test
    void testRecQualityAssessmentAligmentPlusGapsLongerSecondFalse() {
        record.setAlignmentStart(0)
        record.setSecondOfPairFlag(false)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(10, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)
        chromosome.mappedShortR1 = 0
        chromosome.mappedShortR2 = 0
        long mappedShortR1Exp = 1
        long mappedShortR2Exp = 0

        samCountingStatisticWorker.recQualityAssessment(chromosome, record)

        long mappedShortR1Act = chromosome.mappedShortR1
        long mappedShortR2Act = chromosome.mappedShortR2

        assertEquals(mappedShortR1Exp, mappedShortR1Act)
        assertEquals(mappedShortR2Exp, mappedShortR2Act)
    }

    /**
     * lengthOfAligmentPlusGaps longer than parameters.minAlignedRecordLength -> if case
     * meanBaseQualityCheck -> true
     * getSecondOfPairFlag -> true
     */
    @Test
    void testRecQualityAssessmentMeanBaseQualityCheckTrueSecondOfPairFlagTrue() {
        chromosome.qcBasesMapped = 0
        chromosome.mappedQualityLongR2 = 0
        chromosome.mappedQualityLongR1 = 0

        byte[] qualityMapping = [5, 10, 10, 1, 5, 4, 1, 4, 6, 3, 6, 8, 3, 5, 6, 2, 4, 7, 6, 9]
        record.setBaseQualities(qualityMapping)
        record.setSecondOfPairFlag(true)
        record.setAlignmentStart(0)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(20, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)

        long qcBasesMappedExp = 20
        long mappedQualityLongR2Exp = 1
        long mappedQualityLongR1Exp = 0

        samCountingStatisticWorker.recQualityAssessment(chromosome, record)

        long qcBasesMappedAct = chromosome.qcBasesMapped
        long mappedQualityLongR2Act = chromosome.mappedQualityLongR2
        long mappedQualityLongR1Act = chromosome.mappedQualityLongR1

        assertEquals(qcBasesMappedExp, qcBasesMappedAct)
        assertEquals(mappedQualityLongR2Exp, mappedQualityLongR2Act)
        assertEquals(mappedQualityLongR1Exp, mappedQualityLongR1Act)
    }

    /**
     * lengthOfAligmentPlusGaps longer than parameters.minAlignedRecordLength -> if case
     * meanBaseQualityCheck -> true
     * getSecondOfPairFlag -> false
     */
    @Test
    void testRecQualityAssessmentMeanBaseQualityCheckTrueSecondOfPairFlagFalse() {
        chromosome.qcBasesMapped = 0
        chromosome.mappedQualityLongR1 = 0
        chromosome.mappedQualityLongR2 = 0

        byte[] qualityMapping = [5, 10, 10, 1, 5, 4, 1, 4, 6, 3, 6, 8, 3, 5, 6, 2, 4, 7, 6, 9]
        record.setBaseQualities(qualityMapping)
        record.setSecondOfPairFlag(false)
        record.setAlignmentStart(0)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(20, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)

        long qcBasesMappedExp = 20
        long mappedQualityLongR1Exp = 1
        long mappedQualityLongR2Exp = 0

        samCountingStatisticWorker.recQualityAssessment(chromosome, record)

        long qcBasesMappedAct = chromosome.qcBasesMapped
        long mappedQualityLongR1Act = chromosome.mappedQualityLongR1
        long mappedQualityLongR2Act = chromosome.mappedQualityLongR2

        assertEquals(qcBasesMappedExp, qcBasesMappedAct)
        assertEquals(mappedQualityLongR1Exp, mappedQualityLongR1Act)
        assertEquals(mappedQualityLongR2Exp, mappedQualityLongR2Act)
    }

    /**
     * lengthOfAligmentPlusGaps longer than parameters.minAlignedRecordLength -> if case
     * meanBaseQualityCheck -> false
     * getSecondOfPairFlag -> true
     */
    @Test
    void testRecQualityAssessmentMeanBaseQualityCheckFalseSecondOfPairFlagTrue() {
        chromosome.mappedLowQualityR2 = 0
        chromosome.mappedLowQualityR1 = 0
        chromosome.qcBasesMapped = 0

        byte[] qualityMapping = [5, 1, 1, 1, 5, 4, 1, 4, 3, 3, 1, 3, 3, 1, 2, 2, 4, 7, 6, 1]
        record.setBaseQualities(qualityMapping)
        record.setSecondOfPairFlag(true)
        record.setAlignmentStart(0)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(20, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)

        samCountingStatisticWorker.recQualityAssessment(chromosome, record)

        long mappedLowQualityR2Exp = 1
        long mappedLowQualityR1Exp = 0
        long qcBasesMappedExp = 0
        long mappedLowQualityR2Act = chromosome.mappedLowQualityR2
        long mappedLowQualityR1Act = chromosome.mappedLowQualityR1
        long qcBasesMappedAct = chromosome.qcBasesMapped

        assertEquals(qcBasesMappedExp, qcBasesMappedAct)
        assertEquals(mappedLowQualityR1Exp, mappedLowQualityR1Act)
        assertEquals(mappedLowQualityR2Exp, mappedLowQualityR2Act)
    }

    /**
     * lengthOfAligmentPlusGaps longer than parameters.minAlignedRecordLength -> if case
     * meanBaseQualityCheck -> false
     * getSecondOfPairFlag -> false
     */
    @Test
    void testRecQualityAssessmentMeanBaseQualityCheckFalseSecondOfPairFlagFalse() {
        chromosome.mappedLowQualityR2 = 0
        chromosome.mappedLowQualityR1 = 0
        chromosome.qcBasesMapped = 0

        byte[] qualityMapping = [5, 1, 1, 1, 5, 4, 1, 4, 3, 3, 1, 3, 3, 1, 2, 2, 4, 7, 6, 1]
        record.setBaseQualities(qualityMapping)
        record.setSecondOfPairFlag(false)
        record.setAlignmentStart(0)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(20, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)

        long mappedLowQualityR2Exp = 0
        long mappedLowQualityR1Exp = 1
        long qcBasesMappedExp = 0

        samCountingStatisticWorker.recQualityAssessment(chromosome, record)

        long mappedLowQualityR2Act = chromosome.mappedLowQualityR2
        long mappedLowQualityR1Act = chromosome.mappedLowQualityR1
        long qcBasesMappedAct = chromosome.qcBasesMapped

        assertEquals(qcBasesMappedExp, qcBasesMappedAct)
        assertEquals(mappedLowQualityR1Exp, mappedLowQualityR1Act)
        assertEquals(mappedLowQualityR2Exp, mappedLowQualityR2Act)
    }

    /**
     * getReadUnmappedFlag is true
     * record.getAlignmentEnd() != 0
     * record.getMappingQuality() > parameters.mappingQuality
     */
    @Test
    public void testRecordIsQualityMappedReadUnmappedFlagTrue() {
        record.setReadUnmappedFlag(true)
        assertFalse(samCountingStatisticWorker.recordIsQualityMapped(record))
    }

    /**
     * getReadUnmappedFlag is false
     * record.getAlignmentEnd() == 0
     * record.getMappingQuality() > parameters.mappingQuality
     */
    @Test
    public void testRecordIsQualityMappedAlignmentEndEqualZero() {
        record.setReadUnmappedFlag(false)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(1, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)
        assertFalse(samCountingStatisticWorker.recordIsQualityMapped(record))
    }

    /**
     * getReadUnmappedFlag is false
     * record.getAlignmentEnd() != 0
     * record.getMappingQuality() <= parameters.mappingQuality
     */
    @Test
    public void testRecordIsQualityMappedMappingQualityLower() {
        record.setReadUnmappedFlag(false)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(10, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)
        record.setMappingQuality(0)
        assertFalse(samCountingStatisticWorker.recordIsQualityMapped(record))
    }

    /**
     * getReadUnmappedFlag is false
     * record.getAlignmentEnd() != 0
     * record.getMappingQuality() > parameters.mappingQuality
     */
    @Test
    public void testRecordIsQualityMappedTrue() {
        record.setReadUnmappedFlag(false)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(10, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)
        record.setMappingQuality(20)
        assertTrue(samCountingStatisticWorker.recordIsQualityMapped(record))
    }

    /**
     * duplicateCount is true -> setDuplicateReadFlag(true), setFirstOfPairFlag(true/false)
     */
    @Test
    void testCoverageQcDublicateCount() {
        chromosome.notMappedR2 = 0
        chromosome.notMappedR1 = 0
        record.setDuplicateReadFlag(true)
        record.setFirstOfPairFlag(true)
        samCountingStatisticWorker.coverageQc(chromosome, record)
        long notMappedR2Exp = 0
        long notMappedR1Exp = 0
        long notMappedR2Act = chromosome.notMappedR2
        long notMappedR1Act = chromosome.notMappedR1
        assertEquals(notMappedR2Exp, notMappedR2Act)
        assertEquals(notMappedR1Exp, notMappedR1Act)
    }

    /**
     * duplicateCount is false -> setDuplicateReadFlag(false)
     recordIsQualityMapped is true -> setReadUnmappedFlag(false), getAlignmentEnd() != 0, getMappingQuality() > parameters.mappingQuality
     */
    @Test
    void testCoverageQcRecordIsQualityMappedTrue() {
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(false)
        //alignment end is derived from alignment start and CIGAR
        CigarElement cigarElementOne = new CigarElement(15, CigarOperator.MATCH_OR_MISMATCH)
        Cigar cigar = new Cigar()
        cigar.add(cigarElementOne)
        record.setCigar(cigar)
        record.setAlignmentStart(0)
        record.setReadUnmappedFlag(false)
        record.setMappingQuality(10)
        record.setSecondOfPairFlag(true)
        chromosome.mappedShortR2 = 0
        samCountingStatisticWorker.coverageQc(chromosome, record)
        long mappedShortR2Exp = 1
        long mappedShortR2Act = chromosome.mappedShortR2
        assertEquals(mappedShortR2Exp, mappedShortR2Act)
    }

    /**
     * duplicateCount is false -> setDuplicateReadFlag(false)
     recordIsQualityMapped is false -> setReadUnmappedFlag(true)
     getSecondOfPairFlag is true -> setSecondOfPairFlag(true)
     */
    @Test
    void testCoverageQcSecondOfPairFlagTrue() {
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setSecondOfPairFlag(true)
        chromosome.mappedShortR2 = 0
        samCountingStatisticWorker.coverageQc(chromosome, record)
        long chromosomeNotMappedR2Exp = 1
        long chromosomeNotMappedR2Act = chromosome.notMappedR2
        assertEquals(chromosomeNotMappedR2Exp, chromosomeNotMappedR2Act)
    }

    /**
     * duplicateCount is false -> setDuplicateReadFlag(false)
     recordIsQualityMapped is false -> setReadUnmappedFlag(true)
     getSecondOfPairFlag is false -> setSecondOfPairFlag(false)
     */
    @Test
    void testCoverageQcSecondOfPairFlagFalse() {
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setMappingQuality(0)
        record.setSecondOfPairFlag(false)
        chromosome.mappedShortR1 = 0
        samCountingStatisticWorker.coverageQc(chromosome, record)
        long chromosomeNotMappedR1Exp = 1
        long chromosomeNotMappedR1Act = chromosome.notMappedR1
        assertEquals(chromosomeNotMappedR1Exp, chromosomeNotMappedR1Act)
    }

    /**
     * getProperPairFlag is true
     */
    @Test
    void testPairEndReadAberrationProperPairFlagTrue() {
        chromosome.endReadAberration = 0
        record.setProperPairFlag(true)
        long endReadAberrationExp = 0
        samCountingStatisticWorker.pairEndReadAberration(chromosome, record)
        long endReadAberrationAct = chromosome.endReadAberration
        assertEquals(endReadAberrationExp, endReadAberrationAct)
    }

    /**
     * getReadUnmappedFlag is true
     */
    @Test
    void testPairEndReadAberrationReadUnmappedFlagTrue() {
        chromosome.endReadAberration = 0
        record.setReadUnmappedFlag(true)
        long endReadAberrationExp = 0
        samCountingStatisticWorker.pairEndReadAberration(chromosome, record)
        long endReadAberrationAct = chromosome.endReadAberration
        assertEquals(endReadAberrationExp, endReadAberrationAct)
    }

    /**
     * getMateUnmappedFlag is true
     */
    @Test
    void testPairEndReadAberrationMateUnmappedFlagTrue() {
        chromosome.endReadAberration = 0
        record.setMateUnmappedFlag(true)
        long endReadAberrationExp = 0
        samCountingStatisticWorker.pairEndReadAberration(chromosome, record)
        long endReadAberrationAct = chromosome.endReadAberration
        assertEquals(endReadAberrationExp, endReadAberrationAct)
    }

    /**
     * all flags are false and the reference name is correct
     */
    @Test
    void testPairEndReadAberrationRightName() {
        chromosome.endReadAberration = 0
        record.setMateUnmappedFlag(false)
        record.setReadUnmappedFlag(false)
        record.setProperPairFlag(false)
        record.setReferenceName("test")
        record.setMateReferenceName("test")
        long endReadAberrationExp = 0
        samCountingStatisticWorker.pairEndReadAberration(chromosome, record)
        long endReadAberrationAct = chromosome.endReadAberration
        assertEquals(endReadAberrationExp, endReadAberrationAct)
    }

    /**
     * all flags and the reference name are false
     */
    @Test
    void testPairEndReadAberrationWrongName() {
        chromosome.endReadAberration = 0
        record.setMateUnmappedFlag(false)
        record.setReadUnmappedFlag(false)
        record.setProperPairFlag(false)
        record.setReferenceName("test")
        record.setMateReferenceName("andererTest")
        long endReadAberrationExp = 1
        samCountingStatisticWorker.pairEndReadAberration(chromosome, record)
        long endReadAberrationAct = chromosome.endReadAberration
        assertEquals(endReadAberrationExp, endReadAberrationAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is true
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is true
     * getReadPairedFlag is false
     */
    @Test
    void testFlagStatReadFailsVendorQualityCheckFlagTrue() {
        record.setReadFailsVendorQualityCheckFlag(true)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setReadPairedFlag(false)
        chromosome.qcFailedReads = 0
        long qcFailedReadsExp = 1
        samCountingStatisticWorker.flagStat(chromosome, record)
        long qcFailedReadsAct = chromosome.qcFailedReads
        assertEquals(qcFailedReadsExp, qcFailedReadsAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is true
     * getReadUnmappedFlag is true
     * getReadPairedFlag is false
     */
    @Test
    void testFlagStatDuplicateReadFlagTrue() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(true)
        record.setReadUnmappedFlag(true)
        record.setReadPairedFlag(false)
        chromosome.duplicates = 0
        long duplicatesExp = 1
        samCountingStatisticWorker.flagStat(chromosome, record)
        long duplicatesAct = chromosome.duplicates
        assertEquals(duplicatesExp, duplicatesAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is false
     * getReadPairedFlag is false
     */
    @Test
    void testFlagStatReadUnmappedFlagFalse() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(false)
        record.setReadPairedFlag(false)
        chromosome.totalMappedReadCounter = 0
        long totalMappedReadCounterExp = 1
        samCountingStatisticWorker.flagStat(chromosome, record)
        long totalMappedReadCounterAct = chromosome.totalMappedReadCounter
        assertEquals(totalMappedReadCounterExp, totalMappedReadCounterAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is true
     * getReadPairedFlag is false
     */
    @Test
    void testFlagStatReadPairedFlagFalse() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setReadPairedFlag(false)
        chromosome.pairedInSequencing = 0
        long pairedInSequencingExp = 0
        samCountingStatisticWorker.flagStat(chromosome, record)
        long pairedInSequencingAct = chromosome.pairedInSequencing
        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is true
     * getReadPairedFlag is true
     * getSecondOfPairFlag is true
     * getFirstOfPairFlag is false
     * getProperPairFlag is false
     * getMateUnmappedFlag is true
     */
    @Test
    void testFlagStatSecondOfPairFlagTrue() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(true)
        record.setFirstOfPairFlag(false)
        record.setProperPairFlag(false)
        record.setMateUnmappedFlag(true)
        chromosome.pairedInSequencing = 0
        chromosome.pairedRead2 = 0
        long pairedInSequencingExp = 1
        long pairedRead2Exp = 1

        samCountingStatisticWorker.flagStat(chromosome, record)

        long pairedInSequencingAct = chromosome.pairedInSequencing
        long pairedRead2Act = chromosome.pairedRead2

        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(pairedRead2Exp, pairedRead2Act)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is true
     * getReadPairedFlag is true
     * getSecondOfPairFlag is false
     * getFirstOfPairFlag is true
     * getProperPairFlag is false
     * getMateUnmappedFlag is true
     */
    @Test
    void testFlagStatFirstOfPairFlagTrue() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(false)
        record.setFirstOfPairFlag(true)
        record.setProperPairFlag(false)
        record.setMateUnmappedFlag(true)
        chromosome.pairedInSequencing = 0
        chromosome.pairedRead1 = 0
        long pairedInSequencingExp = 1
        long pairedRead1Exp = 1

        samCountingStatisticWorker.flagStat(chromosome, record)

        long pairedInSequencingAct = chromosome.pairedInSequencing
        long pairedRead1Act = chromosome.pairedRead1

        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(pairedRead1Exp, pairedRead1Act)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is true
     * getReadPairedFlag is true
     * getSecondOfPairFlag is false
     * getFirstOfPairFlag is false
     * getProperPairFlag is true
     * getMateUnmappedFlag is true
     */
    @Test
    void testFlagStatProperPairFlagTrue() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(false)
        record.setFirstOfPairFlag(false)
        record.setProperPairFlag(true)
        record.setMateUnmappedFlag(true)
        chromosome.pairedInSequencing = 0
        chromosome.properlyPaired = 0
        long pairedInSequencingExp = 1
        long properlyPairedExp = 1

        samCountingStatisticWorker.flagStat(chromosome, record)

        long pairedInSequencingAct = chromosome.pairedInSequencing
        long properlyPairedAct = chromosome.properlyPaired

        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(properlyPairedExp, properlyPairedAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is true
     * getReadPairedFlag is true
     * getSecondOfPairFlag is false
     * getFirstOfPairFlag is false
     * getProperPairFlag is false
     * getMateUnmappedFlag is true
     */
    @Test
    void testFlagStatReadUnmappedFlagFalseMateUnmappedFlagTrue() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(false)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(false)
        record.setFirstOfPairFlag(false)
        record.setProperPairFlag(false)
        record.setMateUnmappedFlag(true)
        chromosome.pairedInSequencing = 0
        chromosome.totalMappedReadCounter = 0
        chromosome.withItselfAndMateMapped = 0
        chromosome.singletons = 0
        long pairedInSequencingExp = 1
        long totalMappedReadCounterExp = 1
        long withItselfAndMateMappedExp = 0
        long singletonsExp = 1

        samCountingStatisticWorker.flagStat(chromosome, record)

        long pairedInSequencingAct = chromosome.pairedInSequencing
        long totalMappedReadCounterAct = chromosome.totalMappedReadCounter
        long withItselfAndMateMappedAct = chromosome.withItselfAndMateMapped
        long singletonsAct = chromosome.singletons

        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(totalMappedReadCounterExp, totalMappedReadCounterAct)
        assertEquals(withItselfAndMateMappedExp, withItselfAndMateMappedAct)
        assertEquals(singletonsExp, singletonsAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is true
     * getReadPairedFlag is true
     * getSecondOfPairFlag is false
     * getFirstOfPairFlag is false
     * getProperPairFlag is false
     * getMateUnmappedFlag is false
     */
    @Test
    void testFlagStatReadUnmappedFlagTrueMateUnmappedFlagFalse() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(false)
        record.setFirstOfPairFlag(false)
        record.setProperPairFlag(false)
        record.setMateUnmappedFlag(false)
        chromosome.pairedInSequencing = 0
        chromosome.totalMappedReadCounter = 0
        chromosome.withItselfAndMateMapped = 0
        chromosome.singletons = 0
        long pairedInSequencingExp = 1
        long totalMappedReadCounterExp = 0
        long withItselfAndMateMappedExp = 0
        long singletonsExp = 0

        samCountingStatisticWorker.flagStat(chromosome, record)

        long pairedInSequencingAct = chromosome.pairedInSequencing
        long totalMappedReadCounterAct = chromosome.totalMappedReadCounter
        long withItselfAndMateMappedAct = chromosome.withItselfAndMateMapped
        long singletonsAct = chromosome.singletons

        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(totalMappedReadCounterExp, totalMappedReadCounterAct)
        assertEquals(withItselfAndMateMappedExp, withItselfAndMateMappedAct)
        assertEquals(singletonsExp, singletonsAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is false
     * getReadPairedFlag is true
     * getSecondOfPairFlag is false
     * getFirstOfPairFlag is false
     * getProperPairFlag is false
     * getMateUnmappedFlag is false
     * record.getReferenceIndex() == record.getMateReferenceIndex()
     */
    @Test
    void testFlagStatReferenceIndexEqualsMateReferenceIndex() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(false)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(false)
        record.setFirstOfPairFlag(false)
        record.setProperPairFlag(false)
        record.setMateUnmappedFlag(false)
        record.setReferenceIndex(-1)
        record.setMateReferenceIndex(-1)

        chromosome.pairedInSequencing = 0
        chromosome.totalMappedReadCounter = 0
        chromosome.withItselfAndMateMapped = 0
        chromosome.singletons = 0
        chromosome.withMateMappedToDifferentChr = 0

        long pairedInSequencingExp = 1
        long totalMappedReadCounterExp = 1
        long withItselfAndMateMappedExp = 1
        long singletonsExp = 0
        long withMateMappedToDifferentChrExp = 0

        samCountingStatisticWorker.flagStat(chromosome, record)

        long pairedInSequencingAct = chromosome.pairedInSequencing
        long totalMappedReadCounterAct = chromosome.totalMappedReadCounter
        long withItselfAndMateMappedAct = chromosome.withItselfAndMateMapped
        long singletonsAct = chromosome.singletons
        long withMateMappedToDifferentChrAct = chromosome.withMateMappedToDifferentChr

        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(totalMappedReadCounterExp, totalMappedReadCounterAct)
        assertEquals(withItselfAndMateMappedExp, withItselfAndMateMappedAct)
        assertEquals(singletonsExp, singletonsAct)
        assertEquals(withMateMappedToDifferentChrExp, withMateMappedToDifferentChrAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is false
     * getReadPairedFlag is true
     * getSecondOfPairFlag is false
     * getFirstOfPairFlag is false
     * getProperPairFlag is false
     * getMateUnmappedFlag is false
     * record.getReferenceIndex() != record.getMateReferenceIndex()
     * record.getMappingQuality() < 5
     */
    @Test
    void testFlagStatReferenceIndexNotEqualsMateReferenceIndex() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(false)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(false)
        record.setFirstOfPairFlag(false)
        record.setProperPairFlag(false)
        record.setMateUnmappedFlag(false)
        SAMSequenceRecord sAMSequenceRecordOne = new SAMSequenceRecord("test1", 3)
        SAMSequenceRecord sAMSequenceRecordTwo = new SAMSequenceRecord("test2", 2)
        SAMSequenceDictionary sequenceDictionary = new SAMSequenceDictionary()
        sequenceDictionary.addSequence(sAMSequenceRecordOne)
        sequenceDictionary.addSequence(sAMSequenceRecordTwo)
        SAMFileHeader sAMFileHeader = new SAMFileHeader()
        sAMFileHeader.setSequenceDictionary(sequenceDictionary)
        record.setHeader(sAMFileHeader)
        record.setReferenceIndex(1)
        record.setMateReferenceIndex(-1)
        record.setMappingQuality(1)

        chromosome.pairedInSequencing = 0
        chromosome.totalMappedReadCounter = 0
        chromosome.withItselfAndMateMapped = 0
        chromosome.singletons = 0
        chromosome.withMateMappedToDifferentChr = 0
        chromosome.withMateMappedToDifferentChrMaq = 0

        long pairedInSequencingExp = 1
        long totalMappedReadCounterExp = 1
        long withItselfAndMateMappedExp = 1
        long singletonsExp = 0
        long withMateMappedToDifferentChrExp = 1
        long withMateMappedToDifferentChrMaqExp = 0

        samCountingStatisticWorker.flagStat(chromosome, record)

        long pairedInSequencingAct = chromosome.pairedInSequencing
        long totalMappedReadCounterAct = chromosome.totalMappedReadCounter
        long withItselfAndMateMappedAct = chromosome.withItselfAndMateMapped
        long singletonsAct = chromosome.singletons
        long withMateMappedToDifferentChrAct = chromosome.withMateMappedToDifferentChr
        long withMateMappedToDifferentChrMaqAct = chromosome.withMateMappedToDifferentChrMaq

        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(totalMappedReadCounterExp, totalMappedReadCounterAct)
        assertEquals(withItselfAndMateMappedExp, withItselfAndMateMappedAct)
        assertEquals(singletonsExp, singletonsAct)
        assertEquals(withMateMappedToDifferentChrExp, withMateMappedToDifferentChrAct)
        assertEquals(withMateMappedToDifferentChrMaqExp, withMateMappedToDifferentChrMaqAct)
    }

    /**
     * getReadFailsVendorQualityCheckFlag is false
     * getDuplicateReadFlag is false
     * getReadUnmappedFlag is false
     * getReadPairedFlag is true
     * getSecondOfPairFlag is false
     * getFirstOfPairFlag is false
     * getProperPairFlag is false
     * getMateUnmappedFlag is false
     * record.getReferenceIndex() != record.getMateReferenceIndex()
     * record.getMappingQuality() >= 5
     */
    @Test
    void testFlagStatMappingQualityGreater5() {
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(false)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(false)
        record.setFirstOfPairFlag(false)
        record.setProperPairFlag(false)
        record.setMateUnmappedFlag(false)
        SAMSequenceRecord sAMSequenceRecordOne = new SAMSequenceRecord("test1", 3)
        SAMSequenceRecord sAMSequenceRecordTwo = new SAMSequenceRecord("test2", 2)
        SAMSequenceDictionary sequenceDictionary = new SAMSequenceDictionary()
        sequenceDictionary.addSequence(sAMSequenceRecordOne)
        sequenceDictionary.addSequence(sAMSequenceRecordTwo)
        SAMFileHeader sAMFileHeader = new SAMFileHeader()
        sAMFileHeader.setSequenceDictionary(sequenceDictionary)
        record.setHeader(sAMFileHeader)
        record.setReferenceIndex(1)
        record.setMateReferenceIndex(-1)
        record.setMappingQuality(8)

        chromosome.pairedInSequencing = 0
        chromosome.totalMappedReadCounter = 0
        chromosome.withItselfAndMateMapped = 0
        chromosome.singletons = 0
        chromosome.withMateMappedToDifferentChr = 0
        chromosome.withMateMappedToDifferentChrMaq = 0

        long pairedInSequencingExp = 1
        long totalMappedReadCounterExp = 1
        long withItselfAndMateMappedExp = 1
        long singletonsExp = 0
        long withMateMappedToDifferentChrExp = 1
        long withMateMappedToDifferentChrMaqExp = 1

        samCountingStatisticWorker.flagStat(chromosome, record)

        long pairedInSequencingAct = chromosome.pairedInSequencing
        long totalMappedReadCounterAct = chromosome.totalMappedReadCounter
        long withItselfAndMateMappedAct = chromosome.withItselfAndMateMapped
        long singletonsAct = chromosome.singletons
        long withMateMappedToDifferentChrAct = chromosome.withMateMappedToDifferentChr
        long withMateMappedToDifferentChrMaqAct = chromosome.withMateMappedToDifferentChrMaq

        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(totalMappedReadCounterExp, totalMappedReadCounterAct)
        assertEquals(withItselfAndMateMappedExp, withItselfAndMateMappedAct)
        assertEquals(singletonsExp, singletonsAct)
        assertEquals(withMateMappedToDifferentChrExp, withMateMappedToDifferentChrAct)
        assertEquals(withMateMappedToDifferentChrMaqExp, withMateMappedToDifferentChrMaqAct)
    }

    @Test
    void testDoCounting() {
        chromosome.mappedShortR1 = 0
        chromosome.endReadAberration = 0
        chromosome.pairedInSequencing = 0
        chromosome.totalMappedReadCounter = 0
        chromosome.withItselfAndMateMapped = 0
        chromosome.singletons = 0

        record.setDuplicateReadFlag(false)
        record.setReadUnmappedFlag(true)
        record.setMappingQuality(0)
        record.setSecondOfPairFlag(false)
        record.setReadFailsVendorQualityCheckFlag(false)
        record.setReadPairedFlag(true)
        record.setSecondOfPairFlag(false)
        record.setFirstOfPairFlag(false)
        record.setProperPairFlag(false)
        record.setMateUnmappedFlag(false)

        long chromosomeNotMappedR1Exp = 1
        long endReadAberrationExp = 0
        long pairedInSequencingExp = 1
        long totalMappedReadCounterExp = 0
        long withItselfAndMateMappedExp = 0
        long singletonsExp = 0

        samCountingStatisticWorker.doCounting(chromosome, record)

        long chromosomeNotMappedR1Act = chromosome.notMappedR1
        long endReadAberrationAct = chromosome.endReadAberration
        long pairedInSequencingAct = chromosome.pairedInSequencing
        long totalMappedReadCounterAct = chromosome.totalMappedReadCounter
        long withItselfAndMateMappedAct = chromosome.withItselfAndMateMapped
        long singletonsAct = chromosome.singletons

        assertEquals(chromosomeNotMappedR1Exp, chromosomeNotMappedR1Act)
        assertEquals(endReadAberrationExp, endReadAberrationAct)
        assertEquals(pairedInSequencingExp, pairedInSequencingAct)
        assertEquals(totalMappedReadCounterExp, totalMappedReadCounterAct)
        assertEquals(withItselfAndMateMappedExp, withItselfAndMateMappedAct)
        assertEquals(singletonsExp, singletonsAct)
    }

    @Test
    void testProcessChromosomeAll() {
        ChromosomeStatisticWrapper chromosomeStatisticWrapper = new ChromosomeStatisticWrapper("chr1", 1000)
        chromosomeStatisticWrapper.chromosome.referenceLength = 1
        chromosomeStatisticWrapper.chromosome.duplicateR1 = 1
        chromosomeStatisticWrapper.chromosome.duplicateR2 = 1
        chromosomeStatisticWrapper.chromosome.properPairStrandConflict = 1
        chromosomeStatisticWrapper.chromosome.referenceAgreement = 1
        chromosomeStatisticWrapper.chromosome.referenceAgreementStrandConflict = 1
        chromosomeStatisticWrapper.chromosome.mappedQualityLongR1 = 1
        chromosomeStatisticWrapper.chromosome.mappedQualityLongR2 = 1
        chromosomeStatisticWrapper.chromosome.qcBasesMapped = 1
        chromosomeStatisticWrapper.chromosome.mappedLowQualityR1 = 1
        chromosomeStatisticWrapper.chromosome.mappedLowQualityR2 = 1
        chromosomeStatisticWrapper.chromosome.mappedShortR1 = 1
        chromosomeStatisticWrapper.chromosome.mappedShortR2 = 1
        chromosomeStatisticWrapper.chromosome.notMappedR1 = 1
        chromosomeStatisticWrapper.chromosome.notMappedR2 = 1
        chromosomeStatisticWrapper.chromosome.endReadAberration = 1
        chromosomeStatisticWrapper.chromosome.totalReadCounter = 1
        chromosomeStatisticWrapper.chromosome.qcFailedReads = 1
        chromosomeStatisticWrapper.chromosome.duplicates = 1
        chromosomeStatisticWrapper.chromosome.totalMappedReadCounter = 1
        chromosomeStatisticWrapper.chromosome.pairedInSequencing = 1
        chromosomeStatisticWrapper.chromosome.pairedRead2 = 1
        chromosomeStatisticWrapper.chromosome.pairedRead1 = 1
        chromosomeStatisticWrapper.chromosome.properlyPaired = 1
        chromosomeStatisticWrapper.chromosome.withItselfAndMateMapped = 1
        chromosomeStatisticWrapper.chromosome.withMateMappedToDifferentChr = 1
        chromosomeStatisticWrapper.chromosome.withMateMappedToDifferentChrMaq = 1
        chromosomeStatisticWrapper.chromosome.singletons = 1

        ChromosomeStatisticWrapper chromosomeStatisticWrapperAll = new ChromosomeStatisticWrapper("chr1", 1000)
        chromosomeStatisticWrapperAll.chromosome.referenceLength = 0
        chromosomeStatisticWrapperAll.chromosome.duplicateR1 = 0
        chromosomeStatisticWrapperAll.chromosome.duplicateR2 = 0
        chromosomeStatisticWrapperAll.chromosome.properPairStrandConflict = 0
        chromosomeStatisticWrapperAll.chromosome.referenceAgreement = 0
        chromosomeStatisticWrapperAll.chromosome.referenceAgreementStrandConflict = 0
        chromosomeStatisticWrapperAll.chromosome.mappedQualityLongR1 = 0
        chromosomeStatisticWrapperAll.chromosome.mappedQualityLongR2 = 0
        chromosomeStatisticWrapperAll.chromosome.qcBasesMapped = 0
        chromosomeStatisticWrapperAll.chromosome.mappedLowQualityR1 = 0
        chromosomeStatisticWrapperAll.chromosome.mappedLowQualityR2 = 0
        chromosomeStatisticWrapperAll.chromosome.mappedShortR1 = 0
        chromosomeStatisticWrapperAll.chromosome.mappedShortR2 = 0
        chromosomeStatisticWrapperAll.chromosome.notMappedR1 = 0
        chromosomeStatisticWrapperAll.chromosome.notMappedR2 = 0
        chromosomeStatisticWrapperAll.chromosome.endReadAberration = 0
        chromosomeStatisticWrapperAll.chromosome.totalReadCounter = 0
        chromosomeStatisticWrapperAll.chromosome.qcFailedReads = 0
        chromosomeStatisticWrapperAll.chromosome.duplicates = 0
        chromosomeStatisticWrapperAll.chromosome.totalMappedReadCounter = 0
        chromosomeStatisticWrapperAll.chromosome.pairedInSequencing = 0
        chromosomeStatisticWrapperAll.chromosome.pairedRead2 = 0
        chromosomeStatisticWrapperAll.chromosome.pairedRead1 = 0
        chromosomeStatisticWrapperAll.chromosome.properlyPaired = 0
        chromosomeStatisticWrapperAll.chromosome.withItselfAndMateMapped = 0
        chromosomeStatisticWrapperAll.chromosome.withMateMappedToDifferentChr = 0
        chromosomeStatisticWrapperAll.chromosome.withMateMappedToDifferentChrMaq = 0
        chromosomeStatisticWrapperAll.chromosome.singletons = 0

        Collection<ChromosomeStatisticWrapper> chromosomeWrappers = []
        chromosomeWrappers.add(chromosomeStatisticWrapper)

        samCountingStatisticWorker.processChromosomeAll(chromosomeWrappers, chromosomeStatisticWrapperAll)

        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.referenceLength)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.duplicateR1)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.duplicateR2)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.properPairStrandConflict)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.referenceAgreement)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.referenceAgreementStrandConflict)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.mappedQualityLongR1)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.mappedQualityLongR2)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.qcBasesMapped)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.mappedLowQualityR1)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.mappedLowQualityR2)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.mappedShortR1)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.mappedShortR2)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.notMappedR1)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.notMappedR2)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.endReadAberration)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.totalReadCounter)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.qcFailedReads)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.duplicates)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.totalMappedReadCounter)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.pairedInSequencing)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.pairedRead2)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.pairedRead1)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.properlyPaired)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.withItselfAndMateMapped)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.withMateMappedToDifferentChr)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.withMateMappedToDifferentChrMaq)
        assertEquals(1, chromosomeStatisticWrapperAll.chromosome.singletons)
    }

    @Test
    public void testDuplicateCount_DuplicateReadFlag_False() {
        record.setDuplicateReadFlag(false)
        record.setFirstOfPairFlag(true)
        assertFalse(samCountingStatisticWorker.duplicateCount(chromosome, record))
        assertEquals(0, chromosome.duplicateR1)
        assertEquals(0, chromosome.duplicateR2)
    }

    @Test
    public void testDuplicateCount_DuplicateReadFlag_True_First() {
        record.setDuplicateReadFlag(true)
        record.setFirstOfPairFlag(true)
        assertTrue(samCountingStatisticWorker.duplicateCount(chromosome, record))
        assertEquals(1, chromosome.duplicateR1)
        assertEquals(0, chromosome.duplicateR2)
    }

    @Test
    public void testDuplicateCount_DuplicateReadFlag_True_Second() {
        record.setDuplicateReadFlag(true)
        record.setFirstOfPairFlag(false)
        assertTrue(samCountingStatisticWorker.duplicateCount(chromosome, record))
        assertEquals(0, chromosome.duplicateR1)
        assertEquals(1, chromosome.duplicateR2)
    }
}
