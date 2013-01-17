package de.dkfz.tbi.ngstools.coverageQc

import java.io.File
import net.sf.samtools.*

/**
 * This class reads through BAM/BAI file and divides records and bases into categories base on their quality.
 * Number of records and bases falling to each category is counted
 */
 class CoverageQc {

    private File inBamFile
    private File inBamIndex
    private SAMFileReader inputSam
    private int minAlignedRecordLength
    private int minMeanBaseQuality
    private int mappingQuality
    private ReferenceChromosome referenceChromosome
    private final Genome genome
    private File results

    /**
     * Instantiate CoverageQc object and initialize it with BAM file location.
     * It expects to that index file is located in the same directory with the consisting of BAM file name and appended by ".bai"
     * @param bamFile
     */
     CoverageQc(String bamFile) {
        inBamFile = new File(bamFile)
        inBamIndex = new File(bamFile + ".bai")
        genome = new Genome()
    }

    /**
     * Set and initialize results file.
     */
     void setResults(File res) {
        results = res
        if (results.exists()) {
            results.delete()
        }
        results.createNewFile()
    }

    /**
     * Take the following arguments:
     * 1) path to the BAM file
     * 2) path to results file
     * 3) MinAlignedRecordLength
     * 4) MinMeanBaseQuality
     * 5) MappingQuality
     */
    static void main(String [] args) {
        if (args.length < 5) {
            print("Please use following arguments to run this program:\n"
                    + "input_bam_file results_file MinAlignedRecordLength MinMeanBaseQuality MappingQuality\n"
                    + "default values are MinAlignedRecordLength=36 MinMeanBaseQuality=25 MappingQuality=0\n")
            return
        }
        CoverageQc qc = new CoverageQc(args[0])
        qc.setResults(new File(args[1]))
        qc.setMinAlignedRecordLength(new Integer(args[2]))
        qc.setMinMeanBaseQuality(new Integer(args[3]))
        qc.setMappingQuality(new Integer(args[4]))
        qc.countCoverage()
        qc.results << qc.genome.genome2JSON()
    }

    /**
     * This method is iterating through records and gather all informations
     */
     void countCoverage() {
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.LENIENT)
        inputSam = new SAMFileReader(inBamFile, inBamIndex)
        SAMRecordIterator itRecord = inputSam.iterator()
        while (itRecord.hasNext()) {
            final SAMRecord rec = itRecord.next()
            if (duplicateCount(rec)) {
                continue
            }
            genome.assertChromosome(rec.getReferenceName())
            referenceChromosome = genome.getChromosome(rec.getReferenceName())
            countIncorrectProperPairs(rec)
            orientationCounter(rec)
            if (recordIsQualityMapped(rec)) {
                qualityAssesment(rec)
            } else {
                referenceChromosome.incrementNotMapped(rec)
            }
        }
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
        if (d >= minMeanBaseQuality) {
            return true
        }
        return false
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
    private void qualityAssesment(SAMRecord rec) {
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
