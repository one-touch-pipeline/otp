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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvLinkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisConditionalFailJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class RunYapsaConditionalFailJob extends AbstractAnalysisConditionalFailJob implements RunYapsaWorkflowShared {

    @Autowired
    BedFileService bedFileService

    @Autowired
    SnvLinkFileService snvLinkFileService

    @Override
    List<String> doFurtherCheck(WorkflowStep workflowStep, AbstractBamFile bamFileDisease, AbstractBamFile bamFileControl) {
        List<String> errorMessages = []

        // check the BED file
        ReferenceGenome referenceGenome = bamFileControl.referenceGenome
        LibraryPreparationKit libraryPreparationKit = bamFileControl.workPackage.libraryPreparationKit
        if (bamFileDisease.seqType.needsBedFile) {
            BedFile bedFile = bedFileService.findBedFileByReferenceGenomeAndLibraryPreparationKit(referenceGenome, libraryPreparationKit)
            try {
                bedFileService.filePath(bedFile)
            } catch (FileNotReadableException e) {
                errorMessages.push("Required BED file is missing or not readable.\n" + e.message)
            }
        }

        // RunYapsa runs after Snv has run
        RoddySnvCallingInstance snvInstance = getSnvInstance(workflowStep)
        if (!snvInstance) {
            errorMessages.push("Required SNV instance ${snvInstance} is null." as String)
        }

        Path resultPathForRunYapsa = snvLinkFileService.getResultRequiredForRunYapsa(snvInstance)
        if (!fileService.fileIsReadable(resultPathForRunYapsa)) {
            errorMessages.push("SNV result file ${resultPathForRunYapsa} cannot be found in linked view-by-pid folder." as String)
        }

        return errorMessages
    }

    @Override
    BamFilePairAnalysis getAnalysisInstance(WorkflowStep workflowStep) {
        return getRunYapsaInstance(workflowStep)
    }
}
