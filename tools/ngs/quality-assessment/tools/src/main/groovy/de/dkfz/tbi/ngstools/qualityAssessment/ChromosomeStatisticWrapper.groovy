package de.dkfz.tbi.ngstools.qualityAssessment

/**
 * Extends the {@link ChromosomeStatistic} with some temporary needed values.
 *
 *
 */
class ChromosomeStatisticWrapper {

    /**
     * The {@link ChromosomeStatistic} which are wrapped which additional parameters.
     */
    ChromosomeStatistic chromosome

    /**
     * holding data to calculate statistic values
     */
    Map<Long, Long> insertSizeHistogram = new HashMap<Long, Long>()

    /**
     * holds the coverage table
     */
    long[] coverageTable

    public ChromosomeStatisticWrapper(String name, long referenceLength = 0) {
        chromosome = new ChromosomeStatistic(name, referenceLength)
    }

    @Override
    public String toString() {
        return chromosome.toString()
    }
}
