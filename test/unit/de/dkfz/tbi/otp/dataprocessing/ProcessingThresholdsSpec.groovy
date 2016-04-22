package de.dkfz.tbi.otp.dataprocessing

import spock.lang.*

class ProcessingThresholdsSpec extends Specification {


    @Unroll
    void "test isAboveLaneThreshold with #laneCount lanes and #threshold threshold should return #result"() {
        setup:
        ProcessingThresholds processingThresholds = new ProcessingThresholds(numberOfLanes: threshold)
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(numberOfMergedLanes: laneCount)

        expect:
        result == processingThresholds.isAboveLaneThreshold(bamFile)

        where:
        laneCount | threshold || result
        2         | null      || true
        2         | 1         || true
        2         | 2         || true
        2         | 3         || false
    }

    void "test isAboveLaneThreshold with bam file is null should throw exception"() {
        setup:
        ProcessingThresholds processingThresholds = new ProcessingThresholds(numberOfLanes: 2)

        when:
        processingThresholds.isAboveLaneThreshold(null)

        then:
        AssertionError e = thrown()
        e.message ==~ '.*bam file may not be null.*'
    }

    void "test isAboveLaneThreshold with bam file with no lane count set should throw exception"() {
        setup:
        ProcessingThresholds processingThresholds = new ProcessingThresholds(numberOfLanes: 2)
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(numberOfMergedLanes: null)

        when:
        processingThresholds.isAboveLaneThreshold(bamFile)

        then:
        AssertionError e = thrown()
        e.message ==~ '.*property numberOfMergedLanes of the bam has to be set.*'
    }

    @Unroll
    void "test isAboveCoverageThreshold with #coverage coverage and #threshold threshold should return #result"() {
        setup:
        ProcessingThresholds processingThresholds = new ProcessingThresholds(coverage: threshold)
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(coverage: coverage)

        expect:
        result == processingThresholds.isAboveCoverageThreshold(bamFile)

        where:
        coverage | threshold || result
        2        | null      || true
        2        | 1         || true
        2        | 2         || true
        2        | 3         || false
    }

    void "test isAboveCoverageThreshold with bam file is null should throw exception"() {
        setup:
        ProcessingThresholds processingThresholds = new ProcessingThresholds(coverage: 2)

        when:
        processingThresholds.isAboveCoverageThreshold(null)

        then:
        AssertionError e = thrown()
        e.message ==~ '.*bam file may not be null.*'
    }

    void "test isAboveCoverageThreshold with bam file with no lane count set should throw exception"() {
        setup:
        ProcessingThresholds processingThresholds = new ProcessingThresholds(coverage: 2)
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(coverage: null)

        when:
        processingThresholds.isAboveCoverageThreshold(bamFile)

        then:
        AssertionError e = thrown()
        e.message ==~ '.*property coverage of the bam has to be set.*'
    }

}
