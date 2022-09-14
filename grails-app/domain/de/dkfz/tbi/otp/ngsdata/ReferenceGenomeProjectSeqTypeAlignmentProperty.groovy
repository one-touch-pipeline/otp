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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
class ReferenceGenomeProjectSeqTypeAlignmentProperty implements Entity {

    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    String name

    // suppressing because changing this would involve refactoring the code as well as the database columns
    @SuppressWarnings("GrailsDomainReservedSqlKeywordName")
    String value

    static belongsTo = [
            referenceGenomeProjectSeqType: ReferenceGenomeProjectSeqType,
    ]

    static constraints = {
        name blank: false, unique: ['referenceGenomeProjectSeqType']
        value blank: false, maxSize: 500
    }

    static Closure mapping = {
        referenceGenomeProjectSeqType index: "reference_genome_project_seq_type_alignment_property_reference_genome_project_seq_type_idx"
    }
}
