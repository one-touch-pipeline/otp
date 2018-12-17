package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.security.access.prepost.*

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
        submission.addToSamplesToSubmit(sampleSubmissionObject)
        submission.selectionState = EgaSubmission.SelectionState.SAMPLE_INFORMATION
        submission.save(flush: true)
    }

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
        Map<SampleSubmissionObject, Boolean> map = [:]

        submission.samplesToSubmit.each {
           map.put(it, !getAbstractMergedBamFiles(it).empty)
        }

        return map
    }

    void updateSampleSubmissionObjects(EgaSubmission submission, List<String> sampleObjectId, List<String> alias, List<FileType> fileType) {
        if (sampleObjectId.size() == alias.size() && sampleObjectId.size() == fileType.size()) {
            sampleObjectId.eachWithIndex { it, i ->
                SampleSubmissionObject sampleSubmissionObject = SampleSubmissionObject.findById(it as Long)
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

    List getDataFilesAndAlias(EgaSubmission submission) {
        if (submission.dataFilesToSubmit) {
            return submission.dataFilesToSubmit.collect {
                [it.dataFile, SampleSubmissionObject.findBySampleAndSeqType(it.dataFile.seqTrack.sample, it.dataFile.seqType).egaAliasName]
            }.sort { it[1] }
        } else {
            return submission.samplesToSubmit.findAll { it.useFastqFile }.collectMany {
                seqTrackService.getSequenceFilesForSeqTrack(SeqTrack.findBySampleAndSeqType(it.sample, it.seqType))
            }.collect {
                [it, SampleSubmissionObject.findBySampleAndSeqType(it.seqTrack.sample, it.seqType).egaAliasName]
            }.sort { it[1] }
        }
    }

    List getBamFilesAndAlias(EgaSubmission submission) {
        if (submission.bamFilesToSubmit) {
            return submission.bamFilesToSubmit.collect {
                [it.bamFile, SampleSubmissionObject.findBySampleAndSeqType(it.bamFile.sample, it.bamFile.seqType).egaAliasName]
            }.sort { it[1] }
        } else {
            submission.samplesToSubmit.findAll { it.useBamFile }.collectMany { sampleSubmissionObject ->
                getAbstractMergedBamFiles(sampleSubmissionObject)
            }.collect {
                [it, SampleSubmissionObject.findBySampleAndSeqType(it.sample, it.seqType).egaAliasName]
            }.sort { it[1] }
        }
    }

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

    void createBamFileSubmissionObjects(EgaSubmission submission, List<String> fileId, List<String> egaFileAliases, List<String> egaSampleAliases) {
        egaFileAliases.eachWithIndex { it, i ->
            if (it) {
                BamFileSubmissionObject bamFileSubmissionObject = new BamFileSubmissionObject(
                        bamFile: AbstractMergedBamFile.findById(fileId[i] as Long),
                        sampleSubmissionObject: SampleSubmissionObject.findByEgaAliasName(egaSampleAliases[i]),
                        egaAliasName: it
                ).save(flush: true)
                submission.addToBamFilesToSubmit(bamFileSubmissionObject)
            }
        }
    }

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
}
