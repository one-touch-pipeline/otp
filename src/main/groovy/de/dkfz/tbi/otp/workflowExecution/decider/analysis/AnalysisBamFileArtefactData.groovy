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
package de.dkfz.tbi.otp.workflowExecution.decider.analysis

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.decider.ArtefactData

@ToString(includePackage = false, includeNames = true, includeSuper = true)
@EqualsAndHashCode(callSuper = true, cache = true)
class AnalysisBamFileArtefactData extends ArtefactData<AbstractBamFile> {
    final Individual individual
    final SampleType sampleType
    final Sample sample
    final AbstractMergingWorkPackage mergingWorkPackage
    final SeqPlatformGroup seqPlatformGroup

    @SuppressWarnings("ParameterCount")
    AnalysisBamFileArtefactData(WorkflowArtefact workflowArtefact, AbstractBamFile artefact, Project project, SeqType seqType,
                                Individual individual, SampleType sampleType, Sample sample, AbstractMergingWorkPackage mergingWorkPackage,
                                SeqPlatformGroup seqPlatformGroup) {
        super(workflowArtefact, artefact, project, seqType)
        this.individual = individual
        this.sampleType = sampleType
        this.sample = sample
        this.mergingWorkPackage = mergingWorkPackage
        this.seqPlatformGroup = seqPlatformGroup
    }
}
