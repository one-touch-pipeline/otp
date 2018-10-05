package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import htsjdk.samtools.*

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

abstract class AbstractRoddyAlignmentJob extends AbstractExecutePanCanJob<RoddyBamFile> {

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

        cValues.add("possibleControlSampleNamePrefixes:${roddyBamFile.getSampleType().dirName}")
        cValues.add("possibleTumorSampleNamePrefixes:")

        if (!roddyBamFile.seqType.isRna() && roddyBamFile.config.adapterTrimmingNeeded) {
            cValues.add("useAdaptorTrimming:true")
            String adapterFile = exactlyOneElement(roddyBamFile.containedSeqTracks*.libraryPreparationKit*.adapterFile.unique(), "There is not exactly one adapter available for BAM file ${roddyBamFile}")
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

        assert [AbstractMergedBamFile.FileOperationStatus.DECLARED, AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING].contains(roddyBamFile.fileOperationStatus)
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
        assert roddyBamFile.save(flush: true)
    }


    void validateReadGroups(RoddyBamFile bamFile) {
        File bamFilePath = bamFile.workBamFile

        final SamReaderFactory factory = SamReaderFactory.makeDefault()
                .enable(SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS)
        List<String> readGroupsInBam = factory.getFileHeader(bamFilePath).getReadGroups().collect { it.id }.sort()

        List<String> expectedReadGroups = bamFile.containedSeqTracks.collect { it.getReadGroupName() }.sort()

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
            File bamFilePath = baseBamFile.getPathForFurtherProcessing()
            assert bamFilePath.exists()
            assert baseBamFile.fileSize == bamFilePath.length()
        }
    }

    protected abstract void workflowSpecificValidation(RoddyBamFile bamFile)
}
