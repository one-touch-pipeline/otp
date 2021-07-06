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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*

import static org.springframework.util.Assert.notNull

@Transactional
class AbstractMergedBamFileService {

    /**
     * @deprecated Use bamFile.baseDirectory instead, which can also handle chipseq directories
     * @param bamFile , the mergedBamFile which has to be copied
     * @return the final directory of the mergedBamFile after copying
     */
    @Deprecated
    static String destinationDirectory(AbstractMergedBamFile bamFile) {
        notNull(bamFile, "the input of the method destinationDirectory is null")
        return bamFile.baseDirectory.absolutePath + '/'
    }

    void updateSamplePairStatusToNeedProcessing(AbstractMergedBamFile finishedBamFile) {
        assert finishedBamFile: "The input bam file must not be null"
        SamplePair.createCriteria().list {
            or {
                eq('mergingWorkPackage1', finishedBamFile.workPackage)
                eq('mergingWorkPackage2', finishedBamFile.workPackage)
            }
        }.each { SamplePair samplePair ->
            SeqType seqType = samplePair.seqType
            if (samplePair.snvProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED &&
                    SeqTypeService.snvPipelineSeqTypes.contains(seqType)) {
                samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            if (samplePair.indelProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED &&
                    SeqTypeService.indelPipelineSeqTypes.contains(seqType)) {
                samplePair.indelProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            if (samplePair.aceseqProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED &&
                    SeqTypeService.aceseqPipelineSeqTypes.contains(seqType)) {
                samplePair.aceseqProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            if (samplePair.sophiaProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED &&
                    SeqTypeService.sophiaPipelineSeqTypes.contains(seqType)) {
                samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            if (samplePair.runYapsaProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED &&
                    SeqTypeService.runYapsaPipelineSeqTypes.contains(seqType)) {
                samplePair.runYapsaProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            }
            assert samplePair.save(flush: true)
        }
    }

    File getExistingBamFilePath(final AbstractMergedBamFile bamFile) {
        final File file = bamFile.getPathForFurtherProcessing()
        assert bamFile.getMd5sum() ==~ /^[0-9a-f]{32}$/
        assert bamFile.getFileSize() > 0L
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        assert file.length() == bamFile.getFileSize()
        return file
    }

    List<AbstractMergedBamFile> getActiveBlockedBamsContainingSeqTracks(List<SeqTrack> sts) {
        List<MergingWorkPackage> mwps = sts ? MergingWorkPackage.withCriteria {
            seqTracks {
                inList("id", sts*.id)
            }
        } as List<MergingWorkPackage> : []

        return mwps ? AbstractMergedBamFile.withCriteria {
            'in'("workPackage", mwps)
            eq("qcTrafficLightStatus", AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED)
            eq("withdrawn", false)
        } as List<AbstractMergedBamFile> : []
    }
}
