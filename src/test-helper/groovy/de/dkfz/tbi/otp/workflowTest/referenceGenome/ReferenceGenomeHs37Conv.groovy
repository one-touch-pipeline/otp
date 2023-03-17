/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflowTest.referenceGenome

import de.dkfz.tbi.otp.dataprocessing.NotSupportedException

trait ReferenceGenomeHs37Conv extends UsingReferenceGenome {

    final String referenceGenomeFileNamePrefix = 'hs37d5_PhiX_Lambda.conv'

    final String referenceGenomeSpecificPath = 'bwa06_methylCtools_hs37d5_PhiX_Lambda'

    final String chromosomeLengthFilePath = null

    final String chromosomeStatFileName = 'hs37d5_PhiX_Lambda.fa.chrLenOnlyACGT.tab'

    final String referenceGenomeCytosinePositionsIndex = 'hs37d5_PhiX_Lambda.pos.gz'

    @Override
    String getFingerPrintingFileName() {
        throw new NotSupportedException("No fingerprinting file name exists")
    }
}