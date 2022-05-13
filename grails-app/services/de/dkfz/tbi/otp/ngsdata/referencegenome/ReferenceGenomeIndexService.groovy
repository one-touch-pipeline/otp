/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.referencegenome

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.ngsdata.*

@Transactional
class ReferenceGenomeIndexService {
    ReferenceGenomeService referenceGenomeService

    static final String REFERENCE_GENOME_INDEX_PATH_COMPONENT = "indexes"

    File getFile(ReferenceGenomeIndex referenceGenomeIndex) {
        assert referenceGenomeIndex : "referenceGenomeIndex is null"
        new File(getBasePath(referenceGenomeIndex), referenceGenomeIndex.path)
    }

    private File getBasePath(ReferenceGenomeIndex referenceGenomeIndex) {
        new File(new File(referenceGenomeService.referenceGenomeDirectory(referenceGenomeIndex.referenceGenome, false),
                REFERENCE_GENOME_INDEX_PATH_COMPONENT), referenceGenomeIndex.toolName.path)
    }

    ReferenceGenomeIndex findById(long id) {
        return ReferenceGenomeIndex.get(id)
    }

    List<ReferenceGenomeIndex> findAllByReferenceGenome(ReferenceGenome referenceGenome) {
        return ReferenceGenomeIndex.findAllByReferenceGenome(referenceGenome)
    }

    List<ReferenceGenomeIndex> findAllByReferenceGenomeAndToolName(ReferenceGenome refGenome, ToolName toolName) {
        return ReferenceGenomeIndex.findAllByReferenceGenomeAndToolName(refGenome, toolName)
    }
}
