/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow

abstract class AlignmentWorkflow implements OtpWorkflow {

    public static final String INPUT_FASTQ = "FASTQ"
    public static final String INPUT_FASTQC = "FASTQC"
    public static final String OUTPUT_BAM = "BAM"

    /**
     * Resolve the name of the work directory for the copied bam file.
     */
    abstract String buildWorkDirectoryName(AbstractMergingWorkPackage mergingWorkPackage, int identifier)

    /**
     * Since it is designed for repeated run, it creates and returns a new artefact
     */
    @Override
    @CompileDynamic
    Artefact createCopyOfArtefact(Artefact artefact) {
        AbstractBamFile bamFile = artefact as AbstractBamFile
        bamFile.withdrawn = true
        bamFile.save(flush: true)

        AbstractMergingWorkPackage mergingWorkPackage = bamFile.mergingWorkPackage
        int identifier = bamFile.nextIdentifier(mergingWorkPackage)

        AbstractBamFile outputBamFile = bamFile.class.newInstance() as AbstractBamFile
        outputBamFile.workPackage = mergingWorkPackage
        outputBamFile.identifier = identifier
        outputBamFile.workDirectoryName = buildWorkDirectoryName(mergingWorkPackage, identifier)
        outputBamFile.seqTracks = bamFile.seqTracks.collect() as Set
        outputBamFile.numberOfMergedLanes = bamFile.containedSeqTracks.size()
        if (outputBamFile.hasProperty('config')) {
            outputBamFile.config = bamFile.config
        }
        outputBamFile.save(flush: true)

        return outputBamFile
    }

    @Override
    boolean isAlignment() {
        return true
    }

    @Override
    boolean isAnalysis() {
        return false
    }
}
