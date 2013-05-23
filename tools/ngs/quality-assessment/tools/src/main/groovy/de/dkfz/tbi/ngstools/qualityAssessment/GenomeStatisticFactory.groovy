package de.dkfz.tbi.ngstools.qualityAssessment

import de.dkfz.tbi.ngstools.qualityAssessment.GenomeStatistic.ChromosomeDto

/**
 * implementation of this interface will initialize an instance
 * of {@link GenomeStatistic} depending on the upderlying library used
 * for bam parsing. The returned instance is provided with a corresponding
 * list of implementations of {@link StatisticLogic} to perform statistic gethering.
 */
interface GenomeStatisticFactory<Record> {

    /**
     * creates fully initialized instance of GenomeStatistic
     * @param chromosomeDtos see {@link GenomeStatistic} for details
     * @return initialized instance of {@link GenomeStatistic}
     */
    GenomeStatistic<Record> create(List<ChromosomeDto> chromosomeDtos, Parameters parameters)
}
