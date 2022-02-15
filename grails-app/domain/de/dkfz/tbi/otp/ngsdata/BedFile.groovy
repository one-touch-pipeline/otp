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
class BedFile implements Entity {

    /**
     * Name of the bed file located in a sub-directory
     * of the corresponding reference genome directory
     */
    String fileName

    /**
     * Sum of lengths of all target regions covered by this bed file
     */
    long targetSize

    /**
     * Sum of length values of all unique target regions.
     * Existing overlapping target regions have been merged.
     */
    long mergedTargetSize

    ReferenceGenome referenceGenome
    LibraryPreparationKit libraryPreparationKit

    /**
     * A BedFile is always associated to a reference genome to know
     * on what coordinate system the regions specified in the BedFile
     * are based on.
     * A kit can have multiple BedFiles which specify the target regions
     * the kit has been designed for. The kit can have multiple BedFiles
     * because of the fact that the specified target regions can be based
     * on different reference genomes, not on the fact that the kit itself
     * has another version. ( The kit version is included in the name)
     *
     * A kit can exist in the database without having a bed file associated to
     * the kit.
     */
    static belongsTo = [
        referenceGenome: ReferenceGenome,
        libraryPreparationKit: LibraryPreparationKit,
    ]

    static constraints = {
        fileName(unique: 'referenceGenome', blank: false, shared: "pathComponent")
        // alternative primary key (referenceGenome, libraryPreparationKit)
        // we will never have 2 bed files in the db for the same
        // referenceGenome and libraryPreparationKit combination
        referenceGenome(unique: 'libraryPreparationKit')
        targetSize(min: 1L)
    }

    @Override
    String toString() {
        return "${fileName} for genome ${referenceGenome} and kit ${libraryPreparationKit}"
    }

    static mapping = {
        referenceGenome index: "bed_file_reference_genome_idx"
        libraryPreparationKit index: "bed_file_library_preparation_kit_idx"
    }
}
