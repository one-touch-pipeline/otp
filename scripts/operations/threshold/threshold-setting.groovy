package operations.threshold
/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * This script allows to create/update category and threshold for all sampleTypes of a project.
 *
 * The sampleTypes need to exist already in OTP, but must not be connected to the project yet.```
 */

//-----------------------------------------------
//input

String projectName = ""

String sampleTypesDiseaseInput = """
#sampletype1
#sampletype2

"""

String sampleTypesControlInput = """
#sampletype2
#sampletype3

"""

int laneCountWes = 1
int laneCountWgs = 1

//------------------------------------------------
//working

Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName), "Could not find project $projectName")

List<SampleType> sampleTypesDisease = sampleTypesDiseaseInput.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collect {
    SampleType sampleType = SampleTypeService.findSampleTypeByName(it)
    assert sampleType : "Sample type $it not found"
    return sampleType
}
List<SampleType> sampleTypesControl = sampleTypesControlInput.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collect {
    SampleType sampleType = SampleTypeService.findSampleTypeByName(it)
    assert sampleType : "Sample type $it not found"
    return sampleType
}
List<SampleType> allSampleTypes = [
        sampleTypesDisease,
        sampleTypesControl,
].flatten()

SampleTypePerProjectService sampleTypePerProjectService = ctx.sampleTypePerProjectService
ProcessingThresholdsService processingThresholdsService = ctx.processingThresholdsService
SamplePairDeciderService samplePairDeciderService = ctx.samplePairDeciderService

SeqType wes = SeqTypeService.exomePairedSeqType
SeqType wgs = SeqTypeService.wholeGenomePairedSeqType

SampleType.withNewTransaction {
    sampleTypesDisease.each {
        sampleTypePerProjectService.createOrUpdate(project, it, SampleTypePerProject.Category.DISEASE)
    }
    println "create disease: ${sampleTypesDisease*.name.join(', ')}"

    sampleTypesControl.each {
        sampleTypePerProjectService.createOrUpdate(project, it, SampleTypePerProject.Category.CONTROL)
    }
    println "create control: ${sampleTypesControl*.name.join(', ')}"

    allSampleTypes.each { SampleType sampleType ->
        processingThresholdsService.createUpdateOrDelete(project, sampleType, wes, laneCountWes, null)
        processingThresholdsService.createUpdateOrDelete(project, sampleType, wgs, laneCountWgs, null)
    }
    println "create/update thresholds "
    println "    - for WGS with: ${laneCountWgs} lanes and no coverage"
    println "    - for WES with: ${laneCountWes} lanes and no coverage"
    println "    - the following sample types:  ${allSampleTypes*.name.join(', ')}"

    samplePairDeciderService.findOrCreateSamplePairsForProject(project)
    println "create sample pairs "

    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}
''
