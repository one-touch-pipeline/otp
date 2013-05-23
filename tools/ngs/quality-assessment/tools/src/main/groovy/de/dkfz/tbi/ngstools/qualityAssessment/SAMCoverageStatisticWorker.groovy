package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.SAMRecord

class SAMCoverageStatisticWorker extends AbstractStatisticWorker<SAMRecord> {

    @Override
    public void preProcess(ChromosomeStatisticWrapper chromosome) {
        int arraySize = Math.ceil(chromosome.chromosome.referenceLength / parameters.winSize)
        chromosome.coverageTable = new long[arraySize]
    }

    @Override
    public void process(ChromosomeStatisticWrapper chromosome, SAMRecord record) {
        countCoverage(chromosome, record)
    }

    @Override
    public void postProcess(ChromosomeStatisticWrapper chromosome) {
        //no post processing
    }

    @Override
    public void processChromosomeAll(Collection<ChromosomeStatisticWrapper> chromosomeWrappers, ChromosomeStatisticWrapper allChromosomeWrapper) {
        //no all chromosome processing
    }

    private void countCoverage(ChromosomeStatisticWrapper chromosomeWrapper, SAMRecord record) {
        if (record.getDuplicateReadFlag()) {
            return
        }
        if (record.getMappingQuality() < parameters.coverageMappingQualityThreshold) {
            return
        }
        //The -1 is necessary to create equal result as the python library
        int beginning = record.getAlignmentStart() - 1
        int locBin = Math.floor(beginning / parameters.winSize)
        chromosomeWrapper.coverageTable[locBin]++
    }
}
