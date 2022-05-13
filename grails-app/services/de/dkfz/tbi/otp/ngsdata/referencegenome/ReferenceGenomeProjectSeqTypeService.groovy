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

import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

class ReferenceGenomeProjectSeqTypeService {

    ReferenceGenomeProjectSeqType findReferenceGenomeProjectSeqTypeNoSampleType(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType))
    }

    List<ReferenceGenomeProjectSeqType> findReferenceGenomeProjectSeqTypesWithSampleType(Project project, SeqType seqType) {
        return ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNotNullAndDeprecatedDateIsNull(project, seqType)
    }

    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(SeqTrack seqTrack) {
        assert seqTrack
        getConfiguredReferenceGenomeProjectSeqType(seqTrack.project, seqTrack.seqType, seqTrack.sampleType)
    }

    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
        )
    }

    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(Project project, SeqType seqType, SampleType sampleType) {
        assert project
        assert seqType
        assert sampleType
        switch (sampleType.specificReferenceGenome) {
            case SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT:
                return getConfiguredReferenceGenomeProjectSeqTypeUsingProjectDefault(project, seqType, sampleType)
            case SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC:
                return getConfiguredReferenceGenomeProjectSeqTypeUsingSampleTypeSpecific(project, seqType, sampleType)
            case SampleType.SpecificReferenceGenome.UNKNOWN:
                throw new RuntimeException("For sample type '${sampleType} the way to fetch the reference genome is not defined.")
            default:
                throw new RuntimeException("The value ${sampleType.specificReferenceGenome} for specific reference genome is not known")
        }
    }

    static private ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqTypeUsingProjectDefault(
            Project project, SeqType seqType, SampleType sampleType) {
        assert SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT == sampleType.specificReferenceGenome
        try {
            return CollectionUtils.atMostOneElement(
                    ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
            )
        } catch (AssertionError e) {
            throw new RuntimeException("Could not find a reference genome for project '${project}' and '${seqType}'", e)
        }
    }

    static private ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqTypeUsingSampleTypeSpecific(
            Project project, SeqType seqType, SampleType sampleType) {
        assert SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC == sampleType.specificReferenceGenome
        try {
            return CollectionUtils.atMostOneElement(
                    ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeAndDeprecatedDateIsNull(project, seqType, sampleType)
            )
        } catch (AssertionError e) {
            throw new RuntimeException("Could not find a reference genome for project '${project}' and '${seqType}' and '${sampleType}'", e)
        }
    }
}
