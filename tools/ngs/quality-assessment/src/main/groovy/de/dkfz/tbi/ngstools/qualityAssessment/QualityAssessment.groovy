package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.*

/**
 * This class reads through BAM/BAI file and divides records and bases into categories base on their quality.
 * Number of records and bases falling to each category is counted
 */
class QualityAssessment {

    private File inBamFile
    private File inBamIndex
    private SAMFileReader inputSam
    private int minAlignedRecordLength
    private int minMeanBaseQuality
    private int mappingQuality
    private int winSize
    private int coverageMappingQualityThreshold
    private ReferenceChromosome referenceChromosome
    private Genome genome
    private InsertSizes insertSizes
    private CoverageTable coverageTable
    private File results
    private File resultsCoverage
    private File resultsHistogram
    private List<SAMSequenceRecord> chrList

    /**
     * Instantiate object and initialize it with BAM file location.
     * It expects to that index file is located in the same directory with the consisting of BAM file name and appended by ".bai"
     * @param bamFile
     * @param insertSizehistogramBin Size of the histogram bin for the insert size count
     */
    QualityAssessment(String bamFile) {
        inBamFile = new File(bamFile)
        inBamIndex = new File(bamFile + ".bai")
        genome = new Genome()
        insertSizes = new InsertSizes()
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT)
        inputSam = new SAMFileReader(inBamFile, inBamIndex)
        SAMFileHeader header = inputSam.getFileHeader()
        chrList = header.getSequenceDictionary().getSequences()
        genome.init(chrList)
    }

    /**
     * Creates or recreates a results file.
     * @param filepath
     * @returns A new File object
     */
    public File createOrRecreateFile(String filepath) {
        File file = new File(filepath)
        if (file.exists()) {
            if (file.size() == 0) {
                println("File ${file.getAbsolutePath()} already exists and will be deleted (file size was 0)")
                file.delete()
            } else {
                String msg = "File ${file.getAbsolutePath()} already exists (file size bigger than 0)"
                throw new Exception(msg)
            }
        }
        file.createNewFile()
        return file
    }

    /**
     * Take the following arguments:
     * 1) path to the BAM file
     * 2) path to quality assessment results file
     * 3) path to genome coverage results file
     * 4) path to insert sizes histogram results file
     * 5) MinAlignedRecordLength
     * 6) MinMeanBaseQuality
     * 7) MappingQuality
     * 8) Coverage Mapping Quality Threshold
     * 9) Window Size
     * 10) Histogram Max threshold
     *
     */
    static void main(String [] args) {
        if (args.length < 10) {
            print("Please use following arguments to run QualityAssessment:\n"
                    + "input_bam_file results_file genomeCoverage_file MinAlignedRecordLength "
                    + "MinMeanBaseQuality MappingQuality CoverageMappingQualityThreshold WindowsSize InsertSizeCountHistogramBin\n"
                    + "suggested values are: MinAlignedRecordLength=36 MinMeanBaseQuality=25 MappingQuality=0 "
                    + "CoverageMappingQualityThreshold=1 WindowsSize=1000 InsertSizeCountHistogramBin=10\n")
            return
        }
        QualityAssessment qa = new QualityAssessment(args[0])
        qa.results = qa.createOrRecreateFile(args[1])
        qa.resultsCoverage = qa.createOrRecreateFile(args[2])
        qa.resultsHistogram = qa.createOrRecreateFile(args[3])
        qa.minAlignedRecordLength = args[4] as int
        qa.minMeanBaseQuality = args[5] as int
        qa.mappingQuality = args[6] as int
        qa.coverageMappingQualityThreshold = args[7]
        qa.winSize = args[8] as int
        qa.coverageTable = new CoverageTable(qa.winSize, qa.chrList)
        qa.countStat()

        qa.results << qa.genome.genome2JSON()
        qa.resultsCoverage << qa.coverageTable.toTrimedTab()
        qa.resultsHistogram << qa.insertSizes.getHistogramTable(args[9] as int)
    }

    void coverageQc(SAMRecord rec) {
        if (duplicateCount(rec)) {
            return
        }
        countIncorrectProperPairs(rec)
        orientationCounter(rec)
        if (recordIsQualityMapped(rec)) {
            recQualityAssessment(rec)
        } else {
            referenceChromosome.incrementNotMapped(rec)
        }
    }

    public void pairEndReadAberration(SAMRecord rec) {
        //        if (mapqFiltered & rec.getMappingQuality() == 0) {//decide whether to include this it is optional in python
        //            return
        //        }
        if (rec.getProperPairFlag()) {
            return
        }
        if (rec.getReadUnmappedFlag()) {
            return
        }
        if (rec.getMateUnmappedFlag()) {
            return
        }
        if (!rec.getReferenceName().equalsIgnoreCase(rec.getMateReferenceName())) {
            genome.getChromosome(rec.getReferenceName()).incrementEndReadAberration()
        }
    }

    /**
     * Count properties of the given record following FlagStat
     * @param rec
     */
    public void flagStat(SAMRecord rec) {
        referenceChromosome.incrementTotalReadCounter()

        if (rec.getReadFailsVendorQualityCheckFlag()) {
            referenceChromosome.incrementQCFailure()
        }
        if (rec.getDuplicateReadFlag()) {
            referenceChromosome.incrementDuplicates()
        }
        if (!rec.getReadUnmappedFlag()) {
            referenceChromosome.incrementTotalMappedReadCounter()
        }
        if (rec.getReadPairedFlag()) {
            referenceChromosome.incrementPairedInSequencing()

            if (rec.getSecondOfPairFlag()) {
                referenceChromosome.incrementPairedRead2()
            } else if (rec.getReadPairedFlag()) {
                referenceChromosome.incrementPairedRead1()
            }
            if (rec.getProperPairFlag()) {
                referenceChromosome.incrementProperlyPaired()
            }
            if (!rec.getReadUnmappedFlag() && !rec.getMateUnmappedFlag()) {
                referenceChromosome.incrementWithItselfAndMateMapped()

                if (!rec.getReferenceIndex().equals(
                rec.getMateReferenceIndex())) {
                    referenceChromosome.incrementWithMateMappedToDifferentChr()

                    if (rec.getMappingQuality() >= 5) {
                        referenceChromosome.incrementWithMateMappedToDifferentChrMaq()
                    }
                }
            }
            if (!rec.getReadUnmappedFlag() && rec.getMateUnmappedFlag()) {
                referenceChromosome.incrementSingletons()
            }
        }
    }

    /**
     * Populate the table used to construct Genome Coverage figure
     * @param rec
     */
    public void countCoverage(SAMRecord rec) {
        if (rec.getDuplicateReadFlag()) {
            return
        }
        if (rec.getMappingQuality() < coverageMappingQualityThreshold) {
            return
        }
        coverageTable.increaseCoverageCount(rec)
    }

    /**
     * This method populates data holder with insert sizes
     * @param rec
     */
    public void addInsertSize(SAMRecord rec) {
        String chr = rec.getReferenceName()
        int insertSize = rec.getInferredInsertSize()
        insertSizes.add(chr, insertSize)
    }

    public void calculateInsertSizeStatistics() {
        Iterator chrs = insertSizes.chromosomeStats.newKeyIterator()
        while (chrs.hasNext()) {
            String key = chrs.next()
            referenceChromosome = genome.getChromosome(key)
            referenceChromosome.insertSizeMean = insertSizes.chromosomeStats.get(key).mean
            referenceChromosome.insertSizeMedian = insertSizes.chromosomeStats.get(key).median
            referenceChromosome.insertSizeSD = insertSizes.chromosomeStats.get(key).standardDeviation
            referenceChromosome.insertSizeRMS = insertSizes.chromosomeStats.get(key).rms
        }
    }

    void initRefChr(SAMRecord rec) {
        genome.ensureChromosomeExists(rec.getReferenceName())
        referenceChromosome = genome.getChromosome(rec.getReferenceName())
    }

    /**
     * TODO Maybe this method deserve some comments from an informed Person.. Maybe Gideon.. Or are these method names clear for everyone?
     */
    boolean skipRecord(final SAMRecord rec) {
        return !rec.getReadPairedFlag() ||
                rec.getMateUnmappedFlag() ||
                !rec.getFirstOfPairFlag() ||
                rec.getNotPrimaryAlignmentFlag() ||
                //seems not to change anything, maybe no duplicates in test bam
                rec.getDuplicateReadFlag() ||
                rec.getInferredInsertSize() == 0 ||
                !rec.getProperPairFlag()
    }

    /**
     * This method is iterating through records and gather all informations
     */
    void countStat() {
        SAMRecordIterator itRecord = inputSam.iterator()
        while (itRecord.hasNext()) {
            final SAMRecord rec = itRecord.next()
            initRefChr(rec)
            coverageQc(rec)
            pairEndReadAberration(rec)
            flagStat(rec)
            if (!skipRecord(rec)) {
                addInsertSize(rec)
            }
            countCoverage(rec)
        }
        genome.sumUpAll()
        calculateInsertSizeStatistics()
        itRecord.close()
    }

    /**
     * Increase duplicate counter if the record has flag "Duplicate"
     * Duplicate record is defined as one which has duplicateReadFlag equal true
     * @param rec
     * @return
     */
    private boolean duplicateCount(SAMRecord rec) {
        if (!rec.getDuplicateReadFlag()) {
            return false
        }
        referenceChromosome.incrementDuplicate(rec)
        return true
    }

    /**
     * Increase counter of record pairs which:
     * a) have flag "proper pair"
     * b) map to the same chromosome
     * c) map to the same strand
     * @param rec
     */
    private void countIncorrectProperPairs(SAMRecord rec) {
        if (!rec.getProperPairFlag()) {
            return
        }
        if (!rec.getReferenceName().equals(rec.getMateReferenceName())) {
            return
        }
        if (rec.getReadNegativeStrandFlag() == rec.getMateNegativeStrandFlag()) {
            //correspond to incorrectProperPairs in coverageQc.py
            genome.getChromosome(rec.getReferenceName()).incrementProperPairStrandConflict()
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
     * @param rec
     */
    private void orientationCounter(SAMRecord rec) {
        if (rec.getReadUnmappedFlag()) {
            return
        }
        if (rec.getMateUnmappedFlag()) {
            return
        }
        if (rec.getAlignmentEnd() == 0) {
            return
        }
        if (rec.getReferenceName().equals(rec.getMateReferenceName())) {
            //corresponds to totalOrientationCounter in coverageQc.py
            referenceChromosome.incrementReferenceAgreement()
            if (rec.getReadNegativeStrandFlag() == rec.getMateNegativeStrandFlag()) {
                //corresonds to badOrientationCounter in coverageQc.py
                referenceChromosome.incrementReferenceAgreementStrandConflict()
            }
        }
    }

    /**
     * Return true if mean base quality is equal or greater then threshold
     * @param rec
     * @return
     */
    private boolean meanBaseQualityCheck(SAMRecord rec) {
        double d = Math.round(meanBaseQualPlusGaps(rec) * 1) / 1d
        return d >= minMeanBaseQuality
    }

    /**
     * Calculate mean base quality over bases which align to template and those situated in gaps between aligned blocks.
     * @param rec
     * @return
     */
    private double meanBaseQualPlusGaps(SAMRecord rec) {
        int sum = 0
        int sumLength = 0
        int beginning = Integer.MAX_VALUE
        int ending = Integer.MIN_VALUE

        for (AlignmentBlock aligmentBlock : rec.getAlignmentBlocks()) {
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
            sum += rec.getBaseQualities()[i]
        }

        //calculate average value
        double average = (double) sum / (double) sumLength
        return average
    }

    /**
     * Sum up length of all aligmentBlocks
     * @param rec
     * @return sumLength
     */
    private int getLengthOfAligmentsBlocksPlusGaps(SAMRecord rec) {
        int sumLength = 0
        int beginning = Integer.MAX_VALUE
        int ending = Integer.MIN_VALUE

        for (AlignmentBlock aligmentBlock : rec.getAlignmentBlocks()) {
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
     * This method increase the values of few of quality counters.
     * @param rec
     */
    private void recQualityAssessment(SAMRecord rec) {
        int lengthOfAligmentPlusGaps = getLengthOfAligmentsBlocksPlusGaps(rec)
        //corresponds to alignedRead.qlen (query length) from coverageQc.py
        if (lengthOfAligmentPlusGaps >= minAlignedRecordLength) {
            if (meanBaseQualityCheck(rec)) {
                referenceChromosome.incrementMappedQualityLong(rec)
                referenceChromosome.increaseQcBases(lengthOfAligmentPlusGaps)
            } else {
                referenceChromosome.incrementMappedLowQuality(rec)
            }
        } else {
            referenceChromosome.incrementMappedShort(rec)
        }
    }

    /**
     * Return true if record is:
     * a) Mapped
     * b) Has alignment end different then 0
     * c) It's mapping quality is greater then mappingQuality
     * @param rec
     * @return
     */
    private boolean recordIsQualityMapped(SAMRecord rec) {
        if (rec.getReadUnmappedFlag()) {
            return false
        }
        if (rec.getAlignmentEnd() == 0) {
            return false
        }
        if (rec.getMappingQuality() <= mappingQuality) {
            return false
        }
        return true
    }
}
