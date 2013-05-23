package de.dkfz.tbi.ngstools.qualityAssessment

interface GenomeStatistic<Record> {

    /**
     * instances of this class must be used to provide
     * initialization information about chromosomes in init() method
     * of this interface
     */
    public static class ChromosomeDto {
        /**
         * name of the chromsome
         */
        public String chromosomeName
        /**
         * length of the chromosome with this.chromosomeName
         */
        public long chromosomeLength

        @Override
        public String toString() {
            return chromosomeName
        }
    }


    void init(List<ChromosomeDto> chromosomeDtos)

    /**
     * Process {@link StatisticLogic#preProcess} for all chromosomes for all registered {@link StatisticLogic}.
     */
    void preProcess()

    /**
     * Process {@link StatisticLogic#process} for the chromosome given by name with the given Record for all registered
     * {@link StatisticLogic}.
     *
     * @param chromosome the name of the {@link ChromosomeStatisticWrapper} the record is for
     * @param record the record to use for this chromosome
     */
    void process(String chromosomeName, Record record)

    /**
     * Process {@link StatisticLogic#postProcess} for all chromosomes and process
     * {@link StatisticLogic#processChromosomeAll} for all registered {@link StatisticLogic}.
     */
    void postProcess()

    /**
     * returns the chromosome wrappers as a map with chromosome name as key.
     * Depends on the parameter includeChromosomeAll, the all chromosome wrapper is included.
     */
    Map<String, ChromosomeStatisticWrapper> getChromosomeWrappers(boolean includeChromosomeAll)

    /**
     * returns the chromosomes including the all chromosome as a map.
     * The returned map used {@link ChromosomeStatistic} instead of {@link ChromosomeStatisticWrapper}
     */
    Map<String, ChromosomeStatistic> getChromosomes()
}
