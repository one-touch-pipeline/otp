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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import groovy.transform.CompileDynamic
import htsjdk.samtools.SamReaderFactory

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.ensureFileIsReadableAndNotEmpty
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@CompileDynamic
abstract class AbstractRoddyAlignmentJob extends AbstractExecutePanCanJob<RoddyBamFile> {

    /**
     * @deprecated use {@link RoddyConfigValueService#getAlignmentValues()}
     */
    @Deprecated
    List<String> prepareAndReturnAlignmentCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile

        List<String> cValues = []
        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome)
        assert referenceGenomeFastaFile: "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)
        cValues.add("INDEX_PREFIX:${referenceGenomeFastaFile}") //used for pancan pipeline
        cValues.add("GENOME_FA:${referenceGenomeFastaFile}")    //used for rna pipeline

        if (!roddyBamFile.seqType.isRna()) {
            File chromosomeStatSizeFile = referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage)
            assert chromosomeStatSizeFile: "Path to the chromosome stat size file is null"
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(chromosomeStatSizeFile)
            cValues.add("CHROM_SIZES_FILE:${chromosomeStatSizeFile}")
        }

        roddyBamFile.mergingWorkPackage.alignmentProperties.each { MergingWorkPackageAlignmentProperty alignmentProperty ->
            cValues.add("${alignmentProperty.name}:${alignmentProperty.value}")
        }

        cValues.add("possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}")
        cValues.add("possibleTumorSampleNamePrefixes:")

        if (!roddyBamFile.seqType.isRna() && roddyBamFile.config.adapterTrimmingNeeded) {
            cValues.add("useAdaptorTrimming:true")
            String adapterFile = exactlyOneElement(
                    roddyBamFile.containedSeqTracks*.libraryPreparationKit*.adapterFile.unique(),
                    "There is not exactly one adapter available for BAM file ${roddyBamFile}"
            )
            assert adapterFile : "There is exactly one adapter available for BAM file ${roddyBamFile}, but it is null"
            cValues.add("CLIP_INDEX:${adapterFile}")
        }

        if (roddyBamFile.project.fingerPrinting && roddyBamFile.referenceGenome.fingerPrintingFileName) {
            File fingerPrintingFile = referenceGenomeService.fingerPrintingFile(roddyBamFile.referenceGenome)
            cValues.add("runFingerprinting:true")
            cValues.add("fingerprintingSitesFile:${fingerPrintingFile.path}")
        } else {
            cValues.add("runFingerprinting:false")
        }

        return cValues
    }

    @Override
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    protected void validate(RoddyBamFile roddyBamFile) throws Throwable {
        assert roddyBamFile: "Input roddyBamFile must not be null"

        executeRoddyCommandService.correctPermissions(roddyBamFile, roddyBamFile.project.realm)

        try {
            ensureCorrectBaseBamFileIsOnFileSystem(roddyBamFile.baseBamFile)
        } catch (AssertionError e) {
            throw new RuntimeException('The input BAM file seems to have changed on the file system while this job was processing it.', e)
        }

        ['Bam', 'Bai', 'Md5sum'].each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(roddyBamFile."work${it}File")
        }

        validateReadGroups(roddyBamFile)

        ['MergedQA', 'ExecutionStore'].each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(roddyBamFile."work${it}Directory")
        }

        workflowSpecificValidation(roddyBamFile)

        ensureFileIsReadableAndNotEmpty(roddyBamFile.workMergedQAJsonFile)

        if (!roddyBamFile.seqType.isRna()) {
            roddyBamFile.workSingleLaneQAJsonFiles.values().each {
                ensureFileIsReadableAndNotEmpty(it)
            }
        }

        assert [AbstractBamFile.FileOperationStatus.DECLARED,
                AbstractBamFile.FileOperationStatus.NEEDS_PROCESSING].contains(roddyBamFile.fileOperationStatus)
        roddyBamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.NEEDS_PROCESSING
        assert roddyBamFile.save(flush: true)
    }

    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    void validateReadGroups(RoddyBamFile bamFile) {
        File bamFilePath = bamFile.workBamFile

        final SamReaderFactory factory = SamReaderFactory.makeDefault()
                .enable(SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS)
        List<String> readGroupsInBam = factory.getFileHeader(bamFilePath).readGroups*.id.sort()

        List<String> expectedReadGroups = bamFile.containedSeqTracks*.readGroupName.sort()

        if (readGroupsInBam != expectedReadGroups) {
            throw new RuntimeException("""Read groups in BAM file are not as expected.
                                         |Read groups in ${bamFilePath}:
                                         |${readGroupsInBam.join('\n')}
                                         |Expected read groups:
                                         |${expectedReadGroups.join('\n')}
                                         |""".stripMargin()
            )
        }
    }

    void ensureCorrectBaseBamFileIsOnFileSystem(RoddyBamFile baseBamFile) {
        if (baseBamFile) {
            File bamFilePath = baseBamFile.pathForFurtherProcessing
            assert bamFilePath.exists()
            assert baseBamFile.fileSize == bamFilePath.length()
        }
    }

    protected abstract void workflowSpecificValidation(RoddyBamFile bamFile)
}
