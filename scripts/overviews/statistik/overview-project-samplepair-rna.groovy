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

/**
 * The script show for all projects and analysable seqType the count of individuals having a sample pair and how many of them has also rna.
 *
 * Per default a sample pair based on tumor and control, but manually it is possible to create also tumor tumor or control control pairs.
 *
 * The script do not have any input
 */

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

//---------------------------
//work

List<SeqType> analysableSeqTypes = SeqTypeService.allAnalysableSeqTypes

String header = (['project'] + analysableSeqTypes.collectMany {
    [
            "individual with ${it.displayNameWithLibraryLayout} sample pairs",
            "individual with ${it.displayNameWithLibraryLayout} sample pairs and additional rna",
    ]
}).join(', ')


String output = Project.list().sort {
    it.name
}.collect { Project project ->
    List<String> row = [project]

    analysableSeqTypes.each { SeqType seqTypeToUse ->
        List<String> individualsWithTumorControl = SamplePair.createCriteria().list {
            projections {
                mergingWorkPackage1 {
                    sample {
                        individual {
                            eq('project', project)
                            property('pid')
                        }
                    }
                    eq('seqType', seqTypeToUse)
                }
            }
        }.unique()
        row << individualsWithTumorControl.size()

        Integer individualsWithTumorControlAndRna = individualsWithTumorControl ? SeqTrack.createCriteria().get {
            projections {
                sample {
                    individual {
                        'in'('pid', individualsWithTumorControl)
                        countDistinct('pid')
                    }
                }
                seqType {
                    eq('name', 'RNA')
                }
            }
        } : 0

        row << individualsWithTumorControlAndRna
    }
    row.join(', ')
}.join('\n')

println "${header}\n${output}"
