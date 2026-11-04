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

/**
 * Projected scalar data of a single BAM file, used to build the BAM and analysis rows of the trigger workflows tables.
 */
class TriggerWorkflowsBamFileMetadata {
    Long id
    boolean withdrawn
    String project
    String individual
    String sampleType
    String seqType
    String libraryPreparationKit
    String referenceGenome
    String species
    List<String> mixedInSpecies = []

    /** Kits of the contained SeqTracks; null if the BAM file has no contained SeqTracks or they were not fetched */
    List<String> seqTrackLibraryPreparationKits

    /**
     * Library preparation kits of the SeqTracks contained in the BAM file.
     * BAM files without contained SeqTracks (external BAM files) use the kit of their work package.
     */
    List<String> getAnalysisLibraryPreparationKits() {
        return seqTrackLibraryPreparationKits == null ? [libraryPreparationKit] : seqTrackLibraryPreparationKits
    }
}
