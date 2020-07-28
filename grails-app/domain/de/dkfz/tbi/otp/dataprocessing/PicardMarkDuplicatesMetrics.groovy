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

import de.dkfz.tbi.otp.utils.Entity

/**
 * represents the MarkDuplicatesMetrics file of a BamFile and
 * stores all the information, which were generated while removing duplicates by Picard
 *
 * Suppress PropertyName: Unsure if the special naming has any kind of relevance. I am
 * no going to change it, too risky.
 */
@SuppressWarnings("PropertyName")
class PicardMarkDuplicatesMetrics implements Entity {

    String metricsClass
    String library
    long unpaired_reads_examined
    long read_pairs_examined
    long unmapped_reads
    long unpaired_read_duplicates
    long read_pair_duplicates
    long read_pair_optical_duplicates
    /**
     * this is only the label, which is used in the MetricsFile itself
     * the value represents the fraction of duplications
     */
    double percent_duplication
    long estimated_library_size

    AbstractBamFile abstractBamFile

    static belongsTo = [
        abstractBamFile: AbstractBamFile,
    ]

    static constraints = {
        metricsClass(blank: false)
        library(blank: false)
    }

    static mapping = {
        abstractBamFile index: "picard_mark_duplicates_metrics_abstract_bam_file_idx"
    }
}
