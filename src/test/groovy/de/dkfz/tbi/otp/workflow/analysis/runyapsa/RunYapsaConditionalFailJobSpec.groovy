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
package de.dkfz.tbi.otp.workflow.analysis.runyapsa

import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvLinkFileService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisConditionalFailJobSpec

import java.nio.file.Path
import java.nio.file.Paths

class RunYapsaConditionalFailJobSpec extends AbstractAnalysisConditionalFailJobSpec {

    final static Path RESULT_LINK_DIR = Paths.get("result/linkDir")
    final static String BEDFILE_FILE_NAME = "bed_file"

    BedFile bedFile
    RoddySnvCallingInstance snvInstance

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflow: findOrCreateWorkflow(RunYapsaWorkflow.WORKFLOW, [beanName: RunYapsaWorkflow.simpleName.uncapitalize()]),
                ]),
        ])

        bedFile = DomainFactory.createBedFile([
                fileName: BEDFILE_FILE_NAME,
        ])

        job = new RunYapsaConditionalFailJob()
    }

    @Override
    void setupWithSeqType(String seqTypeName) {
        super.setupWithSeqType(seqTypeName)
        instance = DomainFactory.createRunYapsaInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.IN_PROGRESS,
                samplePair     : DomainFactory.createSamplePair([
                        mergingWorkPackage1: bamFile1.mergingWorkPackage,
                        mergingWorkPackage2: bamFile2.mergingWorkPackage,
                ]),
        ])
        snvInstance = DomainFactory.createRoddySnvCallingInstance([
                samplePair: DomainFactory.createSamplePair([
                        mergingWorkPackage1: bamFile1.mergingWorkPackage,
                        mergingWorkPackage2: bamFile2.mergingWorkPackage,
                ]),
                config: DomainFactory.createRoddyWorkflowConfig(
                        project: bamFile1.project,
                        seqType: bamFile1.seqType,
                        pipeline: DomainFactory.createRoddySnvPipelineLazy()
                ),
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2,
        ])
    }

    @Unroll
    void "doFurtherCheck with #name, then errorMessages contains errors"() {
        given:
        setupWithSeqType(SeqTypeNames.EXOME.seqTypeName)
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefact(workflowStep, "SNV") >> snvInstance
            0 * _
        }
        job.snvLinkFileService = Mock(SnvLinkFileService) {
            1 * getResultRequiredForRunYapsa(_) >> RESULT_LINK_DIR
        }

        job.fileService = Mock(FileService) {
            1 * fileIsReadable(RESULT_LINK_DIR) >> link
        }
        job.bedFileService = Mock(BedFileService) {
            1 * findBedFileByReferenceGenomeAndLibraryPreparationKit(_, _) >> bedFile
            1 * filePath(bedFile) >> { bedfile() }
        }

        when:
        List<String> expectedErrorMessages = job.doFurtherCheck(workflowStep, bamFile1, bamFile2)

        then:
        TestCase.assertContainSame(expectedErrorMessages, errmsgs)

        where:
        name                   | bedfile                                                   | link  || errmsgs
        "snv link missing"     | { Paths.get(BEDFILE_FILE_NAME) }                          | false || ["SNV result file ${RESULT_LINK_DIR} cannot be found in linked view-by-pid folder."]
        "bed file missing"     | { throw new FileNotReadableException(BEDFILE_FILE_NAME) } | true  || ["Required BED file is missing or not readable.\ncan not read file: ${BEDFILE_FILE_NAME}"]
    }

    @Override
    void setupMocking() {
        super.setupMocking()
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, "SNV") >> snvInstance
        job.bedFileService = Mock(BedFileService) {
            _ * findBedFileByReferenceGenomeAndLibraryPreparationKit(_, _) >> bedFile
        }
        job.snvLinkFileService = Mock(SnvLinkFileService) {
            1 * getResultRequiredForRunYapsa(_) >> RESULT_LINK_DIR
        }

        1 * job.fileService.fileIsReadable(RESULT_LINK_DIR) >> true
    }
}
