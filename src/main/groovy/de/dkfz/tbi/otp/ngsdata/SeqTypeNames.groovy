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
package de.dkfz.tbi.otp.ngsdata

import groovy.transform.CompileDynamic

/**
 * Holds possible seqType names for using in the code
 */
@CompileDynamic
@SuppressWarnings("FieldName")
enum SeqTypeNames {
    _10X_SCRNA("10x_scRNA"),
    WHOLE_GENOME,
    WHOLE_GENOME_BISULFITE,
    RNA,
    MI_RNA,
    EXOME("EXON"),
    MEDIP,
    SNC_RNA("sncRNA"),
    CHIP_SEQ("ChIP Seq"),
    WHOLE_GENOME_BISULFITE_TAGMENTATION

    /**
     * the name of the seq type
     */
    final String seqTypeName

    private SeqTypeNames(String seqTypeName = null) {
        this.seqTypeName = seqTypeName ?: name()
    }

    boolean isWgbs() {
        return SeqType.WGBS_SEQ_TYPE_NAMES.contains(this)
    }

    boolean isWgs() {
        return SeqType.WGS_SEQ_TYPE_NAMES.contains(this)
    }

    static SeqTypeNames fromSeqTypeName(String seqTypeName) {
        return values().find { it.seqTypeName == seqTypeName }
    }
}
