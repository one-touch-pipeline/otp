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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaLinkFileService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisConditionalFailJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class AceseqConditionalFailJob extends AbstractAnalysisConditionalFailJob implements AceseqWorkflowShared {

    @Autowired
    SophiaLinkFileService sophiaLinkFileService

    @Override
    BamFilePairAnalysis getAnalysisInstance(WorkflowStep workflowStep) {
        return getAceseqInstance(workflowStep)
    }

    @Override
    List<String> doFurtherCheck(WorkflowStep workflowStep, AbstractBamFile bamFileDisease, AbstractBamFile bamFileControl) {
        List<String> errorMessages = []

        Path chromosomeLengthFile = referenceGenomeService.chromosomeLengthPath(bamFileDisease.mergingWorkPackage)
        if (!chromosomeLengthFile) {
            errorMessages.push("Chromosome length file can not be found.")
        } else if (!fileService.isFileReadableAndNotEmpty(chromosomeLengthFile)) {
            errorMessages.push("Chromosome length file ${chromosomeLengthFile} is either not readable or empty." as String)
        }

        Path gcContentFile = referenceGenomeService.gcContentPath(bamFileDisease.mergingWorkPackage)
        if (!gcContentFile) {
            errorMessages.push("gc content file can not be found.")
        } else if (!fileService.isFileReadableAndNotEmpty(gcContentFile)) {
            errorMessages.push("gc content file ${gcContentFile} is either not readable or empty." as String)
        }

        SophiaInstance sophiaInstance = getSophiaInstance(workflowStep)
        Path aceseqInputFile = sophiaLinkFileService.getFinalAceseqInputFile(sophiaInstance)
        if (!aceseqInputFile) {
            errorMessages.push("ACEseq input file can not be found.")
        } else if (!fileService.isFileReadableAndNotEmpty(aceseqInputFile)) {
            errorMessages.push("ACEseq input file ${aceseqInputFile} is either not readable or empty." as String)
        }

        return errorMessages
    }
}
