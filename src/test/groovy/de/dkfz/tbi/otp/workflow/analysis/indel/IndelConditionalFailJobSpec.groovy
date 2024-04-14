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
package de.dkfz.tbi.otp.workflow.analysis.indel

import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisConditionalFailJobSpec

class IndelConditionalFailJobSpec extends AbstractAnalysisConditionalFailJobSpec {

    final static String BEDFILE_FILE_NAME = "bed_file"

    BedFile bedFile

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflow: findOrCreateWorkflow(IndelWorkflow.WORKFLOW, [beanName: IndelWorkflow.simpleName.uncapitalize()]),
                ]),
        ])
        instance = new IndelCallingInstance()
        job = new IndelConditionalFailJob()
    }

    @Override
    void setupWithSeqType(String seqTypeName) {
        super.setupWithSeqType(seqTypeName)

        bedFile = DomainFactory.createBedFile([
                referenceGenome: bamFile1.referenceGenome,
                libraryPreparationKit: bamFile1.mergingWorkPackage.libraryPreparationKit,
                fileName: BEDFILE_FILE_NAME,
        ])
    }

    @Unroll
    void "doFurtherCheck with #name, then errorMessages contains errors"() {
        given:
        setupWithSeqType(SeqTypeNames.EXOME.seqTypeName)
        job.bedFileService = Mock(BedFileService) {
            1 * filePath(bedFile) >> { bedfile() }
        }

        when:
        List<String> expectedErrorMessages = job.doFurtherCheck(workflowStep, bamFile1, bamFile2)

        then:
        TestCase.assertContainSame(expectedErrorMessages, errmsgs)

        where:
        name               | bedfile                                                   || errmsgs
        "bed file found"   | { BEDFILE_FILE_NAME }                                     || []
        "bed file missing" | { throw new FileNotReadableException(BEDFILE_FILE_NAME) } || ["Required BED file ${BEDFILE_FILE_NAME} cannot be found or not readable.\ncan not read file: bed_file"]
    }

    @Override
    void setupMocking() {
        super.setupMocking()

        job.bedFileService = Mock(BedFileService) {
            (bamFile1.seqType.name == SeqTypeNames.EXOME.seqTypeName ? 1 : 0) * filePath(bedFile) >> true
        }
    }
}
