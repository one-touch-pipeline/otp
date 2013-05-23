package de.dkfz.tbi.ngstools.qualityAssessment


class HistogramWriter implements FileWriter {

    private Parameters parameters

    @Override
    public void setParameters(Parameters parameters) {
        this.parameters = parameters
    }

    @Override
    public void write(File file, GenomeStatistic<?> genomeStatistic) {
        file << getHistogramTable(genomeStatistic)
    }

    private String getHistogramTable(GenomeStatistic<?> genomeStatistic) {
        StringBuffer sb = new StringBuffer()
        Map<String, ChromosomeStatisticWrapper> map = genomeStatistic.getChromosomeWrappers(true)
        map.each { String chromosome, ChromosomeStatisticWrapper chromosomeStatisticWrapper ->
            Map<Integer, Integer> histogram = getHistogram(chromosomeStatisticWrapper)
            histogram.each { long bin, long count ->
                sb.append("${chromosome}\t${bin}\t${count}\n")
            }
        }
        return sb.toString()
    }

    /**
     * Retrieves the histogram in form of a Map where the value are the counts per bin (going from the maximum bin that contains any value and with step of binSize)
     * @returns A Map where the keys are sorted by bin
     */
    public Map<Long, Long> getHistogram(ChromosomeStatisticWrapper chromosome) {
        if (chromosome.insertSizeHistogram.isEmpty()) {
            println "empty histogram for ${chromosome}"
            return [:]
        }
        long binSize = parameters.binSize
        if (binSize == 0) {
            return chromosome.insertSizeHistogram
        }
        Map<Long, Long> histogramBin = new TreeMap<Long, Long>()
        List<Long> keys = chromosome.insertSizeHistogram.keySet().sort()
        // first bin of the histogram
        long bin = keys.first() - keys.first() % binSize
        keys.each { long key ->
            while (key >= (bin + binSize)) {
                bin += binSize
                histogramBin.put(bin, 0l)
            }
            long count = (!histogramBin.get(bin)) ? 0l : histogramBin.get(bin)
            histogramBin.put(bin, count + chromosome.insertSizeHistogram.get(key))
        }
        return histogramBin
    }

}
