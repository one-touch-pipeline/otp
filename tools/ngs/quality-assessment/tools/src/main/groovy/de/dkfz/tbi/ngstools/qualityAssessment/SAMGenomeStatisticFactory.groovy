package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.SAMRecord
import de.dkfz.tbi.ngstools.qualityAssessment.GenomeStatistic.ChromosomeDto

/**
 * see {@link GenomeStatisticFactory} for details
 */
class SAMGenomeStatisticFactory implements GenomeStatisticFactory<SAMRecord> {

    @Override
    public GenomeStatistic<SAMRecord> create(List<ChromosomeDto> chromosomeDtos, Parameters parameters) {
        GenomeStatistic<SAMRecord> genomeStatistic = new GenomeStatisticImpl<SAMRecord>(parameters, createStatisticLogics(parameters))
        genomeStatistic.init(chromosomeDtos)
        return genomeStatistic
    }

    private List<StatisticLogic<SAMRecord>> createStatisticLogics(Parameters parameters) {
        List<StatisticLogic<SAMRecord>> statisticLogics = [
            new SAMCountingStatisticWorker(),
            new SAMCoverageStatisticWorker(),
            new SAMInsertSizeStatisticWorker()
        ]
        statisticLogics.each { StatisticLogic statisticLogic ->
            statisticLogic.setParameters(parameters)
        }
        return statisticLogics
    }
}
