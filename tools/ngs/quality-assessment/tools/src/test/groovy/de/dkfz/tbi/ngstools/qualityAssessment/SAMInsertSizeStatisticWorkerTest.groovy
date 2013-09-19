package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.SAMRecord
import org.junit.After
import org.junit.Before
import org.junit.Ignore


class OnLineStatisticsTests extends GroovyTestCase {

    private SAMRecord record
    private ChromosomeStatisticWrapper chromosomeStatisticWrapper
    private SAMInsertSizeStatisticWorker samInsertSizeStatisticWorker

    @Before
    public void setUp() throws Exception {
        record = new SAMRecord()
        chromosomeStatisticWrapper = new ChromosomeStatisticWrapper("chr1", 1000)
        samInsertSizeStatisticWorker = new SAMInsertSizeStatisticWorker()
    }

    @After
    public void tearDown() throws Exception {
        chromosomeStatisticWrapper = null
        record = null
        samInsertSizeStatisticWorker = null
    }

    void testProcessRecordTrueEmptyHist() {
        Map histogram = [:]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(15)
        record.setProperPairFlag(true)
        samInsertSizeStatisticWorker.process(chromosomeStatisticWrapper, record)
        Map histogramExp = [15:1]
        Map histogramAct = chromosomeStatisticWrapper.insertSizeHistogram
        assertEquals(histogramExp, histogramAct)
    }

    void testProcessRecordTrueFilledHist() {
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(15)
        record.setProperPairFlag(true)
        samInsertSizeStatisticWorker.process(chromosomeStatisticWrapper, record)
        Map histogramExp = [3:10, 10:10, 15:11, 19:10, 20:10, 25:10, 30:10]
        Map histogramAct = chromosomeStatisticWrapper.insertSizeHistogram
        assertEquals(histogramExp, histogramAct)
    }

    void testProcessRecordFalseEmptyHist() {
        Map histogram = [:]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        record.setReadPairedFlag(false)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(15)
        record.setProperPairFlag(true)
        samInsertSizeStatisticWorker.process(chromosomeStatisticWrapper, record)
        Map histogramExp = [:]
        Map histogramAct = chromosomeStatisticWrapper.insertSizeHistogram
        assertEquals(histogramExp, histogramAct)
    }

    void testProcessRecordFalseFilledHist() {
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        record.setReadPairedFlag(false)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(15)
        record.setProperPairFlag(true)
        samInsertSizeStatisticWorker.process(chromosomeStatisticWrapper, record)
        Map histogramExp = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        Map histogramAct = chromosomeStatisticWrapper.insertSizeHistogram
        assertEquals(histogramExp, histogramAct)
    }

    void testPostProcess() {
        Map histogram = [9:10, 25:10]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        samInsertSizeStatisticWorker.postProcess(chromosomeStatisticWrapper)
        double meanExp = 17
        double meanAct = chromosomeStatisticWrapper.chromosome.insertSizeMean
        double medianExp = 17
        double medianAct = chromosomeStatisticWrapper.chromosome.insertSizeMedian
        double rmsExp = Math.sqrt(353)
        double rmsAct = chromosomeStatisticWrapper.chromosome.insertSizeRMS
        double sdExp = 8
        double sdAct = chromosomeStatisticWrapper.chromosome.insertSizeSD
        assertEquals(meanExp, meanAct)
        assertEquals(medianExp, medianAct)
        assertEquals(rmsExp, rmsAct)
        assertEquals(sdExp, sdAct)
    }

    void testProcessChromosomeAllEmptyCollection() {
        Collection<ChromosomeStatisticWrapper> chromosomeWrappers = []
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        Map histogramExp = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        samInsertSizeStatisticWorker.processChromosomeAll(chromosomeWrappers, chromosomeStatisticWrapper)
        Map histogramAct = chromosomeStatisticWrapper.insertSizeHistogram
        assertEquals(histogramExp, histogramAct)
    }

    void testProcessChromosomeAllCollectionWithEmptyHistogram() {
        Collection<ChromosomeStatisticWrapper> chromosomeWrappers = []
        ChromosomeStatisticWrapper chromosomeStatisticWrapperEmptyHist = new ChromosomeStatisticWrapper("chr", 1000)
        Map histogramEmpty = [:]
        chromosomeStatisticWrapperEmptyHist.insertSizeHistogram = histogramEmpty
        chromosomeWrappers.add(chromosomeStatisticWrapperEmptyHist)
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        Map histogramExp = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        samInsertSizeStatisticWorker.processChromosomeAll(chromosomeWrappers, chromosomeStatisticWrapper)
        Map histogramAct = chromosomeStatisticWrapper.insertSizeHistogram
        assertEquals(histogramExp, histogramAct)
    }

    void testProcessChromosomeAllEmptyHistogrammInChromosomeWrapper() {
        ChromosomeStatisticWrapper chromosomeStatisticWrapperEmptyHist = new ChromosomeStatisticWrapper("chr", 1000)
        Map histogramEmpty = [:]
        chromosomeStatisticWrapperEmptyHist.insertSizeHistogram = histogramEmpty
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        ChromosomeStatisticWrapper chromosomeStatisticWrapperOne = new ChromosomeStatisticWrapper("chr1", 1000)
        chromosomeStatisticWrapperOne.insertSizeHistogram = histogram
        ChromosomeStatisticWrapper chromosomeStatisticWrapperTwo = new ChromosomeStatisticWrapper("chr2", 1000)
        chromosomeStatisticWrapperTwo.insertSizeHistogram = histogram
        ChromosomeStatisticWrapper chromosomeStatisticWrapperThree = new ChromosomeStatisticWrapper("chr3", 1000)
        chromosomeStatisticWrapperThree.insertSizeHistogram = histogram
        Collection<ChromosomeStatisticWrapper> chromosomeWrappers = []
        chromosomeWrappers.add(chromosomeStatisticWrapperOne)
        chromosomeWrappers.add(chromosomeStatisticWrapperTwo)
        chromosomeWrappers.add(chromosomeStatisticWrapperThree)
        samInsertSizeStatisticWorker.processChromosomeAll(chromosomeWrappers, chromosomeStatisticWrapperEmptyHist)
        Map histogramExp = [3:30, 10:30, 15:30, 19:30, 20:30, 25:30, 30:30]
        Map histogramAct = chromosomeStatisticWrapperEmptyHist.insertSizeHistogram
        assertEquals(histogramExp, histogramAct)
    }

    void testProcessChromosomeAll() {
        ChromosomeStatisticWrapper chromosomeStatisticWrapperAll = new ChromosomeStatisticWrapper("chr", 1000)
        Map histogramAll = [1:10, 3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        chromosomeStatisticWrapperAll.insertSizeHistogram = histogramAll
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        ChromosomeStatisticWrapper chromosomeStatisticWrapperOne = new ChromosomeStatisticWrapper("chr1", 1000)
        chromosomeStatisticWrapperOne.insertSizeHistogram = histogram
        ChromosomeStatisticWrapper chromosomeStatisticWrapperTwo = new ChromosomeStatisticWrapper("chr2", 1000)
        chromosomeStatisticWrapperTwo.insertSizeHistogram = histogram
        ChromosomeStatisticWrapper chromosomeStatisticWrapperThree = new ChromosomeStatisticWrapper("chr3", 1000)
        chromosomeStatisticWrapperThree.insertSizeHistogram = histogram
        Collection<ChromosomeStatisticWrapper> chromosomeWrappers = []
        chromosomeWrappers.add(chromosomeStatisticWrapperOne)
        chromosomeWrappers.add(chromosomeStatisticWrapperTwo)
        chromosomeWrappers.add(chromosomeStatisticWrapperThree)
        samInsertSizeStatisticWorker.processChromosomeAll(chromosomeWrappers, chromosomeStatisticWrapperAll)
        Map histogramExp = [1:10, 3:40, 10:40, 15:40, 19:40, 20:40, 25:40, 30:40]
        Map histogramAct = chromosomeStatisticWrapperAll.insertSizeHistogram
        assertEquals(histogramExp, histogramAct)
    }

    void testSkipRecordReadPairedFlagTrue() {
        record.setReadPairedFlag(false)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(1)
        record.setProperPairFlag(true)
        assertTrue(samInsertSizeStatisticWorker.skipRecord(record))
    }

    void testSkipRecordMateUnmappedFlagTrue() {
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(true)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(1)
        record.setProperPairFlag(true)
        assertTrue(samInsertSizeStatisticWorker.skipRecord(record))
    }

    void testSkipRecordFirstOfPairFlagTrue() {
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(false)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(1)
        record.setProperPairFlag(true)
        assertTrue(samInsertSizeStatisticWorker.skipRecord(record))
    }

    void testSkipRecordNotPrimaryAlignmentFlagTrue() {
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(true)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(1)
        record.setProperPairFlag(true)
        assertTrue(samInsertSizeStatisticWorker.skipRecord(record))
    }

    void testSkipRecordDuplicateReadFlagTrue() {
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(true)
        record.setInferredInsertSize(1)
        record.setProperPairFlag(true)
        assertTrue(samInsertSizeStatisticWorker.skipRecord(record))
    }

    void testSkipRecordInferredInsertSizeTrue() {
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(0)
        record.setProperPairFlag(true)
        assertTrue(samInsertSizeStatisticWorker.skipRecord(record))
    }

    void testSkipRecordProperPairFlagTrue() {
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(1)
        record.setProperPairFlag(false)
        assertTrue(samInsertSizeStatisticWorker.skipRecord(record))
    }

    void testSkipRecordAllFalse() {
        record.setReadPairedFlag(true)
        record.setMateUnmappedFlag(false)
        record.setFirstOfPairFlag(true)
        record.setNotPrimaryAlignmentFlag(false)
        record.setDuplicateReadFlag(false)
        record.setInferredInsertSize(1)
        record.setProperPairFlag(true)
        assertFalse(samInsertSizeStatisticWorker.skipRecord(record))
    }

    void testAddZero() {
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        Map histogramExp = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10, 0:1]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        int value = 0
        samInsertSizeStatisticWorker.addValue(chromosomeStatisticWrapper, value)
        assertEquals(histogramExp, histogram)
    }

    void testAddNegativeValue() {
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        Map histogramExp = [3:10, 10:11, 15:10, 19:10, 20:10, 25:10, 30:10]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        int value = -10
        samInsertSizeStatisticWorker.addValue(chromosomeStatisticWrapper, value)
        assertEquals(histogramExp, histogram)
    }

    void testAddNewValue() {
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        Map histogramExp = [3:10, 5:1, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        int value = 5
        samInsertSizeStatisticWorker.addValue(chromosomeStatisticWrapper, value)
        assertEquals(histogramExp, histogram)
    }

    void testAddValueToAlreadyExists() {
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        Map histogramExp = [3:10, 10:11, 15:10, 19:10, 20:10, 25:10, 30:10]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        int value = 10
        samInsertSizeStatisticWorker.addValue(chromosomeStatisticWrapper, value)
        assertEquals(histogramExp, histogram)
    }

    void testAddValueToEmptyList() {
        Map histogram = [:]
        Map histogramExp = [10:1]
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        int value = 10
        samInsertSizeStatisticWorker.addValue(chromosomeStatisticWrapper, value)
        assertEquals(histogramExp, histogram)
    }

     void testMeanOfEmptyList() {
         Map histogram = [:]
         chromosomeStatisticWrapper.insertSizeHistogram = histogram
         samInsertSizeStatisticWorker.calculateInsertSizeStatistics(chromosomeStatisticWrapper)
         assertEquals(0, chromosomeStatisticWrapper.chromosome.insertSizeMean)
     }

     void testMedianOfEmptyList() {
         Map histogram = [:]
         chromosomeStatisticWrapper.insertSizeHistogram = histogram
         samInsertSizeStatisticWorker.calculateInsertSizeStatistics(chromosomeStatisticWrapper)
         assertEquals(0, chromosomeStatisticWrapper.chromosome.insertSizeMedian)
     }

     void testRMSOfEmptyList() {
         Map histogram = [:]
         chromosomeStatisticWrapper.insertSizeHistogram = histogram
         samInsertSizeStatisticWorker.calculateInsertSizeStatistics(chromosomeStatisticWrapper)
         assertEquals(0, chromosomeStatisticWrapper.chromosome.insertSizeRMS)
     }

     void testSDOfEmptyList() {
         Map histogram = [:]
         chromosomeStatisticWrapper.insertSizeHistogram = histogram
         samInsertSizeStatisticWorker.calculateInsertSizeStatistics(chromosomeStatisticWrapper)
         assertEquals(0, chromosomeStatisticWrapper.chromosome.insertSizeSD)
     }

     void testGetElementAtEmptyList() {
         int position = 3
         Map histogram = [:]
         chromosomeStatisticWrapper.insertSizeHistogram = histogram
         shouldFail(java.lang.ArithmeticException) {
             samInsertSizeStatisticWorker.getElementAt(chromosomeStatisticWrapper, position)
         }
     }

    void testGetElementAt() {
        int position = 12
        Map histogram = [3:10, 10:10, 15:10]
        int elementExp =10
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        int elementAct = samInsertSizeStatisticWorker.getElementAt(chromosomeStatisticWrapper, position)
        assertEquals(elementExp, elementAct)
    }

     void testGetMedianEmptyList() {
         Map histogram = [:]
         chromosomeStatisticWrapper.insertSizeHistogram = histogram
         shouldFail(java.lang.ArithmeticException) {
             samInsertSizeStatisticWorker.getMedian(chromosomeStatisticWrapper)
         }
     }

    void testGetMedianUnevenNumber() {
        Map histogram = [3:11, 10:30, 15:50]
        double medianExp = 15
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        double medianAct = samInsertSizeStatisticWorker.getMedian(chromosomeStatisticWrapper)
        assertEquals(medianExp, medianAct)
    }

    void testGetMedianEvenNumber() {
        Map histogram = [3:10, 10:30, 15:50]
        double medianExp = 15
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        double medianAct = samInsertSizeStatisticWorker.getMedian(chromosomeStatisticWrapper)
        assertEquals(medianExp, medianAct)
    }

    void testGetMedianGroupOfEvenNumber() {
        Map histogram = [3:40, 10:10, 15:30]
        double medianExp = 6.5
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        double medianAct = samInsertSizeStatisticWorker.getMedian(chromosomeStatisticWrapper)
        assertEquals(medianExp, medianAct)
    }

    void testMean() {
        Map histogram = [9:10, 25:10]
        double insertSizeMeanExp = 17
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        samInsertSizeStatisticWorker.calculateInsertSizeStatistics(chromosomeStatisticWrapper)
        double insertSizeMeanAct = chromosomeStatisticWrapper.chromosome.insertSizeMean
        assertEquals(insertSizeMeanExp, insertSizeMeanAct)
    }

    void testMedian() {
        Map histogram = [3:10, 10:30, 15:50]
        double insertSizeMedianExp = 15
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        samInsertSizeStatisticWorker.calculateInsertSizeStatistics(chromosomeStatisticWrapper)
        double insertSizeMedianAct = chromosomeStatisticWrapper.chromosome.insertSizeMedian
        assertEquals(insertSizeMedianExp, insertSizeMedianAct)
    }

    void testRMS() {
        Map histogram = [9:10, 25:10]
        double insertSizeRMSExp = Math.sqrt(353)
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        samInsertSizeStatisticWorker.calculateInsertSizeStatistics(chromosomeStatisticWrapper)
        double insertSizeRMSAct = chromosomeStatisticWrapper.chromosome.insertSizeRMS
        assertEquals(insertSizeRMSExp, insertSizeRMSAct)
    }

    void testSD() {
        Map histogram = [9:10, 25:10]
        double insertSizeSDExp = 8
        chromosomeStatisticWrapper.insertSizeHistogram = histogram
        samInsertSizeStatisticWorker.calculateInsertSizeStatistics(chromosomeStatisticWrapper)
        double insertSizeSDAct = chromosomeStatisticWrapper.chromosome.insertSizeSD
        assertEquals(insertSizeSDExp, insertSizeSDAct)
    }
}
