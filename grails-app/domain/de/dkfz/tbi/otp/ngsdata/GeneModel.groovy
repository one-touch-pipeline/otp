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

import de.dkfz.tbi.otp.utils.Entity

class GeneModel implements Entity {

    static final String GENE_MODELS = "GENE_MODELS"
    static final String GENE_MODELS_EXCLUDE = "GENE_MODELS_EXCLUDE"
    static final String GENE_MODELS_DEXSEQ = "GENE_MODELS_DEXSEQ"
    static final String GENE_MODELS_GC = "GENE_MODELS_GC"


    ReferenceGenome referenceGenome

    // suppressing because changing this would involve refactoring the code as well as the database columns
    @SuppressWarnings("GrailsDomainReservedSqlKeywordName")
    String path
    String fileName
    String excludeFileName
    String dexSeqFileName
    String gcFileName

    static constraints = {
        path unique: 'referenceGenome', blank: false, shared: "relativePath"
        fileName blank: false, shared: "pathComponent"
        excludeFileName blank: false, shared: "pathComponent"
        dexSeqFileName nullable: true, shared: "pathComponent"
        gcFileName nullable: true, shared: "pathComponent"
    }

    @Override
    String toString() {
        fileName
    }
}
