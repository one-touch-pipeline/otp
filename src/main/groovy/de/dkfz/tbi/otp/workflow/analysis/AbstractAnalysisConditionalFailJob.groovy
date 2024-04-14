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
package de.dkfz.tbi.otp.workflow.analysis

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.jobs.AbstractConditionalFailJob
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Slf4j
abstract class AbstractAnalysisConditionalFailJob extends AbstractConditionalFailJob implements AnalysisWorkflowShared {

    @Autowired
    AbstractBamFileService abstractBamFileService

    @Autowired
    FileService fileService

    @Autowired
    ReferenceGenomeService referenceGenomeService

    /**
     * Perform other checks if needed
     *
     * @param workflowStep is the analysis instance for the workflow
     * @return a list of errors
     */
    abstract List<String> doFurtherCheck(WorkflowStep workflowStep, AbstractBamFile bamFileDisease, AbstractBamFile bamFileControl)

    /**
     * Get the concrete analysis instance for the workflow
     *
     * @param workflowStep the current step in workflow
     * @return analysis instance for the workflow
     */
    abstract BamFilePairAnalysis getAnalysisInstance(WorkflowStep workflowStep)

    @Override
    protected void check(WorkflowStep workflowStep) {
        List<String> errorMessages = []
        BamFilePairAnalysis analysis = getAnalysisInstance(workflowStep)

        assert analysis: "The input Analysis Instance must not be null."

        AbstractBamFile bamFileDisease = getTumorBamFile(workflowStep)
        AbstractBamFile bamFileControl = getControlBamFile(workflowStep)

        // If exist, they should have the same seqType
        if (bamFileDisease.seqType != bamFileControl.seqType) {
            errorMessages.push("The Tumor and Control BAM files do not have the same seqType.")
        }

        [bamFileDisease, bamFileControl].each {
            // They should not have been withdrawn
            if (it.withdrawn) {
                errorMessages.push("Bam File of ${it.bamFileName} has been withdrawn while this job processed it." as String)
            }

            // Ensure all the files exist and readable
            if (!fileService.isFileReadableAndNotEmpty(abstractBamFileService.getBaseDirectory(it).resolve(it.bamFileName))) {
                errorMessages.push("Path to bam file of ${it.bamFileName} is either null or not readable." as String)
            }
        }

        // Check the reference genome
        ReferenceGenome referenceGenome = bamFileDisease.referenceGenome
        Path referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome).toPath()
        if (!referenceGenomeFastaFile) {
            errorMessages.push("Reference genome file ${referenceGenomeFastaFile} can not be found." as String)
        } else if (!fileService.isFileReadableAndNotEmpty(referenceGenomeFastaFile)) {
            errorMessages.push("Path to the reference genome file ${referenceGenomeFastaFile} is either not readable or empty." as String)
        }

        // Some other checks to make
        errorMessages.addAll(doFurtherCheck(workflowStep, bamFileDisease, bamFileControl))

        if (errorMessages) {
            throw new WorkflowException(errorMessages.join('\n'))
        }
    }
}
