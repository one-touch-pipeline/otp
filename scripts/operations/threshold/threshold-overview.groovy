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

import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * This script shows the categories and thresholds of all configured sampleTypes for a given project.
 *
 * Additionally, it shows missing categories and thresholds for the project.
 */

// -----------------------------------------------
// input

String projectName = ""

// ------------------------------------------------
// working

Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName), "Could not find project $projectName")

SampleTypeService sampleTypeService = ctx.sampleTypeService

SeqType wes = SeqTypeService.exomePairedSeqType
SeqType wgs = SeqTypeService.wholeGenomePairedSeqType

List<SampleType> sampleTypes = sampleTypeService.findUsedSampleTypesForProject(project)
List<SampleTypePerProject> sampleTypePerProjects = SampleTypePerProject.findAllByProject(project)
List<SampleType> sampleTypesWithCategory = sampleTypePerProjects*.sampleType
List<ProcessingThresholds> processingThresholds = ProcessingThresholds.findAllByProject(project)
List<SampleType> sampleTypesWithWesThreshold = processingThresholds.findAll { it.seqType == wes }*.sampleType
List<SampleType> sampleTypesWithWgsThreshold = processingThresholds.findAll { it.seqType == wgs }*.sampleType

println '\n\ncategories: '
println sampleTypePerProjects.collect {
    [
            it.sampleType,
            it.category,
    ].join(' ')
}.sort().join('\n')

println '\n\nprocessingThresholds: '
println processingThresholds.collect {
    [
            it.sampleType,
            it.seqType,
            it.numberOfLanes,
            it.coverage,
    ].join(' ')
}.sort().join('\n')

println '\n\nmissed categories: '
println sampleTypes.findAll {
    !sampleTypesWithCategory.contains(it)
}*.name.sort().join('\n')

println '\n\nmissed wes processingThresholds: '
println sampleTypes.findAll {
    !sampleTypesWithWesThreshold.contains(it)
}*.name.sort().join('\n')

println '\n\nmissed wgs processingThresholds: '
println sampleTypes.findAll {
    !sampleTypesWithWgsThreshold.contains(it)
}*.name.sort().join('\n')

''
