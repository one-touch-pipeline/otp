/*
 * Copyright 2011-2025 The OTP authors
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

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

class ReferenceGenomeProjectSeqTypeService {

    @CompileDynamic
    ReferenceGenomeProjectSeqType findReferenceGenomeProjectSeqTypeNoSampleType(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType))
    }

    @CompileDynamic
    List<ReferenceGenomeProjectSeqType> findReferenceGenomeProjectSeqTypesWithSampleType(Project project, SeqType seqType) {
        return ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNotNullAndDeprecatedDateIsNull(project, seqType)
    }

    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(SeqTrack seqTrack) {
        assert seqTrack
        return getConfiguredReferenceGenomeProjectSeqType(seqTrack.project, seqTrack.seqType, seqTrack.sampleType)
    }

    @CompileDynamic
    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
        )
    }

    @SuppressWarnings("ThrowRuntimeException")
    // ignored: will be removed with the old workflow system
    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(Project project, SeqType seqType, SampleType sampleType) {
        assert project
        assert seqType
        assert sampleType
        if (sampleType) {
            return getConfiguredReferenceGenomeProjectSeqTypeUsingSampleType(project, seqType, sampleType)
        }
        return getConfiguredReferenceGenomeProjectSeqTypeWithoutSampleType(project, seqType)
    }

    @SuppressWarnings("ThrowRuntimeException")
    // ignored: will be removed with the old workflow system
    @CompileDynamic
    static private ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqTypeWithoutSampleType(
            Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
        )
    }

    @SuppressWarnings("ThrowRuntimeException")
    // ignored: will be removed with the old workflow system
    @CompileDynamic
    static private ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqTypeUsingSampleType(
            Project project, SeqType seqType, SampleType sampleType) {
        return CollectionUtils.atMostOneElement(
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeAndDeprecatedDateIsNull(project, seqType, sampleType)
        )
    }
}
