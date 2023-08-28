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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * show for one project all samples, those seqTracks belongs to different seqPlatformGroups.
 *
 * Only seqTracks of alignable seqTypes are considered.
 *
 * The check only work, if the mergingCriteria is defined for the project and seqType, since that is needed to find the seqPlatformGroup.
 *
 * The input is the name of the project to check.
 *
 * The output is an table for all sample/seqtype combination with more then one seqPlatformgroup with the following columns:
 * - pid
 * - sample type
 * - seq type (name, sequencongReadType, single cell)
 * - first seqplatform group
 * - second seqplatform group
 * - ... further seq platform groups
 */

// ---------------------------
// input area
String projectName = ''

// ---------------------------
// work area
assert projectName : 'Please select a project'
Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName),
        "Could not find project with name: '${projectName}'")

List<SeqType> seqTypes = SeqTypeService.allAlignableSeqTypes

println "sample with different seqplatformgroups"
println "\n"
println "pid\tsampleType\tseqType\tseqPlatformgroup1\tseqPlatformgroup2\t..."
List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
    sample {
        individual {
            eq('project', project)
        }
    }
    'in'('seqType', seqTypes)
}

println seqTracks.collect {
    [
            [
                    it.individual.pid,
                    it.sampleType.name,
                    it.seqType.displayNameWithLibraryLayout,
            ],
            it.seqPlatformGroup,
    ]
}.unique().groupBy {
    it[0]
}.findAll { key, value ->
    value.size() > 1
}.collect { key, value ->
    (key + value*.get(1)*.toString().sort()).join('\t')
}.sort().join('\n')
