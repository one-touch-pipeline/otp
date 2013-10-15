package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.AlignmentBlock
import net.sf.samtools.SAMRecord
import de.dkfz.tbi.ngstools.bedUtils.*

class SAMCountingStatisticWorker extends AbstractStatisticWorker<SAMRecord> {

    private TargetIntervals targetIntervals

    /**
     * assumptions:
     * fileParameters has been successfully validated:
     * <li> bedFilePath and refGenMetaInfoFilePath is not null and not empty
     * <li> both bedFile and refGenMetaInfoFile can be read and are not empty
     */
    public void init() {
        if (fileParameters.inputMode != Mode.EXOME) {
            return
        }
        List<String> referenceGenomeEntryNames = []
        File refGenMetaInfoFile = new File(fileParameters.refGenMetaInfoFilePath)
        refGenMetaInfoFile.eachLine { String line ->
            referenceGenomeEntryNames.add(line.split("\t")[0])
        }
        targetIntervals = TargetIntervalsFactory.create(fileParameters.bedFilePath, referenceGenomeEntryNames)
        if (!targetIntervals) {
            throw new NullPointerException('the external factory has failed to initialize TargetIntervals instance')
        }
    }

    @Override
    public void preProcess(ChromosomeStatisticWrapper chromosome) {
        // no pre processing
    }

    @Override
    public void process(ChromosomeStatisticWrapper chromosome, SAMRecord record) {
        doCounting(chromosome.chromosome, record)
    }

    @Override
    public void postProcess(ChromosomeStatisticWrapper chromosome) {
        //no post processing
    }

    protected boolean filterChromosome(ChromosomeStatisticWrapper chromosomeStatisticWrapper) {
        return parameters.testMode && parameters.filteredChromosomes.contains(chromosomeStatisticWrapper.chromosome.chromosomeName)
    }

    @Override
    public void processChromosomeAll(Collection<ChromosomeStatisticWrapper> chromosomeWrappers, ChromosomeStatisticWrapper allChromosomeWrapper) {
        ChromosomeStatistic allChromosome = allChromosomeWrapper.chromosome
        chromosomeWrappers.each { ChromosomeStatisticWrapper chromosomeStatisticWrapper ->
            ChromosomeStatistic chromosome = chromosomeStatisticWrapper.chromosome
            if (filterChromosome(chromosomeStatisticWrapper)) {
                println "Skip chromosome ${chromosomeStatisticWrapper} for calculation of COV parameters of ${allChromosomeWrapper}"
            } else {
                allChromosome.referenceLength += chromosome.referenceLength
                allChromosome.duplicateR1 += chromosome.duplicateR1
                allChromosome.duplicateR2 += chromosome.duplicateR2
                allChromosome.properPairStrandConflict += chromosome.properPairStrandConflict
                allChromosome.referenceAgreement += chromosome.referenceAgreement
                allChromosome.referenceAgreementStrandConflict += chromosome.referenceAgreementStrandConflict
                allChromosome.mappedQualityLongR1 += chromosome.mappedQualityLongR1
                allChromosome.mappedQualityLongR2 += chromosome.mappedQualityLongR2
                allChromosome.qcBasesMapped += chromosome.qcBasesMapped
                allChromosome.mappedLowQualityR1 += chromosome.mappedLowQualityR1
                allChromosome.mappedLowQualityR2 += chromosome.mappedLowQualityR2
                allChromosome.mappedShortR1 += chromosome.mappedShortR1
                allChromosome.mappedShortR2 += chromosome.mappedShortR2
                allChromosome.notMappedR1 += chromosome.notMappedR1
                allChromosome.notMappedR2 += chromosome.notMappedR2
            }

            allChromosome.endReadAberration += chromosome.endReadAberration
            allChromosome.totalReadCounter += chromosome.totalReadCounter
            allChromosome.qcFailedReads += chromosome.qcFailedReads
            allChromosome.duplicates += chromosome.duplicates
            allChromosome.totalMappedReadCounter += chromosome.totalMappedReadCounter
            allChromosome.pairedInSequencing += chromosome.pairedInSequencing
            allChromosome.pairedRead2 += chromosome.pairedRead2
            allChromosome.pairedRead1 += chromosome.pairedRead1
            allChromosome.properlyPaired += chromosome.properlyPaired
            allChromosome.withItselfAndMateMapped += chromosome.withItselfAndMateMapped
            allChromosome.withMateMappedToDifferentChr += chromosome.withMateMappedToDifferentChr
            allChromosome.withMateMappedToDifferentChrMaq += chromosome.withMateMappedToDifferentChrMaq
            allChromosome.singletons += chromosome.singletons

            allChromosome.allBasesMapped += chromosome.allBasesMapped
            if (targetIntervals) {
                allChromosome.onTargetMappedBases += chromosome.onTargetMappedBases
            }
        }
    }

    private void doCounting(ChromosomeStatistic chromosome, SAMRecord record) {
        coverageQc(chromosome, record)
        pairEndReadAberration(chromosome, record)
        flagStat(chromosome, record)
        countBasesMapped(chromosome, record)
    }

    private void coverageQc(ChromosomeStatistic chromosome, SAMRecord record) {
        if (duplicateCount(chromosome, record)) {
            return
        }
        countIncorrectProperPairs(chromosome, record)
        orientationCounter(chromosome, record)
        if (recordIsQualityMapped(record)) {
            recQualityAssessment(chromosome, record)
        } else {
            if (record.getSecondOfPairFlag()) {
                chromosome.notMappedR2++
            } else {
                chromosome.notMappedR1++
            }
        }
    }

    public void pairEndReadAberration(ChromosomeStatistic chromosome, SAMRecord record) {
        //if (mapqFiltered & record.getMappingQuality() == 0) {//decide whether to include this it is optional in python
        //      return
        //}
        if (record.getProperPairFlag()) {
            return
        }
        if (record.getReadUnmappedFlag()) {
            return
        }
        if (record.getMateUnmappedFlag()) {
            return
        }
        if (!record.getReferenceName().equalsIgnoreCase(record.getMateReferenceName())) {
            chromosome.endReadAberration++
        }
    }

    /**
     * Count properties of the given record following FlagStat
     * @param record the analyzed {@link SAMRecord}
     */
    public void flagStat(ChromosomeStatistic chromosome, SAMRecord record) {
        chromosome.totalReadCounter++
        if (record.getReadFailsVendorQualityCheckFlag()) {
            chromosome.qcFailedReads++
        }
        if (record.getDuplicateReadFlag()) {
            chromosome.duplicates++
        }
        if (!record.getReadUnmappedFlag()) {
            chromosome.totalMappedReadCounter++
        }
        if (record.getReadPairedFlag()) {
            chromosome.pairedInSequencing++
            if (record.getSecondOfPairFlag()) {
                chromosome.pairedRead2++
            } else if (record.getFirstOfPairFlag()) {
                chromosome.pairedRead1++
            }
            if (record.getProperPairFlag()) {
                chromosome.properlyPaired++
            }
            if (!record.getReadUnmappedFlag() && !record.getMateUnmappedFlag()) {
                chromosome.withItselfAndMateMapped++
                if (record.getReferenceIndex() != record.getMateReferenceIndex()) {
                    chromosome.withMateMappedToDifferentChr++
                    if (record.getMappingQuality() >= 5) {
                        chromosome.withMateMappedToDifferentChrMaq++
                    }
                }
            }
            if (!record.getReadUnmappedFlag() && record.getMateUnmappedFlag()) {
                chromosome.singletons++
            }
        }
    }

    /**
     * Counting of two values:
     * - allBasesMapped -> all bases mapped to the reference genome
     * - onTargetMappedBases -> bases mapped to the target regions
     *
     * The calculation is grouped in one method for the case when a filter is used.
     * It has to be ensured that this filter is the same for both calculations.
     *
     * Gaps are not counted.
     *
     * @param chromosome (chromosome wrapper), to which the record belongs to
     * @param record, which contains the read
     */
    private void countBasesMapped(ChromosomeStatistic chromosome, SAMRecord record) {
        if (recordIsQualityMapped(record) && !record.getDuplicateReadFlag() && meanBaseQualityCheck(record)) {
            List <AlignmentBlock> alignmentBlocks = record.getAlignmentBlocks()
            alignmentBlocks.each { alignmentBlock ->
                long alignmentBlockLength = alignmentBlock.length
                chromosome.allBasesMapped += alignmentBlockLength
                // check if alignment block overlaps with target interval(s) and count overlap
                if (targetIntervals) {
                    long alignmentBlockStart = alignmentBlock.getReferenceStart()
                    long alignmentBlockEnd = alignmentBlockStart + alignmentBlockLength - 1
                    chromosome.onTargetMappedBases += targetIntervals.getOverlappingBaseCount(chromosome.chromosomeName, alignmentBlockStart - 1, alignmentBlockEnd)
                }
            }
        }
    }

    /**
     * Increase duplicate counter if the record has flag "Duplicate"
     * Duplicate record is defined as one which has duplicateReadFlag equal true
     * @param record the analyzed {@link SAMRecord}
     * @return
     */
    private boolean duplicateCount(ChromosomeStatistic chromosome, SAMRecord record) {
        if (!record.getDuplicateReadFlag()) {
            return false
        }
        if (!record.getReadPairedFlag() || record.getFirstOfPairFlag()) {
            chromosome.duplicateR1++
        } else {
            chromosome.duplicateR2++
        }
        return true
    }

    /**
     * Increase counter of record pairs which:
     * a) have flag "proper pair"
     * b) map to the same chromosome
     * c) map to the same strand
     * @param record the analyzed {@link SAMRecord}
     */
    private void countIncorrectProperPairs(ChromosomeStatistic chromosome, SAMRecord record) {
        if (!record.getProperPairFlag()) {
            return
        }
        if (record.getReferenceName() != record.getMateReferenceName()) {
            return
        }
        if (record.getReadNegativeStrandFlag() == record.getMateNegativeStrandFlag()) {
            //correspond to incorrectProperPairs in coverageQc.py
            chromosome.properPairStrandConflict++
        }
    }

    /**
     * Increase counter of read pairs which:
     * For totalOrientationCounter
     * a) are not unmapped
     * b) map to the same chromosome
     * For badOrientationCounter
     * a) are not unmapped
     * b) map to the same chromosome
     * c) map to the same strand
     * @param record the analyzed {@link SAMRecord}
     */
    private void orientationCounter(ChromosomeStatistic chromosome, SAMRecord record) {
        if (record.getReadUnmappedFlag()) {
            return
        }
        if (record.getMateUnmappedFlag()) {
            return
        }
        if (record.getAlignmentEnd() == 0) {
            return
        }
        if (record.getReferenceName() == record.getMateReferenceName()) {
            //corresponds to totalOrientationCounter in coverageQc.py
            chromosome.referenceAgreement++
            if (record.getReadNegativeStrandFlag() == record.getMateNegativeStrandFlag()) {
                //corresonds to badOrientationCounter in coverageQc.py
                chromosome.referenceAgreementStrandConflict++
            }
        }
    }

    /**
     * Return true if mean base quality is equal or greater then threshold
     * @param record the analyzed {@link SAMRecord}
     * @return
     */
    private boolean meanBaseQualityCheck(SAMRecord record) {
        double d = Math.round(meanBaseQualPlusGaps(record))
        return d >= parameters.minMeanBaseQuality
    }

    /**
     * Calculate mean base quality over bases which align to template and those situated in gaps between aligned blocks.
     * @param record the analyzed {@link SAMRecord}
     * @return
     */
    private double meanBaseQualPlusGaps(SAMRecord record) {
        int sum = 0
        int sumLength = 0
        int beginning = Integer.MAX_VALUE
        int ending = Integer.MIN_VALUE

        for (AlignmentBlock aligmentBlock : record.getAlignmentBlocks()) {
            int blockStart = aligmentBlock.getReadStart()
            beginning = (blockStart < beginning) ? blockStart : beginning
            int blockLength = aligmentBlock.getLength()
            int blockEnd = blockStart + blockLength - 1
            ending = (blockEnd > ending) ? blockEnd : ending
        }
        sumLength = ending - beginning + 1
        if (sumLength <= 0) {
            return 0
        }

        //sum up values over whole stretch including both alignments and gaps
        int start = beginning - 1
        int end = start + sumLength
        for (int i = start; i < end;  i++) {
            sum += record.getBaseQualities()[i]
        }

        //calculate average value
        double average = (double) sum / (double) sumLength
        return average
    }

    /**
     * Sum up length of all aligmentBlocks
     * @param record the analyzed {@link SAMRecord}
     * @return sumLength
     */
    private int getLengthOfAligmentsBlocksPlusGaps(SAMRecord record) {
        int sumLength = 0
        int beginning = Integer.MAX_VALUE
        int ending = Integer.MIN_VALUE

        for (AlignmentBlock aligmentBlock : record.getAlignmentBlocks()) {
            int blockStart = aligmentBlock.getReadStart()
            beginning = (blockStart < beginning) ? blockStart : beginning
            int blockLength = aligmentBlock.getLength()
            int blockEnd = blockStart + blockLength - 1
            ending = (blockEnd > ending) ? blockEnd : ending
        }
        sumLength = ending - beginning + 1
        return Math.max(sumLength, 0)
    }

    /**
     * This method increase the values of few quality counters.
     * @param record the analyzed {@link SAMRecord}
     */
    private void recQualityAssessment(ChromosomeStatistic chromosome, SAMRecord record) {
        int lengthOfAligmentPlusGaps = getLengthOfAligmentsBlocksPlusGaps(record)
        //corresponds to alignedRead.qlen (query length) from coverageQc.py
        if (lengthOfAligmentPlusGaps >= parameters.minAlignedRecordLength) {
            if (meanBaseQualityCheck(record)) {
                if (record.getSecondOfPairFlag()) {
                    chromosome.mappedQualityLongR2++
                } else {
                    chromosome.mappedQualityLongR1++
                }
                chromosome.qcBasesMapped += lengthOfAligmentPlusGaps
            } else {
                if (record.getSecondOfPairFlag()) {
                    chromosome.mappedLowQualityR2++
                } else {
                    chromosome.mappedLowQualityR1++
                }
            }
        } else {
            if (record.getSecondOfPairFlag()) {
                chromosome.mappedShortR2++
            } else {
                chromosome.mappedShortR1++
            }
        }
    }

    /**
     * Return true if record is:
     * a) Mapped
     * b) Has alignment end different then 0
     * c) It's mapping quality is greater then mappingQuality
     * @param record
     * @return
     */
    private boolean recordIsQualityMapped(SAMRecord record) {
        if (record.getReadUnmappedFlag()) {
            return false
        }
        if (record.getAlignmentEnd() == 0) {
            return false
        }
        if (record.getMappingQuality() <= parameters.mappingQuality) {
            return false
        }
        return true
    }
}
