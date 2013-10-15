package de.dkfz.tbi.ngstools.qualityAssessment

/**
 * implementation of this interface will read the given bam file
 * and calculate quality statistics
 * @param <Record> - a record of a bam file, e.g. {@link SAMRecord}
 */
interface BamFileReader<Record> {

    /**
     * reads the given bamFile and calculates quality statistics for this file
     *
     * @param bamFile - bam file for which the quality statistics must be calculated
     * @param indexFile - index file for the given bam file
     * @return instance of {@link GenomeStatistic} having all the calculate
     * quality statistics
     */
    GenomeStatistic<Record> read(File bamFile, File indexFile)

    void setParameters(Parameters parameters)

    void setFileParameters(FileParameters FileParameters)

    void setGenomeStatisticFactory(GenomeStatisticFactory<Record> factory)
}
