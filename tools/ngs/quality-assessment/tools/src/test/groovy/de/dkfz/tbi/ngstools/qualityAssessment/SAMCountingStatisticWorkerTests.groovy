package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.Cigar
import net.sf.samtools.CigarElement
import net.sf.samtools.CigarOperator
import net.sf.samtools.SAMRecord
import net.sf.samtools.AlignmentBlock
import org.junit.*

class CountingStatisticWorkerTests extends GroovyTestCase {

    private ChromosomeStatistic chromosome

    private SAMCountingStatisticWorker samCountingStatisticWorker

    private SAMRecord record

    private Parameters parameters

    @Before
    public void setUp() throws Exception {
        chromosome = new ChromosomeStatistic("TEST", 0)
        samCountingStatisticWorker = new SAMCountingStatisticWorker()
        parameters = new Parameters(winSize: 10, mappingQuality: 5, coverageMappingQualityThreshold: 5, minMeanBaseQuality: 25)
        samCountingStatisticWorker.setParameters(parameters)
        record = new SAMRecord()
    }

    @After
    public void tearDown() throws Exception {
        chromosome = null
        samCountingStatisticWorker = null
        record = null
        parameters = null
    }

    @Test
    public void testDuplicateCount_DuplicateReadFlag_False() {
        record.setReadPairedFlag(true)
        record.setDuplicateReadFlag(false)
        record.setFirstOfPairFlag(true)
        assertFalse(samCountingStatisticWorker.duplicateCount(chromosome, record))
        assertEquals(0, chromosome.duplicateR1)
        assertEquals(0, chromosome.duplicateR2)
    }

    @Test
    public void testDuplicateCount_DuplicateReadFlag_True_First() {
        record.setReadPairedFlag(true) //need to be true for read firstOfPairFlag
        record.setDuplicateReadFlag(true)
        record.setFirstOfPairFlag(true)
        assertTrue(samCountingStatisticWorker.duplicateCount(chromosome, record))
        assertEquals(1, chromosome.duplicateR1)
        assertEquals(0, chromosome.duplicateR2)
    }

    @Test
    public void testDuplicateCount_DuplicateReadFlag_True_Second() {
        record.setReadPairedFlag(true) //need to be true for read firstOfPairFlag
        record.setDuplicateReadFlag(true)
        record.setFirstOfPairFlag(false)
        assertTrue(samCountingStatisticWorker.duplicateCount(chromosome, record))
        assertEquals(0, chromosome.duplicateR1)
        assertEquals(1, chromosome.duplicateR2)
    }

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
        byte[] qualityMapping = [30, 18.8, 0, 0, 0]
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
}
