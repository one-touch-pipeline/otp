/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import spock.lang.Specification
import spock.lang.Unroll

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
