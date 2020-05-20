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

import groovy.transform.ToString

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/**
 * For some processes, e.g. SNV pipeline, it is only reasonable to be started when a defined threshold, e.g. coverage, is reached.
 * These thresholds are stored in this domain.
 * It is project depending if the number of lanes or the coverage threshold is known. Therefore both possibilities are included in this domain.
 * At least one of the two properties have to be filled in.
 */
@ToString
class ProcessingThresholds implements Entity {

    Project project

    SeqType seqType

    SampleType sampleType

    /**
     * This property contains the coverage which has to be reached to start a process.
     */
    Double coverage

    /**
     * This property contains the number of lanes which have to be reached to start a process.
     */
    Integer numberOfLanes

    static constraints = {
        coverage nullable: true, validator: { val, obj ->
            return (val != null && val > 0) || (val == null && obj.numberOfLanes != null)
        }
        numberOfLanes nullable: true, validator: { val, obj ->
            return (val != null && val > 0) || (val == null && obj.coverage != null)
        }
    }

    static mapping = {
        project  index: "processing_thresholds_project_seq_type_sample_type_idx"
        seqType  index: "processing_thresholds_project_seq_type_sample_type_idx"
        sampleType  index: "processing_thresholds_project_seq_type_sample_type_idx"
    }

    boolean isAboveLaneThreshold(AbstractMergedBamFile bamFile) {
        assert bamFile : 'bam file may not be null'
        assert bamFile.numberOfMergedLanes : 'property numberOfMergedLanes of the bam has to be set'
        return numberOfLanes == null || numberOfLanes <= bamFile.numberOfMergedLanes
    }

    boolean isAboveCoverageThreshold(AbstractMergedBamFile bamFile) {
        assert bamFile : 'bam file may not be null'
        assert bamFile.coverage : 'property coverage of the bam has to be set'
        return coverage == null || coverage <= bamFile.coverage
    }
}
