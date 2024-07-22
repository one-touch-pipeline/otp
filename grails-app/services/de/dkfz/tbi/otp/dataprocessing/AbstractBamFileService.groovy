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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Path

@Transactional
class AbstractBamFileService {

    IndividualService individualService

    Path getBaseDirectory(AbstractBamFile bamFile) {
        String antiBodyTarget = bamFile.seqType.hasAntibodyTarget ? "-${((MergingWorkPackage) bamFile.mergingWorkPackage).antibodyTarget.name}" : ''
        Path viewByPid = individualService.getViewByPidPath(bamFile.individual, bamFile.seqType)
        return viewByPid.resolve("${bamFile.sample.sampleType.dirName}${antiBodyTarget}")
                .resolve(bamFile.seqType.libraryLayoutDirName)
                .resolve('merged-alignment')
    }

    @CompileDynamic
    void updateSamplePairStatusToNeedProcessing(AbstractBamFile finishedBamFile) {
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

    File getExistingBamFilePath(final AbstractBamFile bamFile) {
        final File file = bamFile.pathForFurtherProcessing
        assert bamFile.md5sum ==~ /^[0-9a-f]{32}$/
        assert bamFile.fileSize > 0L
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        assert file.length() == bamFile.fileSize
        return file
    }

    AbstractBamFile findById(long id) {
        return AbstractBamFile.get(id)
    }

    /**
     * Returns all the AbstractBamFiles given by individual, sampleType and seqType.
     * Only individual is required, others are optional. Missing parameters or null values means without these condition/constrains.
     * For example: if sampleType is null or missing, then all sampleTypes are taken into account.
     * The same is true with seqType.
     *
     * @param individual required
     * @param sampleType all sample types if missing
     * @param seqType all seq types if missing
     * @return all the AbstractBamFile
     */
    @CompileDynamic
    List<AbstractBamFile> findAllByIndividualSampleTypeSeqType(Individual individual, SampleType sampleType = null, SeqType seqType = null) {
        return AbstractBamFile.createCriteria().list {
            eq("withdrawn", false)
            eq("fileOperationStatus",
                    AbstractBamFile.FileOperationStatus.PROCESSED)
            workPackage {
                sample {
                    eq('individual', individual)
                    if (sampleType) {
                        eq('sampleType', sampleType)
                    }
                }
                if (seqType) {
                    eq('seqType', seqType)
                }
            }
        }.findAll { it.isMostRecentBamFile() }
    }

    @CompileDynamic
    List<AbstractBamFile> findAllByProjectAndSampleType(Project project, Set<SampleType> sampleTypes) {
        return AbstractBamFile.createCriteria().list {
            eq("withdrawn", false)
            eq("fileOperationStatus",
                    AbstractBamFile.FileOperationStatus.PROCESSED)
            workPackage {
                sample {
                    individual {
                        eq('project', project)
                    }
                    'in'('sampleType', sampleTypes)
                }
            }
        } as List<AbstractBamFile>
    }

    Double calculateCoverageWithN(AbstractBamFile bamFile) {
        assert bamFile : 'Parameter bamFile must not be null'

        if (!bamFile.seqType.needsBedFile) {
            Long length
            Long basesMapped

            ReferenceGenome referenceGenome = bamFile.referenceGenome
            assert referenceGenome : "Unable to find a reference genome for the BAM file ${bamFile}"

            length = referenceGenome.length
            basesMapped = bamFile.qualityAssessment.qcBasesMapped

            return basesMapped.doubleValue() / length.doubleValue()
        }
        // In case of sequencing types that need a BED file this value stays 'null' since there is no differentiation between 'with N' and 'without N'
        return null
    }

    /**
     * @Deprecated methods accessing the database shouldn't be static, since then the transaction proxies does not work.
     */
    @Deprecated
    @CompileDynamic
    static AbstractBamFile saveBamFile(AbstractBamFile bamFile) {
        return bamFile.save(flush: true)
    }
}
