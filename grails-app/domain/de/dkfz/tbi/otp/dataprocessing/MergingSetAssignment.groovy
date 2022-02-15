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

import grails.gorm.hibernate.annotation.ManagedEntity
import org.hibernate.Hibernate

import de.dkfz.tbi.otp.utils.Entity

/**
 * many to many connection between
 * {@link MergingSet} and {@link ProcessedBamFile}
 */
@Deprecated
@ManagedEntity
class MergingSetAssignment implements Entity {

    MergingSet mergingSet
    AbstractBamFile bamFile

    static belongsTo = [
        mergingSet: MergingSet,
        bamFile: AbstractBamFile,
    ]

    static constraints = {
        bamFile validator: { AbstractBamFile bamFile, MergingSetAssignment msa ->
            if (!msa.mergingSet?.mergingWorkPackage?.satisfiesCriteria(bamFile)) {
                return false
            }
            /** Before you remove this constraint, make sure that all existing code
                correctly handles other types of {@link AbstractBamFile}s in a merging set. */
            Class bamFileClass = Hibernate.getClass(bamFile)
            return ProcessedBamFile.isAssignableFrom(bamFileClass) || ProcessedMergedBamFile.isAssignableFrom(bamFileClass)
        }
    }

    static mapping = {
        mergingSet index: "merging_set_assignment_merging_set_idx"
        bamFile index: "merging_set_assignment_bam_file_idx"
    }
}
