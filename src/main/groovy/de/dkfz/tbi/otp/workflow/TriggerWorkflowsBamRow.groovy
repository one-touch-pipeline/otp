/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow

import groovy.transform.Immutable

/**
 * Projected scalar data of a single BAM file shown in the BAM table of the trigger workflows page.
 */
@Immutable
class TriggerWorkflowsBamRow {

    Long id

    String project

    String individual

    String sampleType

    /** Full sequencing type label, matching {@link de.dkfz.tbi.otp.ngsdata.SeqType#getDisplayNameWithLibraryLayout()} */
    String seqType

    boolean withdrawn

    /** Library preparation kit of the work package */
    String libPrepKit

    String species

    String mixedInSpecies

    String referenceGenome

    /** Display string of the seq platform group of the work package, empty for external BAM files */
    String seqPlatformGroup

    /** IDs of the eligible analyses referencing this BAM file, distinct and numerically sorted */
    List<Long> analysisIds
}
