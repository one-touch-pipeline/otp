package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.SAMRecord

class SAMInsertSizeStatisticWorker extends AbstractStatisticWorker<SAMRecord> {

    @Override
    public void preProcess(ChromosomeStatisticWrapper chromosome) {
        //no pre processing needed
    }

    @Override
    public void process(ChromosomeStatisticWrapper chromosome, SAMRecord record) {
        if (!skipRecord(record)) {
            long insertSize = record.getInferredInsertSize().longValue()
            addValue(chromosome, insertSize)
        }
    }

    @Override
    public void postProcess(ChromosomeStatisticWrapper chromosome) {
        calculateInsertSizeStatistics(chromosome)
    }

    @Override
    public void processChromosomeAll(Collection<ChromosomeStatisticWrapper> chromosomeWrappers, ChromosomeStatisticWrapper allChromosomeWrapper) {
        chromosomeWrappers.each { ChromosomeStatisticWrapper chromosomeWrapper ->
            chromosomeWrapper.insertSizeHistogram.each { long value, long count ->
                Long currentCount = allChromosomeWrapper.insertSizeHistogram.get(value)
                if (!currentCount) {
                    allChromosomeWrapper.insertSizeHistogram.put(value, count)
                } else {
                    allChromosomeWrapper.insertSizeHistogram.put(value, count += currentCount)
                }
            }
        }
        calculateInsertSizeStatistics(allChromosomeWrapper)
    }

    /**
     * check, if a record should be skipped for some processing.
     */
    protected boolean skipRecord(SAMRecord record) {
        return !record.getReadPairedFlag() ||
        record.getMateUnmappedFlag() ||
        !record.getFirstOfPairFlag() ||
        record.getNotPrimaryAlignmentFlag() ||
        //seems not to change anything, maybe no duplicates in test bam
        record.getDuplicateReadFlag() ||
        record.getInferredInsertSize() == 0 ||
        !record.getProperPairFlag()
    }

    private void addValue(ChromosomeStatisticWrapper chromosome, long value) {
        value = Math.abs(value)
        Long count = chromosome.insertSizeHistogram.get(value)
        if (!count) {
            chromosome.insertSizeHistogram.put(value, 1l)
        } else {
            chromosome.insertSizeHistogram.put(value, ++count)
        }
    }

    private void calculateInsertSizeStatistics(ChromosomeStatisticWrapper chromosome) {
        long size = 0
        long sum = 0
        double s2 = 0

        chromosome.insertSizeHistogram.each { long value, long count ->
            size += count
            sum += count * value
            s2 += count * Math.pow(value, 2)
        }
        if (size != 0) {
            double mean = sum / size
            double temp = s2 / size
            chromosome.chromosome.insertSizeMean = mean
            chromosome.chromosome.insertSizeMedian = getMedian(chromosome)
            chromosome.chromosome.insertSizeRMS = Math.sqrt(temp)
            chromosome.chromosome.insertSizeSD = Math.sqrt(temp - Math.pow(mean, 2))
        }
    }

    private double getMedian(ChromosomeStatisticWrapper chromosome) {
        long countOfNumber = chromosome.insertSizeHistogram.values() ? chromosome.insertSizeHistogram.values().sum() : 0
        if (countOfNumber == 0) {
            throw new ArithmeticException()
        }
        if (countOfNumber % 2 == 0) {
            int position = countOfNumber / 2 as int
            return (getElementAt(chromosome, position) + getElementAt(chromosome, position + 1)) / 2
        } else {
            return getElementAt(chromosome, Math.ceil(countOfNumber / 2) as int)
        }
    }

    private long getElementAt(ChromosomeStatisticWrapper chromosome, long position) {
        List<Long> list = chromosome.insertSizeHistogram.keySet().sort()
        long counter = 0
        for (key in list) {
            counter += chromosome.insertSizeHistogram.get(key)
            if (counter >= position) {
                return key
            }
        }
        throw new ArithmeticException("no position " + position + " available")
    }
}
