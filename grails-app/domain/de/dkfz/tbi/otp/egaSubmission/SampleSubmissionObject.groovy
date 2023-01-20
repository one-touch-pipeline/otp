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
package de.dkfz.tbi.otp.egaSubmission

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
class SampleSubmissionObject implements Entity, SubmissionObject {

    Sample sample
    SeqType seqType
    boolean useBamFile = false
    boolean useFastqFile = false

    static belongsTo = [
            sample: Sample,
    ]

    static constraints = {
        egaAliasName nullable: true, unique: true
    }

    static mapping = {
        sample index: 'sample_submission_sample_idx'
        seqType index: 'sample_submission_seq_type_idx'
        egaAliasName index: 'sample_submission_ega_alias_name_idx'
    }

    Project getProject() {
        sample.project
    }

    @Override
    String toString() {
        "${sample.individual.displayName} ${seqType.displayNameWithLibraryLayout} ${sample.sampleType.displayName}"
    }
}
