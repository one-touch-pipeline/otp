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
package de.dkfz.tbi.otp.egaSubmission

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileStatic
@Transactional
class EgaFileContentService {

    static final String UPLOAD_FILE_NAME = 'filesToUpload.tsv'

    /** EGA-specified header for single-end fastq tables. */
    static final String HEADER_FASTQ_SINGLE_FILE = 'Sample alias,Fastq File,Checksum,Unencrypted checksum'

    /** EGA-specified header for paired-end fastq tables. */
    static final String HEADER_FASTQ_PAIRED_FILE =
            'Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum'

    /** EGA-specified header for bam tables. */
    static final String HEADER_BAM_FILE = 'Sample alias,BAM File,Checksum,Unencrypted checksum'

    LsdfFilesService lsdfFilesService

    /**
     * Creates the mapping file of internal path to EGA name, as needed for ega-cluster-cryptor encryption.
     *
     * A list of all data files and bams in this submission, listing:
     * /absolute/path/to/selected/file.ext<TAB>public-filename-for-ega.ext
     *
     * @see https://github.com/DKFZ-ODCF/ega-cluster-cryptor
     * @return a map of filename -> file-content-as-string
     */
    Map<String, String> createFilesToUploadFileContent(EgaSubmission submission) {
        log.debug("creating mapping file for ${submission}")
        StringBuilder out = new StringBuilder()

        submission.dataFilesToSubmit.each {
            out << lsdfFilesService.getFileFinalPath(it.dataFile) << '\t' << it.egaAliasName << '\n'
        }

        submission.bamFilesToSubmit.each {
            out << it.bamFile.pathForFurtherProcessing << '\t' << it.egaAliasName << '\n'
        }

        return [
                (UPLOAD_FILE_NAME): out.toString()
        ]
    }

    /**
     * Collects all the EGA-relevant experimental information for a datafile, to enable grouping by protocol during submission.
     *
     * An EGA "experiment" describes what was done to a sample in the lab and how it was sequenced.
     * Each "experiment" is a distinct combination of these parameters.
     * When registering file metadata in the EGA submitter portal, this can be batched per experiment and file-layout,
     * to avoid redundantly entering the same information over and over again.
     *
     * Elements unknown to OTP are replaced with "unspecified"
     *
     * @return a string of underscore-separated experiment components, as far as known to OTP.
     */
    String createKeyForFastq(DataFileSubmissionObject dataFileSubmissionObject) {
        DataFile dataFile = dataFileSubmissionObject.dataFile
        SeqTrack seqTrack = dataFile.seqTrack
        SeqType seqType = seqTrack.seqType
        return [
                seqType.displayName,
                seqType.libraryLayout,
                seqTrack.seqPlatform.name,
                seqTrack.seqPlatform.seqPlatformModelLabel?.name ?: 'unspecified',
                seqTrack.libraryPreparationKit?.name ?: 'unspecified',
        ].join('-').replace(' ', '_')
    }

    /**
     * Collects all the EGA-relevant experimental information for a bamfile, to enable grouping by protocol during submission.
     *
     * An EGA "experiment" describes what was done to a sample in the lab and how it was sequenced.
     * Each "experiment" is a distinct combination of these parameters.
     * When registering file metadata in the EGA submitter portal, this can be batched per experiment and file-layout,
     * to avoid redundantly entering the same information over and over again.
     *
     * Elements unknown to OTP are replaced with "unspecified"
     *
     * Since bamfiles can contain multiple merged fastq, which may or may not be sequenced in exactly the same way,
     * this key can become quite verbose for bamfiles: it lists all distinct library preparation kits and sequencing platforms
     * that were used for its constituent fastqs.
     *
     * @return a string of underscore-separated experiment components, as far as known to OTP.
     */
    String createKeyForBamFile(BamFileSubmissionObject bamFileSubmissionObject) {
        AbstractBamFile bamFile = bamFileSubmissionObject.bamFile
        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks

        List<SeqPlatform> seqPlatforms = seqTracks*.seqPlatform
        List<List<String>> seqPlatformNames = seqPlatforms.collect { SeqPlatform seqPlatform ->
            [
                    seqPlatform.name,
                    seqPlatform.seqPlatformModelLabel?.name ?: 'unspecified',
            ]
        }.unique().sort()

        List<LibraryPreparationKit> libraryPreparationKits = seqTracks*.libraryPreparationKit
        List<String> libPrepKitNames = libraryPreparationKits.collect { LibraryPreparationKit libraryPreparationKit ->
            libraryPreparationKit?.name ?: 'unspecified'
        }.unique().sort()

        return [
                bamFile.seqType.displayName,
                bamFile.seqType.libraryLayout,
                seqPlatformNames,
                libPrepKitNames,
        ].flatten().join('-').replace(' ', '_')
    }

    /**
     * Prepares run-registration tables for single-end samples in this submission, one table per experimental protocol.
     *
     * These tables allow one to "link files and samples" in the EGA submitter portal, and represent the mapping
     * between a biological sample, the experimental protocol applied to that sample, and the resulting data file(s).
     *
     * This method prepares the single-fastq section of this submission, according to EGA's 'samples-one-fastq-file-single' template
     *
     * @param egaSubmission a fully populated submission object
     * @return a map of table-filename -> file-content-as-string.
     */
    Map<String, String> createSingleFastqFileMapping(EgaSubmission egaSubmission) {
        log.debug("creating single fastq mappings for ${egaSubmission}")
        Map<String, String> fileFileContent = [:]
        egaSubmission.dataFilesToSubmit.findAll { DataFileSubmissionObject dataFileSubmissionObject ->
            dataFileSubmissionObject.dataFile.seqType.libraryLayout == SequencingReadType.SINGLE
        }.groupBy { DataFileSubmissionObject dataFileSubmissionObject ->
            createKeyForFastq(dataFileSubmissionObject)
        }.each { String key, List<DataFileSubmissionObject> dataFileSubmissionObjects ->
            String fileName = "runs-fastqs-${key}.csv"
            log.debug("    ${fileName}")
            String content = dataFileSubmissionObjects.collect { DataFileSubmissionObject dataFileSubmissionObject ->
                [
                        dataFileSubmissionObject.sampleSubmissionObject.egaAliasName,
                        dataFileSubmissionObject.egaAliasName,
                        '',
                        '',
                ].join(',')
            }.join('\n')
            fileFileContent[fileName] = "${HEADER_FASTQ_SINGLE_FILE}\n${content}\n".toString()
        }
        return fileFileContent
    }

    /**
     * Prepares run-registration tables for paired-end samples in this submission, one table per experimental protocol.
     *
     * These tables allow one to "link files and samples" in the EGA submitter portal, and represent the mapping
     * between a biological sample, the experimental protocol applied to that sample, and the resulting data file(s).
     *
     * This method prepares the paired-fastq section of this submission, according to EGA's 'samples-two-fastq-files-paired' template.
     *
     * @param egaSubmission a fully populated submission object
     * @return a map of table-filename -> file-content-as-string.
     */
    Map<String, String> createPairedFastqFileMapping(EgaSubmission egaSubmission) {
        log.debug("creating paired fastq mappings for ${egaSubmission}")
        Map<String, String> fileFileContent = [:]
        egaSubmission.dataFilesToSubmit.findAll { DataFileSubmissionObject dataFileSubmissionObject ->
            dataFileSubmissionObject.dataFile.seqType.libraryLayout == SequencingReadType.PAIRED
        }.groupBy { DataFileSubmissionObject dataFileSubmissionObject ->
            createKeyForFastq(dataFileSubmissionObject)
        }.each { String key, List<DataFileSubmissionObject> dataFileSubmissionObjects ->
            String fileName = "runs-fastqs-${key}.csv"
            log.debug("    ${fileName}")
            String content = dataFileSubmissionObjects.groupBy {
                it.dataFile.seqTrack
            }.collect { SeqTrack seqTrack, List<DataFileSubmissionObject> dataFileSubmissionObjectPerSeqTrack ->
                assert dataFileSubmissionObjectPerSeqTrack.size() == 2
                List<String> fileAlias = dataFileSubmissionObjectPerSeqTrack*.egaAliasName.sort()
                SampleSubmissionObject sampleSubmissionObject = CollectionUtils.exactlyOneElement(
                        dataFileSubmissionObjectPerSeqTrack*.sampleSubmissionObject.unique())
                return [
                        sampleSubmissionObject.egaAliasName,
                        fileAlias[0],
                        '',
                        '',
                        fileAlias[1],
                        '',
                        '',
                ].join(',')
            }.join('\n')
            fileFileContent[fileName] = "${HEADER_FASTQ_PAIRED_FILE}\n${content}\n".toString()
        }
        return fileFileContent
    }

    /**
     * Prepares run-registration tables for bam samples in this submission, one table per experimental protocol.
     *
     * These tables allow one to "link files and samples" in the EGA submitter portal, and represent the mapping
     * between a biological sample, the experimental protocol applied to that sample, and the resulting data file(s).
     *
     * This method prepares the bam section of this submission, according to EGA's 'samples-bam' template.
     *
     * @param egaSubmission a fully populated submission object
     * @return a map of table-filename -> file-content-as-string.
     */
    Map<String, String> createBamFileMapping(EgaSubmission egaSubmission) {
        log.debug("creating bam mappings for ${egaSubmission}")
        Map<String, String> fileFileContent = [:]
        egaSubmission.bamFilesToSubmit.groupBy {
            createKeyForBamFile(it)
        }.each { String key, List<BamFileSubmissionObject> submissionBamFiles ->
            String fileName = "runs-bams-${key}.csv"
            log.debug("    ${fileName}")
            String content = submissionBamFiles.collect { BamFileSubmissionObject bamFileSubmissionObject ->
                [
                        bamFileSubmissionObject.sampleSubmissionObject.egaAliasName,
                        bamFileSubmissionObject.egaAliasName,
                        '',
                        '',
                ].join(',')
            }.join('\n')
            fileFileContent[fileName] = "${HEADER_BAM_FILE}\n${content}\n".toString()
        }
        return fileFileContent
    }
}
