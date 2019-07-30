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

    static final String HEADER_FASTQ_SINGLE_FILE = 'Sample alias,Fastq File,Checksum,Unencrypted checksum'

    static final String HEADER_FASTQ_PAIRED_FILE =
            'Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum'

    static final String HEADER_BAM_FILE = 'Sample alias,BAM File,Checksum,Unencrypted checksum'

    LsdfFilesService lsdfFilesService

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

    Map<String, String> createSingleFastqFileMapping(EgaSubmission egaSubmission) {
        log.debug("creating single fastq mappings for ${egaSubmission}")
        Map<String, String> fileFileContent = [:]
        egaSubmission.dataFilesToSubmit.findAll { DataFileSubmissionObject dataFileSubmissionObject ->
            dataFileSubmissionObject.dataFile.seqType.libraryLayout == LibraryLayout.SINGLE
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

    Map<String, String> createPairedFastqFileMapping(EgaSubmission egaSubmission) {
        log.debug("creating paired fastq mappings for ${egaSubmission}")
        Map<String, String> fileFileContent = [:]
        egaSubmission.dataFilesToSubmit.findAll { DataFileSubmissionObject dataFileSubmissionObject ->
            dataFileSubmissionObject.dataFile.seqType.libraryLayout == LibraryLayout.PAIRED
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
