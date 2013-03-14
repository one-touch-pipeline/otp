package de.dkfz.tbi.ngstools.qualityAssessment

public class InsertSizes {

    private Map<String, OnlineStatistics> chromosomeStats = new LinkedHashMap<String, OnlineStatistics>()

    public void add(String chr, int insertSize) {
        insertSize = Math.abs(insertSize)
        getStatistics(chr).add(insertSize)
        getStatistics("ALL").add(insertSize)
    }

    public OnlineStatistics getStatistics(String name) {
        OnlineStatistics stats = chromosomeStats.get(name)
        if (!stats) {
            stats = new OnlineStatistics()
            chromosomeStats.put(name, stats)
        }
        return stats
    }

    public String getHistogramTable(int binSize) {
        StringBuffer sb = new StringBuffer()
        chromosomeStats.each { String chromosome, OnlineStatistics stats ->
            Map<Integer, Integer> histogram = stats.getHistogram(binSize)
            histogram.each { int bin, int count ->
                sb.append("${chromosome}\t${bin}\t${count}\n")
            }
        }
        return sb.toString()
    }
}
