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

package operations.showData

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile

/**
 *  This script provides details of all the External BAM Files related to a Project
 *  takes a project name as input
 *  produces a list of BAM Files with details below as output
 *  - BAM FileName
 *  - Individual
 *  - SampleType
 *  - SeqType
 *  - Source Path of BAM File
 */

// ----------------------------------------------------
// input area
String projectName = ""

// ----------------------------------------
// work area
assert projectName: 'Please input project name'

List<String> table = []

List<ExternallyProcessedBamFile> bams = ExternallyProcessedBamFile.createCriteria().listDistinct {
    workPackage {
        sample {
            individual {
                project {
                    eq("name", projectName)
                }
            }
        }
    }
    order("fileName")
}

if (bams.size()) {
    println "Detailed List of BAM Files for Project : ${projectName} -> Found ${bams.size()} entries"

    table << [
            "BAM FileName",
            "Individual",
            "SampleType",
            "SeqTypeName",
            "LibraryLayout",
            "SingleCell",
            "SourcePath",
    ].join('\t')

    bams.each { ExternallyProcessedBamFile b ->
        table << [
                b.fileName,
                b.workPackage.individual.toString(),
                b.workPackage.sampleType.toString(),
                b.workPackage.seqType.name,
                b.workPackage.seqType.libraryLayout.toString(),
                b.workPackage.seqType.singleCell.toString(),
                b.importedFrom,
        ].join('\t')
    }
    println table.join('\n')
} else {
    println "No BAM Files found for Project : ${projectName}"
}

''
