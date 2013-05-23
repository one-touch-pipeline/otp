package de.dkfz.tbi.ngstools.qualityAssessment

class CoverageWriter implements FileWriter {

    private Parameters parameters

    @Override
    public void setParameters(Parameters parameters) {
        this.parameters = parameters
    }

    @Override
    public void write(File file, GenomeStatistic<?> genomeStatistic) {
        file << coverageToTrimedTab(genomeStatistic)
    }

    /**
     * convert coverage results to tabulated string
     */
    private String coverageToTrimedTab(GenomeStatistic<?> genomeStatistic) {
        int winSize = parameters.winSize
        StringBuffer sb = new StringBuffer()
        Map<String, ChromosomeStatisticWrapper> map = genomeStatistic.getChromosomeWrappers(false)
        map.each { String key, ChromosomeStatisticWrapper chromosome ->
            chromosome.coverageTable.eachWithIndex { long value, int index ->
                int loc = index * winSize
                sb.append(key)
                sb.append("\t")
                sb.append(loc)
                sb.append("\t")
                sb.append(value)
                sb.append("\n")
            }
        }
        return sb.toString()
    }
}
