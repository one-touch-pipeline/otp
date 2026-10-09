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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.hibernate.annotation.ManagedEntity

/**
 * Unaligned or aligned single lane CRAM file.
 * <p>
 * One instance represents the reads of one lane, not necessarily a single mate: since a CRAM file is able to contain
 * both mates of a read pair, a CRAM converted from a read pair is represented by one instance with
 * {@link #mateNumber} being {@code null}. {@link #mateNumber} is only given for CRAM files converted from a single
 * FASTQ file. The converted FASTQ files are referenced by {@link #sourceFastqFiles}.
 */
@ManagedEntity
class SequenceCramFile extends RawSequenceFile {
    String cramMd5sum
    ReferenceGenome referenceGenome

    /**
     * The FASTQ files this CRAM file was converted from, one per mate, or empty if it was not converted by OTP,
     * for example if it was imported directly.
     * <p>
     * Their {@link FastqFile#fastqMd5sum} are the source checksums of this file.
     */
    Set<FastqFile> sourceFastqFiles

    static hasMany = [
            sourceFastqFiles: FastqFile,
    ]

    static constraints = {
        // a CRAM converted from a read pair has no fastqMd5sum, so its own checksum is the only one available
        cramMd5sum nullable: true, matches: /^[0-9a-f]{32}$/, validator: { val, obj -> val != null || obj.fastqMd5sum != null }
        referenceGenome nullable: true
        sourceFastqFiles validator: { Set<FastqFile> val, SequenceCramFile obj ->
            if (!val) {
                return true
            }
            List<Integer> mateNumbers = val*.mateNumber
            if (mateNumbers.unique(false).size() != mateNumbers.size()) {
                return "duplicateMate"
            }
            return obj.mateNumber != null && mateNumbers != [obj.mateNumber] ? "mateNumberMismatch" : true
        }
    }

    @Override
    @SuppressWarnings('GetterMethodCouldBeProperty')
    String getDataFormat() {
        return 'cram'
    }

    @Override
    boolean isMateNumberRequired() {
        return false
    }

    @Override
    boolean isFastqMd5sumRequired() {
        return false
    }
}
