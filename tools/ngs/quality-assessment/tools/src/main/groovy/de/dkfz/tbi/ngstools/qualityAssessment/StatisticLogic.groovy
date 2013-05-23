package de.dkfz.tbi.ngstools.qualityAssessment

/**
 * Interface for workers to generate data from records. The data are saved either in {@link ChromosomeStatistic} or
 * {@link ChromosomeStatisticWrapper}
 *
 *
 * @param <Record> the record to use to extract datas
 */
interface StatisticLogic<Record> {

    /**
     * sets the {@link Parameters} object. That object contains the parsed input parameters.
     */
    void setParameters(Parameters parameters)

    /**
     * A method to prepare and initialize data for the given chromosome in {@link ChromosomeStatistic} and in
     * {@link ChromosomeStatisticWrapper}, if necessary
     * @param chromosome the {@link ChromosomeStatisticWrapper} to initialize
     */
    void preProcess(ChromosomeStatisticWrapper chromosome)

    /**
     * A method to collect data from the given record in {@link ChromosomeStatistic} and in
     * {@link ChromosomeStatisticWrapper}
     * @param chromosome the {@link ChromosomeStatisticWrapper} to hold the collected data
     * @param record the record to extract data from
     */
    void process(ChromosomeStatisticWrapper chromosome, Record record)

    /**
     * A method to post process data in {@link ChromosomeStatistic} and in
     * {@link ChromosomeStatisticWrapper}, if necessary
     * @param chromosome the {@link ChromosomeStatisticWrapper} to do post processing
     */
    void postProcess(ChromosomeStatisticWrapper chromosome)

    /**
     * A method to init the data for the "ALL" chromosome, if necessary
     * @param chromosomes the {@link ChromosomeStatisticWrapper}s to use for collecting the data
     * @param allChromosome the all chromosome to fill with data
     */
    void processChromosomeAll(Collection<ChromosomeStatisticWrapper> chromosomes, ChromosomeStatisticWrapper allChromosome)
}
