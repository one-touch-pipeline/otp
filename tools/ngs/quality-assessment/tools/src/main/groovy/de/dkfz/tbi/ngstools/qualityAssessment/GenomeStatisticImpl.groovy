package de.dkfz.tbi.ngstools.qualityAssessment

import de.dkfz.tbi.ngstools.qualityAssessment.GenomeStatistic.ChromosomeDto

class GenomeStatisticImpl<Record> implements GenomeStatistic<Record> {

    private final Map<String, ChromosomeStatisticWrapper> chromosomesWrappers = new LinkedHashMap<String, ChromosomeStatisticWrapper>()

    private final ChromosomeStatisticWrapper allChromosomeWrapper

    private final List<StatisticLogic<Record>> statisticLogics

    private final Parameters parameters

    public GenomeStatisticImpl(Parameters parameters, List<StatisticLogic<Record>> statisticLogics) {
        this.parameters = parameters
        this.statisticLogics = statisticLogics
        allChromosomeWrapper = new ChromosomeStatisticWrapper(parameters.allChromosomeName)
    }

    private ChromosomeStatisticWrapper getOrCreateChromosomeStatisticWrapper(String name) {
        ChromosomeStatisticWrapper chromosome = chromosomesWrappers.get(name)
        if (chromosome == null) {
            println "create chromosome for ${name}, not in ${chromosomesWrappers.keySet()}"
            chromosome = new ChromosomeStatisticWrapper(name)
            chromosomesWrappers.put(name, chromosome)
        }
        return chromosome
    }

    @Override
    public void init(List<ChromosomeDto> chromosomeDtos) {
        chromosomeDtos.each { ChromosomeDto chromosomeDto ->
            ChromosomeStatisticWrapper wrapper = new ChromosomeStatisticWrapper(chromosomeDto.chromosomeName, chromosomeDto.chromosomeLength)
            chromosomesWrappers.put(wrapper.chromosome.chromosomeName, wrapper)
        }
    }

    @Override
    public void preProcess() {
        statisticLogics.each { StatisticLogic<Record> statisticLogic ->
            chromosomesWrappers.values().each { ChromosomeStatisticWrapper chromosome ->
                statisticLogic.preProcess(chromosome)
            }
        }
    }

    @Override
    public void process(String chromosomeName, Record record) {
        ChromosomeStatisticWrapper chromosome = getOrCreateChromosomeStatisticWrapper(chromosomeName)
        statisticLogics.each { StatisticLogic<Record> statisticLogic ->
            statisticLogic.process(chromosome, record)
        }
    }

    @Override
    public void postProcess() {
        statisticLogics.each { StatisticLogic<Record> statisticLogic ->
            chromosomesWrappers.values().each { ChromosomeStatisticWrapper chromosome ->
                statisticLogic.postProcess(chromosome)
            }
            statisticLogic.processChromosomeAll(chromosomesWrappers.values(), allChromosomeWrapper)
        }
    }

    @Override
    public Map<String, ChromosomeStatisticWrapper> getChromosomeWrappers(boolean includeChromosomeAll) {
        Map<String, ChromosomeStatisticWrapper> map = chromosomesWrappers.clone()
        if (includeChromosomeAll){
            map.put(allChromosomeWrapper.chromosome.chromosomeName, allChromosomeWrapper)
        }
        return map
    }

    @Override
    public Map<String, ChromosomeStatistic> getChromosomes() {
        Map<String, ChromosomeStatistic> map = [:]
        chromosomesWrappers.each { String key, ChromosomeStatisticWrapper chromosome ->
            map.put(key, chromosome.chromosome)
        }
        map.put(allChromosomeWrapper.chromosome.chromosomeName, allChromosomeWrapper.chromosome)
        return map
    }
}
