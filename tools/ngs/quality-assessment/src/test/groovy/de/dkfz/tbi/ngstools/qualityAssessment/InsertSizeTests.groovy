package de.dkfz.tbi.ngstools.qualityAssessment

class InsertSizeTests extends GroovyTestCase {

    void testAddInsertSizeForRegularChromosome() {
        InsertSizes insertSizes = new InsertSizes()
        String chr1 = "chr1"
        int value = 10
        int numberOfInsertSizes = 5
        numberOfInsertSizes.times {
            insertSizes.add(chr1, value)
        }
        assert (insertSizes.chromosomeStats.get(chr1).getHistogram().get(value) == numberOfInsertSizes)
    }

    void testAddInsertSizeForChromosomeAll() {
        InsertSizes insertSizes = new InsertSizes()
        // the chromosome all is incremented when a "regular" chromosome is added
        String chr1 = "chr1"
        int value = 10
        int numberOfInsertSizes = 5
        numberOfInsertSizes.times {
            insertSizes.add(chr1, value)
        }
        assert (insertSizes.chromosomeStats.get("ALL").getHistogram().get(value) == numberOfInsertSizes)
    }

    void testAddInsertSizeWithNegativeValues() {
        InsertSizes insertSizes = new InsertSizes()
        // the chromosome all is incremented when a "regular" chromosome is added
        String chr1 = "chr1"
        int value = 10
        int numberOfInsertSizes = 5
        numberOfInsertSizes.times {
            insertSizes.add(chr1, value)
        }
        // now some negative values..
        numberOfInsertSizes.times {
            insertSizes.add(chr1, -value)
        }
        assert (insertSizes.chromosomeStats.get("ALL").getHistogram().get(value) == 2 * numberOfInsertSizes)
    }

    void testAddInsertSizeForDifferentChromosomes() {
        InsertSizes insertSizes = new InsertSizes()
        String chr1 = "chr1"
        String chr2 = "chr2"
        String chr3 = "chr3"
        List chromosomes = [chr1, chr2, chr3]
        int value = 10
        chromosomes.each { String chr ->
            insertSizes.add(chr, value)
        }
        assert (insertSizes.chromosomeStats.get("ALL").getHistogram().get(value) == chromosomes.size())
    }

    void testHistogramTableForChromosome() {
        InsertSizes insertSizes = new InsertSizes()
        int binSize = 10

        // sample histogram with 1 case at first bin, 2 cases at second bin
        Map histogram = [3:10, 10:10, 15:10, 19:10, 20:10, 25:10, 30:10]
        OnlineStatistics onlineStatistics = new OnlineStatistics()
        List list = onlineStatistics.toList(histogram)
        list.each {
            onlineStatistics.add(it as int)
        }
        String chrName = "chr1"
        insertSizes.chromosomeStats = [(chrName): onlineStatistics]

        // not sure which way would be more readable...
        String histogramTable = "${chrName}\t0\t10\n${chrName}\t10\t30\n${chrName}\t20\t20\n${chrName}\t30\t10\n"
//        String histogramTable ="""${chrName}\t0\t10
//${chrName}\t10\t30
//${chrName}\t20\t20
//${chrName}\t30\t10
//"""
        assert (insertSizes.getHistogramTable(binSize) == histogramTable)
    }
}
