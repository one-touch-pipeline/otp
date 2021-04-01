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
package de.dkfz.tbi.otp.domainFactory.pipelines.externalBam

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsAlignment
import de.dkfz.tbi.otp.ngsdata.SequencingReadType
import de.dkfz.tbi.otp.utils.HelperUtils

trait ExternalBamFactory implements IsAlignment {

    private final String seqTrackName = "seqTrack_${nextId}"

    @Override
    Pipeline findOrCreatePipeline() {
        findOrCreatePipeline(Pipeline.Name.EXTERNALLY_PROCESSED, Pipeline.Type.ALIGNMENT)
    }

    @Override
    ExternalMergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true) {
        createDomainObject(ExternalMergingWorkPackage, [
                sample         : { createSample() },
                seqType        : { createSeqType() },
                pipeline       : { findOrCreatePipeline() },
                referenceGenome: { createReferenceGenome() },
        ], properties, saveAndValidate)
    }

    @Override
    ExternallyProcessedMergedBamFile createBamFile(Map properties = [:]) {
        return createDomainObject(ExternallyProcessedMergedBamFile, [
                fileName           : "bamfile_${nextId}.bam",
                workPackage        : { createMergingWorkPackage() },
                numberOfMergedLanes: null,
                importedFrom       : "/importFrom_${nextId}",
                furtherFiles       : [],
        ], properties)
    }

    ExternallyProcessedMergedBamFile createFinishedBamFile(Map properties = [:]) {
        ExternallyProcessedMergedBamFile externallyProcessedMergedBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum             : HelperUtils.randomMd5sum,
                fileSize           : nextId,
        ] + properties)
        ExternalMergingWorkPackage externalMergingWorkPackage = externallyProcessedMergedBamFile.mergingWorkPackage
        externalMergingWorkPackage.bamFileInProjectFolder = externallyProcessedMergedBamFile
        assert externalMergingWorkPackage.save(flush: true)
        return externallyProcessedMergedBamFile
    }

    @Override
    Map getSeqTypeProperties() {
        return [
                name         : seqTrackName,
                displayName  : "${seqTrackName}_alias",
                dirName      : "${seqTrackName}_sequencing",
                roddyName    : null,
                libraryLayout: SequencingReadType.PAIRED,
                singleCell   : false,
        ]
    }

    @Override
    Map getConfigProperties(Map properties) {
        throw new OtpRuntimeException('not available')
    }

    @SuppressWarnings('GetterMethodCouldBeProperty')
    @Override
    Class getConfigPerProjectAndSeqTypeClass() {
        throw new OtpRuntimeException('not available')
    }

    @SuppressWarnings('GetterMethodCouldBeProperty')
    @Override
    Class getQaClass() {
        return ExternalProcessedMergedBamFileQualityAssessment
    }

    @Override
    Map getQaValuesProperties() {
        return [
                properlyPaired    : 1919,
                pairedInSequencing: 2120,
                insertSizeMedian  : 406,
                insertSizeCV      : 23,
        ]
    }

    ImportProcess createImportProcess(Map properties) {
        return createDomainObject(ImportProcess, [
                externallyProcessedMergedBamFiles: { [createBamFile()] as Set },
                state                            : ImportProcess.State.NOT_STARTED,
                linkOperation                    : ImportProcess.LinkOperation.COPY_AND_KEEP,
        ], properties)
    }
}

class ExternalBamFactoryInstance implements ExternalBamFactory {

    static final ExternalBamFactoryInstance INSTANCE = new ExternalBamFactoryInstance()
}
