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

class Sample implements Entity {

    static hasMany = [
            seqTracks: SeqTrack
    ]

    static belongsTo = [
        individual : Individual,
        sampleType : SampleType,
    ]
    Individual individual
    SampleType sampleType

    static constraints = {
        individual(nullable: false)
        sampleType(nullable: false, unique: 'individual')
    }

    @Override
    String toString() {
        // useful for scaffolding
        "${individual?.mockPid} ${sampleType?.name}"
    }

    String getDisplayName() {
        return "${individual?.displayName} ${sampleType?.displayName}"
    }

    /**
     * @return List of SampleIdentifier for this Sample.
     */
    List<SampleIdentifier> getSampleIdentifiers() {
        return SampleIdentifier.findAllBySample(this)
    }

    Project getProject() {
        return individual.project
    }

    /**
     * @return The category of this sample's type or <code>null</code> if it is not configured.
     */
    SampleType.Category getSampleTypeCategory() {
        return sampleType.getCategory(project)
    }

    static mapping = {
        sampleType index: "sample_sample_type_idx"
    }
}
