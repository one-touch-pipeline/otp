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

import groovy.transform.ToString

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/**
 * To receive more structure in the sample types it was decided to ask for the samples types which are expected to occur within a project.
 * These sample types are then stored in this domain per project.
 * Furthermore it is relevant to know if a sample type represents a DISEASE or a CONTROL.
 * This information will be requested via the SNV-GUI.
 */
@ToString(excludes=['dateCreated', 'lastUpdated'], includePackage = false)
class SampleTypePerProject implements Entity {

    Project project

    SampleType sampleType

    /**
     * Holds the information if the specified sampleType is a DISEASE or a CONTROL in this project.
     */
    SampleType.Category category


    static constraints = {
        sampleType unique: 'project'
    }

    static mapping = {
        project index: "sample_type_per_project__project__sample_type__category_idx"
        sampleType index: "sample_type_per_project__project__sample_type__category_idx,sample_type_per_project__sample_type_idx"
        category index: "sample_type_per_project__project__sample_type__category_idx,sample_type_per_project__category_idx"
    }
}
