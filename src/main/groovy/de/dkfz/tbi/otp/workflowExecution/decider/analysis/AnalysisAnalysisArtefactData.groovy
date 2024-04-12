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
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.decider.ArtefactData

@ToString(includePackage = false, includeNames = true, includeSuper = true)
@EqualsAndHashCode(callSuper = true, cache = true)
class AnalysisAnalysisArtefactData<A extends BamFilePairAnalysis> extends ArtefactData<A> {
    final SamplePair samplePair
    final Individual individual
    final SampleType sampleType1
    final SampleType sampleType2
    final Sample sample1
    final Sample sample2
    final AbstractBamFile abstractBamFile1
    final AbstractBamFile abstractBamFile2

    @SuppressWarnings("ParameterCount")
    AnalysisAnalysisArtefactData(WorkflowArtefact workflowArtefact, A artefact, Project project, SeqType seqType,
                                 SamplePair samplePair, Individual individual, SampleType sampleType1, SampleType sampleType2, Sample sample1, Sample sample2,
                                 AbstractBamFile abstractBamFile1, AbstractBamFile abstractBamFile2) {
        super(workflowArtefact, artefact, project, seqType)
        this.samplePair = samplePair
        this.individual = individual
        this.sampleType1 = sampleType1
        this.sampleType2 = sampleType2
        this.sample1 = sample1
        this.sample2 = sample2
        this.abstractBamFile1 = abstractBamFile1
        this.abstractBamFile2 = abstractBamFile2
    }
}
