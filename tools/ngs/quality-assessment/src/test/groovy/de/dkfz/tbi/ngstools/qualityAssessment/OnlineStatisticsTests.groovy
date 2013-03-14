package de.dkfz.tbi.ngstools.qualityAssessment

class OnLineStatisticsTests extends GroovyTestCase {

    void testSampleSize() {
        List lista = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        lista.each {
            onlineStatistics.add(it)
        }
        assert onlineStatistics.sampleSize == lista.size()
    }

    void testMedianFromOddNumberOfElements() {
        List unsorted = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]  // it is not yet but will be shuffled next
        Collections.shuffle(unsorted)
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        unsorted.each {
            onlineStatistics.add(it)
        }
        assert onlineStatistics.median == 6
    }

    void testMedianFromEvenNumberOfElements() {
        List unsorted = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]  // it is not yet but will be shuffled next
        Collections.shuffle(unsorted)
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        unsorted.each {
            onlineStatistics.add(it)
        }
        assert onlineStatistics.getMedian() == 5.5
    }

    void testMedianFromEmptyListOfElements() {
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        shouldFail(ArithmeticException) {
            onlineStatistics.median
        }
    }

    void testMedianFromListWithSingleElement() {
        int singleElement = 3
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        onlineStatistics.add(singleElement)
        assert onlineStatistics.median == singleElement
    }

    void testCanAccessMedianDirectly() {
        int singleElement = 3
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        onlineStatistics.add(singleElement)
        assert onlineStatistics.median == onlineStatistics.getMedian()
    }

    void testCanAccessMeanDirectly() {
        int singleElement = 3
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        onlineStatistics.add(singleElement)
        assert onlineStatistics.mean == onlineStatistics.getMean()
    }

    void testMeanFromListOfElements() {
        List unsorted = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]  // it is not yet but will be shuffled next
        Collections.shuffle(unsorted)
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        unsorted.each {
            onlineStatistics.add(it)
        }
        assert onlineStatistics.mean == 6
    }

    void testMeanFromEmptyListOfElements() {
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        shouldFail(ArithmeticException) {
            onlineStatistics.mean
        }
    }

    void testMeanFromListWithSingleElement() {
        int singleElement = 3
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        onlineStatistics.add(singleElement)
        assert onlineStatistics.mean == singleElement
    }

    void testCanAccessStandardDeviationDirectly() {
        int singleElement = 3
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        onlineStatistics.add(singleElement)
        assert onlineStatistics.standardDeviation == onlineStatistics.getStandardDeviation()
    }

    void testStandardDeviationFromListOfElements() {
        List unsorted = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]  // it is not yet but will be shuffled next
        Collections.shuffle(unsorted)
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        unsorted.each {
            onlineStatistics.add(it)
        }
        assert onlineStatistics.standardDeviation == 3.1622776601683795
    }

    void testStandardDeviationFromEmptyListOfElements() {
        int initialCapacity = 10
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        shouldFail(ArithmeticException) {
            onlineStatistics.standardDeviation
        }
    }

    void testStandardDeviationFromListWithSingleElement() {
        int singleElement = 3
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        onlineStatistics.add(singleElement)
        assert onlineStatistics.standardDeviation == 0
    }

    void testCanAccessRmsDirectly() {
        int singleElement = 3
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        onlineStatistics.add(singleElement)
        assert onlineStatistics.rms == onlineStatistics.getRms()
    }

    void testRmsFromListOfElements() {
        List unsorted = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]  // it is not yet but will be shuffled next
        Collections.shuffle(unsorted)
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        unsorted.each {
            onlineStatistics.add(it)
        }
        assert onlineStatistics.rms == 6.782329983125268
    }

    void testRmsFromEmptyListOfElements() {
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        shouldFail(ArithmeticException) {
            onlineStatistics.rms
        }
    }

    void testRmsFromListWithSingleElement() {
        int singleElement = 3
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        onlineStatistics.add(singleElement)
        assert onlineStatistics.rms == 3
    }

    void testRealDataProcessing() {
        // temporary test data.. Maybe it should be replace by some real file in some test data repository in the future..
        File file = new File("$HOME/tumor_run121026_SN952_0117_BC19ADACXX_paired.bam.sorted.bam.insertSizes")
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        file.eachLine {
            onlineStatistics.add(it as int)
        }
        assert onlineStatistics.median == 248
    }

    void testToListEmtpy() {
        List list = []
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        assert (list == onlineStatistics.toList())
    }

    void testToListRepeatedElements() {
        List list = [1, 1, 2, 2, 2, 3, 3, 3, 3]
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        list.each {
            onlineStatistics.add(it)
        }
        assert (list == onlineStatistics.toList())
    }

    void testToListNoRepeatedElements() {
        List list = [1, 2, 3]
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        list.each {
            onlineStatistics.add(it)
        }
        assert (list == onlineStatistics.toList())
    }

    void testHistogramWithoutBin() {
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        List list = onlineStatistics.toList(histogram)
        list.each {
            onlineStatistics.add(it as int)
        }
        assert onlineStatistics.getHistogram() == histogram
    }

    void testHistogramWithBin() {
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        List list = onlineStatistics.toList(histogram)
        list.each {
            onlineStatistics.add(it as int)
        }
        int binSize = 10
        assert onlineStatistics.getHistogram(binSize) == [0:10, 10:30, 20:20, 30:10]
    }

    void testHistogramWithBinValuesNotStartingAtFirstDefaultBin() {
        Map histogram = [53:10, 60:10, 65:10]
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        List list = onlineStatistics.toList(histogram)
        list.each {
            onlineStatistics.add(it as int)
        }
        int binSize = 10

        assert onlineStatistics.getHistogram(binSize) == [50:10, 60:20]
    }
}
