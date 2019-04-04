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
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.hibernate.criterion.CriteriaSpecification
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*

@CompileStatic
@Transactional
class EgaSubmissionService {

    SeqTrackService seqTrackService

    enum FileType {
        BAM,
        FASTQ,
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(params.project, 'OTP_READ_ACCESS')")
    EgaSubmission createSubmission(Map params) {
        EgaSubmission submission = new EgaSubmission( params + [
                state: EgaSubmission.State.SELECTION,
                selectionState: EgaSubmission.SelectionState.SELECT_SAMPLES,
        ])
        assert submission.save(flush: true, failOnError: true)

        return submission
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateSubmissionState (EgaSubmission submission, EgaSubmission.State state) {
        submission.state = state
        submission.save(flush: true)
    }

    @CompileDynamic
    List<SeqType> seqTypeByProject(Project project) {
        List<Long> seqTypeIds = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeId")
            }
        }
        List<SeqType> seqTypes = []
        if (seqTypeIds) {
            seqTypes = SeqType.withCriteria {
                'in'("id", seqTypeIds)
                order("name")
                order("libraryLayout")
            }
        }
        return seqTypes
    }

    void saveSampleSubmissionObject(EgaSubmission submission, Sample sample, SeqType seqType) {
        SampleSubmissionObject sampleSubmissionObject = new SampleSubmissionObject(
                sample: sample,
                seqType: seqType
        ).save(flush: true)
        submission.samplesToSubmit.add(sampleSubmissionObject)
        submission.selectionState = EgaSubmission.SelectionState.SAMPLE_INFORMATION
        submission.save(flush: true)
    }

    @CompileDynamic
    Map<SampleSubmissionObject, Boolean> checkFastqFiles(EgaSubmission submission) {
        Map<SampleSubmissionObject, Boolean> map = [:]

        submission.samplesToSubmit.each {
            List<DataFile> dataFiles = SeqTrack.findBySampleAndSeqType(it.sample, it.seqType).dataFiles.findAll {
                !it.fileWithdrawn
            }
            if (dataFiles.empty) {
                map.put(it, false)
            } else {
                map.put(it, dataFiles.first().fileExists)
            }
        }

        return map
    }

    Map<SampleSubmissionObject, Boolean> checkBamFiles(EgaSubmission submission) {
        Map<SampleSubmissionObject, Boolean> map = new HashMap<>(submission.samplesToSubmit.size())

        submission.samplesToSubmit.each {
            SampleSubmissionObject sampleSubmissionObject = it as SampleSubmissionObject
            map.put(sampleSubmissionObject, !getAbstractMergedBamFiles(sampleSubmissionObject).empty)
        }

        return map
    }

    void updateSampleSubmissionObjects(EgaSubmission submission, List<String> sampleObjectId, List<String> alias, List<FileType> fileType) {
        if (sampleObjectId.size() == alias.size() && sampleObjectId.size() == fileType.size()) {
            sampleObjectId.eachWithIndex { it, i ->
                SampleSubmissionObject sampleSubmissionObject = SampleSubmissionObject.get(it as Long)
                sampleSubmissionObject.egaAliasName = alias[i]
                sampleSubmissionObject.useBamFile = fileType[i] == FileType.BAM
                sampleSubmissionObject.useFastqFile = fileType[i] == FileType.FASTQ
                sampleSubmissionObject.save(flush: true)
            }
            if (submission.samplesToSubmit.any { it.useFastqFile } ) {
                submission.selectionState = EgaSubmission.SelectionState.SELECT_FASTQ_FILES
            } else {
                submission.selectionState = EgaSubmission.SelectionState.SELECT_BAM_FILES
            }
            submission.save(flush: true)
        }
    }

    List deleteSampleSubmissionObjects(EgaSubmission submission) {
        List samplesWithSeqType = []
        submission.samplesToSubmit.toArray().each { SampleSubmissionObject it ->
            submission.samplesToSubmit.remove(it)
            samplesWithSeqType.add("${it.sample.id}${it.seqType.toString()}")
            it.delete()
        }
        submission.selectionState = EgaSubmission.SelectionState.SELECT_SAMPLES
        submission.save(flush: true)

        return samplesWithSeqType
    }

    void updateDataFileSubmissionObjects(List<String> filenames, List<String> egaFileAlias, EgaSubmission submission) {
        filenames.eachWithIndex { filename, i ->
            DataFileSubmissionObject dataFileSubmissionObject = submission.dataFilesToSubmit.find {
                it.dataFile.fileName == filename
            }
            dataFileSubmissionObject.egaAliasName = egaFileAlias[i]
            dataFileSubmissionObject.save(flush: true)
        }
        if (submission.samplesToSubmit.any { it.useBamFile } ) {
            submission.selectionState = EgaSubmission.SelectionState.SELECT_BAM_FILES
            submission.save(flush: true)
        }
    }

    @CompileDynamic
    List<List> getDataFilesAndAlias(EgaSubmission submission) {
        if (submission.dataFilesToSubmit) {
            return submission.dataFilesToSubmit.collect {
                DataFile file = it.dataFile
                [file, submission.samplesToSubmit.find { it.sample == file.seqTrack.sample && it.seqType == file.seqType }.egaAliasName]
            }.sort { it[1] }
        } else {
            return submission.samplesToSubmit.findAll { it.useFastqFile }.collectMany {
                seqTrackService.getSequenceFilesForSeqTrack(SeqTrack.findBySampleAndSeqType(it.sample, it.seqType))
            }.collect { DataFile file ->
                [file, submission.samplesToSubmit.find { it.sample == file.seqTrack.sample && it.seqType == file.getSeqType() }.egaAliasName]
            }.sort { it[1] }
        }
    }

    @CompileDynamic
    List<List> getBamFilesAndAlias(EgaSubmission submission) {
        if (submission.bamFilesToSubmit) {
            return submission.bamFilesToSubmit.collect {
                AbstractMergedBamFile file = it.bamFile
                [file, submission.samplesToSubmit.find { it.sample == file.sample && it.seqType == file.seqType }.egaAliasName]
            }.sort { it[1] }
        } else {
            return submission.samplesToSubmit.findAll { it.useBamFile }.collectMany {
                getAbstractMergedBamFiles(it)
            }.collect { AbstractMergedBamFile file ->
                [file, submission.samplesToSubmit.find { it.sample == file.sample && it.seqType == file.seqType }.egaAliasName]
            }.sort { it[1] }
        }
    }

    void updateBamFileSubmissionObjects(List<String> fileIds, List<String> egaFileAliases, EgaSubmission submission) {
        fileIds.eachWithIndex { fileId, i ->
            BamFileSubmissionObject bamFileSubmissionObject = submission.bamFilesToSubmit.find {
                it.bamFile.id == fileId as long
            }
            bamFileSubmissionObject.egaAliasName = egaFileAliases[i]
            bamFileSubmissionObject.save(flush: true)
        }
    }

    @CompileDynamic
    private List<AbstractMergedBamFile> getAbstractMergedBamFiles(SampleSubmissionObject sampleSubmissionObject) {
        return AbstractMergedBamFile.createCriteria().list {
            workPackage {
                eq('sample', sampleSubmissionObject.sample)
                eq('seqType', sampleSubmissionObject.seqType)
            }
            eq('withdrawn', false)
            eq('fileOperationStatus', AbstractMergedBamFile.FileOperationStatus.PROCESSED)
        }.findAll {
            it.isMostRecentBamFile()
        }
    }

    @CompileDynamic
    void createDataFileSubmissionObjects(EgaSubmission submission, List<Boolean> selectBox, List<String> filename, List<String> egaSampleAlias) {
        selectBox.eachWithIndex { it, i ->
            if (it) {
                DataFileSubmissionObject dataFileSubmissionObject = new DataFileSubmissionObject(
                        dataFile: DataFile.findByFileName(filename[i]),
                        sampleSubmissionObject: SampleSubmissionObject.findByEgaAliasName(egaSampleAlias[i])
                ).save(flush: true)
                submission.addToDataFilesToSubmit(dataFileSubmissionObject)
            }
        }
    }

    @CompileDynamic
    void createBamFileSubmissionObjects(EgaSubmission submission) {
        getBamFilesAndAlias(submission).each {
            AbstractMergedBamFile bamFile = it[0] as AbstractMergedBamFile
            String egaSampleAlias = it[1] as String
            if (!(bamFile instanceof ExternallyProcessedMergedBamFile)) {
                BamFileSubmissionObject bamFileSubmissionObject = new BamFileSubmissionObject(
                        bamFile: bamFile,
                        sampleSubmissionObject: SampleSubmissionObject.findByEgaAliasName(egaSampleAlias),
                ).save(flush: true)
                submission.addToBamFilesToSubmit(bamFileSubmissionObject)
            }
        }
    }

    @CompileDynamic
    List<List> getSampleAndSeqType(Project project) {
        return SeqTrack.createCriteria().list {
            projections {
                sample {
                    property('id')
                    individual {
                        eq('project', project)
                        property('pid')
                        order('pid', 'asc')
                    }
                    sampleType {
                        property('name')
                    }
                }
                property('seqType')
            }
        }.unique()
    }

    Map generateDefaultEgaAliasesForDataFiles(List<List> dataFilesAndAliases) {
        Map<String, String> aliasNames = [:]

        dataFilesAndAliases.each {
            DataFile dataFile = it[0] as DataFile
            String runNameWithoutDate = dataFile.run.name.replaceAll("(?<!\\d)((?:20)?[0-2]\\d)-?(0\\d|1[012])-?([0-2]\\d|3[01])[-_]", "")
            List aliasNameHelper = [
                    dataFile.seqType.displayName,
                    dataFile.seqType.libraryLayout,
                    it[1],
                    dataFile.seqTrack.normalizedLibraryName,
                    runNameWithoutDate,
                    dataFile.seqTrack.laneId,
                    "R${dataFile.mateNumber}",
            ].findAll()
            String aliasName = "${aliasNameHelper.join("_")}.fastq.gz"
            aliasNames.put(dataFile.fileName + dataFile.run, aliasName)
        }

        return aliasNames
    }

    Map generateDefaultEgaAliasesForBamFiles(List<List> bamFilesAndAliases) {
        Map<String, String> aliasNames = [:]

        bamFilesAndAliases.each {
            AbstractMergedBamFile bamFile = it[0] as AbstractMergedBamFile
            List aliasNameHelper = [
                    bamFile.seqType.displayName,
                    bamFile.seqType.libraryLayout,
                    it[1],
                    bamFile.md5sum,
            ].findAll()
            String aliasName = "${aliasNameHelper.join("_")}.bam"
            aliasNames.put(bamFile.bamFileName + it[1], aliasName)
        }

        return aliasNames
    }

    @CompileDynamic
    List getExperimentalMetadata(EgaSubmission submission) {
        return SeqTrack.createCriteria().list {
            resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
            projections {
                'in'('sample', submission.samplesToSubmit*.sample)
                seqType {
                    property('libraryLayout', 'libraryLayout')
                    property('displayName', 'displayName')
                }
                property('libraryPreparationKit','libraryPreparationKit')
            }
        }.unique()
    }
}
