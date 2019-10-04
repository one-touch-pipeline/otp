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
import groovy.transform.*
import org.hibernate.criterion.CriteriaSpecification
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

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
        assert submission.save(flush: true)

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
            List<DataFile> dataFiles = SeqTrack.findBySampleAndSeqType(it.sample, it.seqType).dataFiles
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

    void updateDataFileSubmissionObjects(SelectFilesDataFilesFormSubmitCommand cmd) {
        EgaSubmission submission = cmd.submission
        List<String> egaFileAlias = cmd.egaFileAlias
        cmd.fastqFile.eachWithIndex { fastqIdString, i ->
            long fastqId = fastqIdString as long
            DataFileSubmissionObject dataFileSubmissionObject = submission.dataFilesToSubmit.find {
                it.dataFile.id == fastqId
            }
            dataFileSubmissionObject.egaAliasName = egaFileAlias[i]
            dataFileSubmissionObject.save(flush: false)
        }
        if (submission.samplesToSubmit.any { it.useBamFile } ) {
            submission.selectionState = EgaSubmission.SelectionState.SELECT_BAM_FILES
        }
        submission.save(flush: true)
    }

    @CompileDynamic
    List<DataFileAndSampleAlias> getDataFilesAndAlias(EgaSubmission submission) {
        List<DataFile> dataFiles
        if (submission.dataFilesToSubmit) {
            dataFiles = submission.dataFilesToSubmit*.dataFile
        } else {
            dataFiles = submission.samplesToSubmit.findAll { it.useFastqFile }.collectMany {
                SeqTrack.findAllBySampleAndSeqType(it.sample, it.seqType).collectMany {
                    seqTrackService.getSequenceFilesForSeqTrackIncludingWithdrawn(it)
                }
            }
        }

        Map<Sample, Map<SeqType, List<SampleSubmissionObject>>> submissionObjectsPerSampleAndSeqType = submission.samplesToSubmit.groupBy({ it.sample }, { it.seqType })
        return dataFiles.collect { DataFile file ->
            SampleSubmissionObject sampleSubmissionObject = exactlyOneElement(submissionObjectsPerSampleAndSeqType[file.seqTrack.sample][file.seqTrack.seqType])
            return new DataFileAndSampleAlias(file, sampleSubmissionObject)
        }.sort()
    }

    @CompileDynamic
    List<BamFileAndSampleAlias> getBamFilesAndAlias(EgaSubmission submission) {
        List<AbstractMergedBamFile> bamFiles
        if (submission.bamFilesToSubmit) {
            bamFiles = submission.bamFilesToSubmit*.bamFile
        } else {
            bamFiles = submission.samplesToSubmit.findAll { it.useBamFile }.collectMany {
                getAbstractMergedBamFiles(it)
            }
        }

        return bamFiles.collect { AbstractMergedBamFile file ->
            boolean producedByOtp = !(file instanceof ExternallyProcessedMergedBamFile)
            boolean withdrawn = file.withdrawn
            // For normal BAMs created by OTP, there is only one file and the decision was made on the previous page (disabled and checked)
            // Withdrawn BAMs can be submitted (enabled but unchecked by default) but the user should explicitly confirm that they want to submit "bad data"
            // Imported BAMs are not supported (disabled and unchecked), regardless of withdrawn state.
            new BamFileAndSampleAlias(
                    file,
                    submission.samplesToSubmit.find { it.sample == file.sample && it.seqType == file.seqType }.egaAliasName,
                    producedByOtp && withdrawn,
                    producedByOtp && !withdrawn,
                    producedByOtp,
            )
        }.sort()
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
            eq('fileOperationStatus', AbstractMergedBamFile.FileOperationStatus.PROCESSED)
        }.findAll {
            it.isMostRecentBamFile()
        }
    }

    @CompileDynamic
    void createDataFileSubmissionObjects(SelectFilesDataFilesFormSubmitCommand cmd) {
        EgaSubmission submission = cmd.submission
        cmd.selectBox.eachWithIndex { it, i ->
            if (it) {
                DataFileSubmissionObject dataFileSubmissionObject = new DataFileSubmissionObject(
                        dataFile: DataFile.get(cmd.fastqFile[i]),
                        sampleSubmissionObject: SampleSubmissionObject.get(cmd.egaSample[i])
                ).save(flush: false)
                submission.addToDataFilesToSubmit(dataFileSubmissionObject)
            }
        }
        submission.save(flush: true)
    }

    @SuppressWarnings('Instanceof')
    @CompileDynamic
    void createBamFileSubmissionObjects(EgaSubmission submission) {
        getBamFilesAndAlias(submission).each {
            if (!(it.bamFile instanceof ExternallyProcessedMergedBamFile)) {
                BamFileSubmissionObject bamFileSubmissionObject = new BamFileSubmissionObject(
                        bamFile: it.bamFile,
                        sampleSubmissionObject: SampleSubmissionObject.findByEgaAliasName(it.sampleAlias),
                ).save(flush: false)
                submission.addToBamFilesToSubmit(bamFileSubmissionObject)
            }
            submission.save(flush: true)
        }
    }

    @CompileDynamic
    List<SampleAndSeqTypeProjection> getSamplesWithSeqType(Project project) {
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
        }.unique().collect {
            return new SampleAndSeqTypeProjection(
                    sampleId:       it[0],
                    pid:            it[1],
                    sampleTypeName: it[2],
                    seqTypeId:      it[3].id,
                    seqTypeString:  it[3].toString(),
            )
        }
    }

    Map generateDefaultEgaAliasesForDataFiles(List<DataFileAndSampleAlias> dataFilesAndAliases) {
        Map<String, String> dataFileAliases = [:]

        dataFilesAndAliases.each {
            String runNameWithoutDate = it.dataFile.run.name.replaceAll("(?<!\\d)((?:20)?[0-2]\\d)-?(0\\d|1[012])-?([0-2]\\d|3[01])[-_]", "")
            List aliasNameHelper = [
                    it.dataFile.seqType.displayName,
                    it.dataFile.seqType.libraryLayout,
                    it.sampleSubmissionObject.egaAliasName,
                    it.dataFile.seqTrack.normalizedLibraryName,
                    runNameWithoutDate,
                    it.dataFile.seqTrack.laneId,
                    "R${it.dataFile.mateNumber}",
            ].findAll()
            String aliasName = "${aliasNameHelper.join("_")}.fastq.gz"
            dataFileAliases.put(it.dataFile.fileName + it.dataFile.run, aliasName)
        }

        return dataFileAliases
    }

    Map generateDefaultEgaAliasesForBamFiles(List<BamFileAndSampleAlias> bamFilesAndSampleAliases) {
        Map<String, String> bamFileAliases = [:]

        bamFilesAndSampleAliases.each {
            List aliasNameHelper = [
                    it.bamFile.seqType.displayName,
                    it.bamFile.seqType.libraryLayout,
                    it.sampleAlias,
                    it.bamFile.md5sum,
            ].findAll()
            String aliasName = "${aliasNameHelper.join("_")}.bam"
            bamFileAliases.put(it.bamFile.bamFileName + it.sampleAlias, aliasName)
        }

        return bamFileAliases
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
                property('libraryPreparationKit', 'libraryPreparationKit')
            }
        }.unique()
    }
}

@Canonical
class DataFileAndSampleAlias implements Comparable<DataFileAndSampleAlias> {
    DataFile dataFile
    SampleSubmissionObject sampleSubmissionObject

    @Override
    int compareTo(DataFileAndSampleAlias other) {
        return this.dataFile.individual.displayName <=> other.dataFile.individual.displayName ?:
                this.dataFile.seqType.toString() <=> other.dataFile.seqType.toString() ?:
                        this.dataFile.sampleType.displayName <=> other.dataFile.sampleType.displayName
    }

}

@Canonical
class BamFileAndSampleAlias implements Comparable<BamFileAndSampleAlias> {
    AbstractMergedBamFile bamFile
    String sampleAlias
    boolean selectionEditable
    boolean defaultSelectionState
    boolean producedByOtp

    @Override
    int compareTo(BamFileAndSampleAlias other) {
        return this.bamFile.individual.displayName <=> other.bamFile.individual.displayName ?:
                this.bamFile.seqType.toString() <=> other.bamFile.seqType.toString() ?:
                        this.bamFile.sampleType.displayName <=> other.bamFile.sampleType.displayName
    }
}

@Canonical
class SampleAndSeqTypeProjection implements Comparable<SampleAndSeqTypeProjection> {
    long sampleId
    String pid
    String sampleTypeName
    long seqTypeId
    String seqTypeString

    @Override
    int compareTo(SampleAndSeqTypeProjection other) {
        return this.pid <=> other.pid ?:
                this.seqTypeString <=> other.seqTypeString ?:
                        this.sampleTypeName <=> other.sampleTypeName
    }
}
