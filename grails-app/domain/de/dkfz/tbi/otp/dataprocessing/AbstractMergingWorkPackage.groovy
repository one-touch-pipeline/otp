/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
abstract class AbstractMergingWorkPackage implements Entity {

    Sample sample
    SeqType seqType
    ReferenceGenome referenceGenome
    Pipeline pipeline
    AntibodyTarget antibodyTarget

    LibraryPreparationKit libraryPreparationKit

    /**
     * The BAM file which moving to the final destination has been initiated for most recently.
     *
     * The referenced BAM file might be withdrawn.
     * If you want to use the referenced BAM file as input for further processing, use
     * {@link #getProcessableBamFileInProjectFolder()}).
     */
    /**
     * Due some strange behavior of GORM (?), this has to be set on null explicitly where objects of
     * {@link de.dkfz.tbi.otp.dataprocessing.AbstractBamFile} or its subclasses are built or created.
     */
    AbstractBamFile bamFileInProjectFolder

    static belongsTo = [
            sample: Sample,
    ]

    static constraints = {
        bamFileInProjectFolder nullable: true, validator: { val, obj ->
            if (val) {
                val.workPackage == obj &&
                        [AbstractBamFile.FileOperationStatus.INPROGRESS, AbstractBamFile.FileOperationStatus.PROCESSED].contains(
                                val.fileOperationStatus)
            } else {
                return true
            }
        }
        antibodyTarget nullable: true, validator: { AntibodyTarget val, AbstractMergingWorkPackage obj ->
            if (obj.seqType) {
                if (obj.seqType.hasAntibodyTarget) {
                    if (val == null) {
                        return ["required", obj.seqType]
                    }
                } else {
                    if (val != null) {
                        return ["not.allowed", obj.seqType]
                    }
                }
            } else {
                return true
            }
        }
        libraryPreparationKit nullable: true
    }

    /**
     * Returns the BAM file which is currently in the project folder, or <code>null</code> if there is no BAM file or it
     * is withdrawn or it is unknown which one currently is there.
     *
     * If you use the returned BAM file as input for further processing, ensure that the file on the file system is
     * consistent with the database object by comparing the file size on the file system to
     * {@link AbstractBamFile#fileSize}. Perform this check a second time <em>after</em> reading from the file
     * to ensure that the file has not been overwritten between the first check and starting to read the file.
     */
    AbstractBamFile getProcessableBamFileInProjectFolder() {
        if (bamFileInProjectFolder && !bamFileInProjectFolder.withdrawn &&
                bamFileInProjectFolder.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED) {
            return bamFileInProjectFolder
        }
        return null
    }

    abstract AbstractBamFile getBamFileThatIsReadyForFurtherAnalysis()

    Project getProject() {
        return sample.project
    }

    Individual getIndividual() {
        return sample.individual
    }

    SampleType getSampleType() {
        return sample.sampleType
    }

    @SuppressWarnings("GrailsDuplicateMapping")
    // this is needed due to grails not supporting indices on multiple columns
    static Closure mapping = {
        'class' index: "abstract_merging_work_package_class_idx"
        seqType index: "abstract_merging_work_package_seq_type_idx"
        seqType index: 'abstract_merging_work_package_sample_seq_type_idx'
        referenceGenome index: "abstract_merging_work_package_reference_genome_idx"
        bamFileInProjectFolder index: "abstract_merging_work_package_bam_file_in_project_folder_idx"
        antibodyTarget index: 'abstract_merging_work_package_antibody_target_idx'
        sample index: 'abstract_merging_work_package_sample_seq_type_idx'
        pipeline index: 'abstract_merging_work_package_pipeline_idx'
        libraryPreparationKit index: 'abstract_merging_work_package_library_preparation_kit_idx'
    }
}
