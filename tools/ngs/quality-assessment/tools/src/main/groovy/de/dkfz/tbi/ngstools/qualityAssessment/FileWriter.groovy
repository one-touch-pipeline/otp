package de.dkfz.tbi.ngstools.qualityAssessment

/**
 * Interface for the writers.
 *
 *
 */
interface FileWriter {

    /**
     * sets the {@link Parameters} object. That object contains the parsed input parameters.
     */
    public void setParameters(Parameters parameters)

    /**
     * executes the writer.
     * @param file the file to write into.
     * @param genomeStatistic reference to the data to write
     */
    public void write(File file, GenomeStatistic<?> genomeStatistic)
}
