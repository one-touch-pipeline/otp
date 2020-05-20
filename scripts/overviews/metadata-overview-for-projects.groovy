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
 * Create overview of SampleIdentifier with the corresponding Project, Individual, SeqType, SampleType and ilse number.
 */


//-----------------------------
//input

/**
 * input of project names. Empty lines and lines starting with # are ignored.
 */
String projectInputArea = """
#project1
#project2

"""


//-----------------------------
//work


List<Project> projects = projectInputArea.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collect {
    CollectionUtils.exactlyOneElement(Project.findAllByName(it))
}

MetaDataKey key = MetaDataKey.findByName(MetaDataColumn.SAMPLE_ID.toString())

println MetaDataEntry.createCriteria().list {
    eq('key', key)
    dataFile {
        seqTrack {
            sample {
                individual {
                    'in'('project', projects)
                }
            }
        }
    }
}.collect { MetaDataEntry entry ->
    DataFile dataFile = entry.dataFile
    [
            dataFile.project,
            dataFile.individual,
            dataFile.sampleType,
            dataFile.seqType.displayNameWithLibraryLayout,
            dataFile.seqTrack.ilseId,
            entry.value
    ].join(',')
}.sort().join('\n')
