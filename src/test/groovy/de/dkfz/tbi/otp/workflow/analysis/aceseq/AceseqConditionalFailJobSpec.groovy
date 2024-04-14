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
package de.dkfz.tbi.otp.workflow.analysis.aceseq

import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaLinkFileService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisConditionalFailJobSpec

import java.nio.file.Path
import java.nio.file.Paths

class AceseqConditionalFailJobSpec extends AbstractAnalysisConditionalFailJobSpec {

    SophiaInstance sophiaInstance

    static final Path CHROMOSOME_LEN_PATH = Paths.get("chrTotalLength.tsv")
    static final Path GC_CONTENT_PATH = Paths.get("gcContentFile.tsv")
    static final Path ACESEQ_INPUT_PATH = Paths.get("aceseqInputFile")

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflow: findOrCreateWorkflow(AceseqWorkflow.WORKFLOW, [beanName: AceseqWorkflow.simpleName.uncapitalize()]),
                ]),
        ])
        instance = new AceseqInstance()
        sophiaInstance = new SophiaInstance()

        job = new AceseqConditionalFailJob()
        job.sophiaLinkFileService = Mock(SophiaLinkFileService)
    }

    @Unroll
    void "doFurtherCheck with #name, then errorMessages contains errors"() {
        given:
        setupWithSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName)
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            1 * chromosomeLengthPath(bamFile1.mergingWorkPackage) >> chromLenPath
            1 * gcContentPath(bamFile1.mergingWorkPackage) >> gcContPath
        }
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefact(workflowStep, "SOPHIA") >> sophiaInstance
        }
        1 * job.sophiaLinkFileService.getFinalAceseqInputFile(_) >> aceseqPath

        job.fileService = Mock(FileService)
        (chromLenPath ? 1 : 0) * job.fileService.isFileReadableAndNotEmpty(CHROMOSOME_LEN_PATH) >> chromLen
        (gcContPath ? 1 : 0) * job.fileService.isFileReadableAndNotEmpty(GC_CONTENT_PATH) >> gcCont
        (aceseqPath ? 1 : 0) * job.fileService.isFileReadableAndNotEmpty(ACESEQ_INPUT_PATH) >> inFile

        when:
        List<String> expectedErrorMessages = job.doFurtherCheck(workflowStep, bamFile1, bamFile2)

        then:
        TestCase.assertContainSame(expectedErrorMessages, errmsgs)

        where:
        name                                    | chromLen | chromLenPath        | gcCont | gcContPath      | inFile | aceseqPath        || errmsgs
        "chrom file missing"                    | true     | null                | true   | GC_CONTENT_PATH | true   | ACESEQ_INPUT_PATH || ["Chromosome length file can not be found."]
        "chrom file not readable"               | false    | CHROMOSOME_LEN_PATH | true   | GC_CONTENT_PATH | true   | ACESEQ_INPUT_PATH || ["Chromosome length file ${CHROMOSOME_LEN_PATH} is either not readable or empty."]
        "gcCon file missing"                    | true     | CHROMOSOME_LEN_PATH | false  | null            | true   | ACESEQ_INPUT_PATH || ["gc content file can not be found."]
        "gcCon file not readable"               | true     | CHROMOSOME_LEN_PATH | false  | GC_CONTENT_PATH | true   | ACESEQ_INPUT_PATH || ["gc content file ${GC_CONTENT_PATH} is either not readable or empty."]
        "aceseq file missing"                   | true     | CHROMOSOME_LEN_PATH | true   | GC_CONTENT_PATH | true   | null              || ["ACEseq input file can not be found."]
        "aceseq file not readable"              | true     | CHROMOSOME_LEN_PATH | true   | GC_CONTENT_PATH | false  | ACESEQ_INPUT_PATH || ["ACEseq input file ${ACESEQ_INPUT_PATH} is either not readable or empty."]
        "both chrom & gcCon files missing"      | true     | null                | true   | null            | true   | ACESEQ_INPUT_PATH || ["Chromosome length file can not be found.", "gc content file can not be found."]
        "both chrom & gcCon files not readable" | false    | CHROMOSOME_LEN_PATH | false  | GC_CONTENT_PATH | true   | ACESEQ_INPUT_PATH || ["Chromosome length file ${CHROMOSOME_LEN_PATH} is either not readable or empty.", "gc content file ${GC_CONTENT_PATH} is either not readable or empty."]
        "all files missing"                     | true     | null                | true   | null            | true   | null              || ["Chromosome length file can not be found.", "gc content file can not be found.", "ACEseq input file can not be found."]
        "all files not readable"                | false    | CHROMOSOME_LEN_PATH | false  | GC_CONTENT_PATH | false  | ACESEQ_INPUT_PATH || ["Chromosome length file ${CHROMOSOME_LEN_PATH} is either not readable or empty.", "gc content file ${GC_CONTENT_PATH} is either not readable or empty.", "ACEseq input file ${ACESEQ_INPUT_PATH} is either not readable or empty."]
        "some files missing, some not readable" | false    | null                | false  | GC_CONTENT_PATH | false  | ACESEQ_INPUT_PATH || ["Chromosome length file can not be found.", "gc content file ${GC_CONTENT_PATH} is either not readable or empty.", "ACEseq input file ${ACESEQ_INPUT_PATH} is either not readable or empty."]
    }

    @Override
    void setupMocking() {
        super.setupMocking()
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, "SOPHIA") >> sophiaInstance
        1 * job.sophiaLinkFileService.getFinalAceseqInputFile(_) >> ACESEQ_INPUT_PATH

        1 * job.fileService.isFileReadableAndNotEmpty(CHROMOSOME_LEN_PATH) >> true
        1 * job.fileService.isFileReadableAndNotEmpty(GC_CONTENT_PATH) >> true
        1 * job.fileService.isFileReadableAndNotEmpty(ACESEQ_INPUT_PATH) >> true
        1 * job.referenceGenomeService.chromosomeLengthPath(bamFile1.mergingWorkPackage) >> CHROMOSOME_LEN_PATH
        1 * job.referenceGenomeService.gcContentPath(bamFile1.mergingWorkPackage) >> GC_CONTENT_PATH
    }
}
