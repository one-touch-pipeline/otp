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
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@CompileStatic
@Transactional
class EgaSubmissionService {

    SeqTrackService seqTrackService

    static protected final String RAW_PREFIX = "UNMAPPED:"

    enum FileType {
        BAM,
        FASTQ,
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(params.project, 'OTP_READ_ACCESS')")
    EgaSubmission createSubmission(Map params) {
        EgaSubmission submission = new EgaSubmission(params + [
                state         : EgaSubmission.State.SELECTION,
                selectionState: EgaSubmission.SelectionState.SELECT_SAMPLES,
        ])
        assert submission.save(flush: true)

        return submission
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR') or returnObject == null or hasPermission(returnObject.project, 'OTP_READ_ACCESS')")
    EgaSubmission getEgaSubmission(Long id) {
        return EgaSubmission.get(id)
    }


    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateSubmissionState(EgaSubmission submission, EgaSubmission.State state) {
        submission.state = state
        submission.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updatePubMedId(EgaSubmission submission, String pubMedId) {
        submission.pubMedId = pubMedId
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

    void createAndSaveSampleSubmissionObjects(EgaSubmission submission, List<String> sampleIdSeqTypeIdList) {
        assert submission
        assert sampleIdSeqTypeIdList

        Project project = submission.project

        int size = sampleIdSeqTypeIdList.size()
        //use explicit collection classes to set the capacity (improve performance)
        List<SampleIdSeqTypeId> listOfPairOfSampleIdAndSeqTypeId = new ArrayList<SampleIdSeqTypeId>(size)
        Set<Long> sampleIds = new HashSet<Long>(size)
        Set<Long> seqTypeIds = new HashSet<Long>(15)

        sampleIdSeqTypeIdList.each {
            SampleIdSeqTypeId sampleIdSeqTypeId = new SampleIdSeqTypeId(it)
            sampleIds << sampleIdSeqTypeId.sampleId
            seqTypeIds << sampleIdSeqTypeId.seqTypeId
            listOfPairOfSampleIdAndSeqTypeId << sampleIdSeqTypeId
        }
        Map<Long, SeqType> seqTypeMap = fetchAndReturnSeqTypePerIdMap(seqTypeIds)
        Map<Long, Sample> sampleMap = fetchAndReturnSamplePerIdMap(sampleIds)
        List<SampleSubmissionObject> sampleSubmissionObjects = listOfPairOfSampleIdAndSeqTypeId.collect { SampleIdSeqTypeId sampleIdSeqTypeId ->
            Sample sample = sampleMap[sampleIdSeqTypeId.sampleId]
            SeqType seqType = seqTypeMap[sampleIdSeqTypeId.seqTypeId]
            assert sample: "no sample for ${sampleIdSeqTypeId.sampleId} could be found"
            assert seqType: "no seqType for ${sampleIdSeqTypeId.seqTypeId} could be found"
            assert sample.project == project
            new SampleSubmissionObject(
                    sample: sample,
                    seqType: seqType,
            ).save(flush: false)
        }
        submission.samplesToSubmit.addAll(sampleSubmissionObjects)
        submission.selectionState = EgaSubmission.SelectionState.SAMPLE_INFORMATION
        submission.save(flush: true)
    }

    @CompileDynamic
    private Map<Long, Sample> fetchAndReturnSamplePerIdMap(Set<Long> sampleIds) {
        return Sample.findAllByIdInList(sampleIds.toList()).collectEntries { Sample sample ->
            assert sample
            [(sample.id): sample]
        }
    }

    @CompileDynamic
    private Map<Long, SeqType> fetchAndReturnSeqTypePerIdMap(Set<Long> seqTypeIds) {
        return SeqType.findAllByIdInList(seqTypeIds.toList()).collectEntries { SeqType seqType ->
            assert seqType
            [(seqType.id): seqType]
        }
    }

    Map<SampleSubmissionObject, Boolean> checkFastqFiles(EgaSubmission egaSubmission) {
        Map<SampleSubmissionObject, Boolean> map = [:]

        String checkFastqFileExistQuery = """
            select distinct
                samplesToSubmit.id
            from
                EgaSubmission egaSubmission
                    join egaSubmission.samplesToSubmit samplesToSubmit,
                DataFile datafile
                    join datafile.seqTrack seqTrack
            where
                egaSubmission = :egaSubmission
                and samplesToSubmit.sample = seqTrack.sample
                and samplesToSubmit.seqType = seqTrack.seqType
                and datafile.fileExists = true
        """

        Set<Long> submissionSampleIdsWithFastqFiles = SampleSubmissionObject.executeQuery(checkFastqFileExistQuery, [
                egaSubmission: egaSubmission,
        ]).toSet()

        egaSubmission.samplesToSubmit.each {
            map.put(it, submissionSampleIdsWithFastqFiles.contains(it.id))
        }

        return map
    }

    Map<SampleSubmissionObject, Boolean> checkBamFiles(EgaSubmission egaSubmission) {
        Map<SampleSubmissionObject, Boolean> map = new HashMap<>(egaSubmission.samplesToSubmit.size())

        String checkBamFileExistQuery = """
            select
                samplesToSubmit.id,
                bamFile
            from
                EgaSubmission egaSubmission
                    join egaSubmission.samplesToSubmit samplesToSubmit,
                AbstractMergedBamFile bamFile
                    join fetch bamFile.workPackage workPackage
            where
                egaSubmission = :egaSubmission
                and workPackage.bamFileInProjectFolder = bamFile
                and samplesToSubmit.sample = workPackage.sample
                and samplesToSubmit.seqType = workPackage.seqType
                and bamFile.fileOperationStatus = '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'
        """

        Set<Long> submissionSampleIdsWithBamFiles = SampleSubmissionObject.executeQuery(checkBamFileExistQuery, [
                egaSubmission: egaSubmission,
        ]).findAll {
            ((it as List)[1] as AbstractMergedBamFile).mostRecentBamFile
        }.collect {
            (it as List)[0] as Long
        }.toSet()

        egaSubmission.samplesToSubmit.each {
            map.put(it, submissionSampleIdsWithBamFiles.contains(it.id))
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
            if (submission.samplesToSubmit.any { it.useFastqFile }) {
                submission.selectionState = EgaSubmission.SelectionState.SELECT_FASTQ_FILES
            } else {
                submission.selectionState = EgaSubmission.SelectionState.SELECT_BAM_FILES
            }
            submission.save(flush: true)
        }
    }

    List deleteSampleSubmissionObjects(EgaSubmission submission) {
        List samplesWithSeqType = []
        submission.samplesToSubmit*.seqType.unique()*.refresh()
        submission.samplesToSubmit.toArray().each { SampleSubmissionObject it ->
            submission.samplesToSubmit.remove(it)
            samplesWithSeqType.add("${it.sample.id}${it.seqType}")
            it.delete(flush: false)
        }
        submission.selectionState = EgaSubmission.SelectionState.SELECT_SAMPLES
        submission.save(flush: true)

        return samplesWithSeqType
    }

    void updateDataFileSubmissionObjects(SelectFilesDataFilesFormSubmitCommand cmd) {
        EgaSubmission submission = cmd.submission
        List<String> egaFileAlias = cmd.egaFileAlias
        Map<Long, DataFileSubmissionObject> dataFileSubmissionObjectMap = cmd.submission.dataFilesToSubmit.collectEntries {
            [(it.dataFile.id): it]
        }

        cmd.fastqFile.eachWithIndex { fastqIdString, i ->
            long fastqId = fastqIdString as long
            DataFileSubmissionObject dataFileSubmissionObject = dataFileSubmissionObjectMap[fastqId]
            dataFileSubmissionObject.egaAliasName = egaFileAlias[i]
            dataFileSubmissionObject.save(flush: false)
        }
        if (submission.samplesToSubmit.any { it.useBamFile }) {
            submission.selectionState = EgaSubmission.SelectionState.SELECT_BAM_FILES
        }
        submission.save(flush: true)
    }

    List<DataFileAndSampleAlias> getDataFilesAndAlias(EgaSubmission egaSubmission) {
        if (egaSubmission.dataFilesToSubmit) {
            return egaSubmission.dataFilesToSubmit.collect {
                return new DataFileAndSampleAlias(it.dataFile, it.sampleSubmissionObject)
            }.sort()
        }
        String queryFastqFiles = """
            select
                dataFile,
                samplesToSubmit
            from
                EgaSubmission egaSubmission
                    join egaSubmission.samplesToSubmit samplesToSubmit,
                DataFile dataFile
                    join dataFile.seqTrack seqTrack
            where
                egaSubmission = :egaSubmission
                and samplesToSubmit.sample = seqTrack.sample
                and samplesToSubmit.seqType = seqTrack.seqType
                and samplesToSubmit.useFastqFile = true
                and dataFile.fileExists = true
                and dataFile.fileType.type = '${de.dkfz.tbi.otp.ngsdata.FileType.Type.SEQUENCE}'
            """

        return DataFile.executeQuery(queryFastqFiles, [
                egaSubmission: egaSubmission,
        ]).collect {
            List list = it as List
            new DataFileAndSampleAlias(list[0] as DataFile, list[1] as SampleSubmissionObject)
        }.sort()
    }

    List<BamFileAndSampleAlias> getBamFilesAndAlias(EgaSubmission egaSubmission) {
        if (egaSubmission.bamFilesToSubmit) {
            return egaSubmission.bamFilesToSubmit.collect {
                new BamFileAndSampleAlias(it.bamFile, it.sampleSubmissionObject)
            }.sort()
        }
        String queryBamFile = """
            select
                bamFile,
                samplesToSubmit
            from
                EgaSubmission egaSubmission
                    join egaSubmission.samplesToSubmit samplesToSubmit,
                AbstractMergedBamFile bamFile
                    join fetch bamFile.workPackage workPackage
            where
                egaSubmission = :egaSubmission
                and workPackage.bamFileInProjectFolder = bamFile
                and samplesToSubmit.sample = workPackage.sample
                and samplesToSubmit.seqType = workPackage.seqType
                and samplesToSubmit.useBamFile = true
                and bamFile.fileOperationStatus = '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'
            """
        return SampleSubmissionObject.executeQuery(queryBamFile, [
                egaSubmission: egaSubmission,
        ]).findAll {
            ((it as List)[0] as AbstractMergedBamFile).mostRecentBamFile
        }.collect {
            List list = (it as List)
            new BamFileAndSampleAlias(list[0] as AbstractMergedBamFile, list[1] as SampleSubmissionObject)
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

    void createBamFileSubmissionObjects(EgaSubmission submission) {
        getBamFilesAndAlias(submission).findAll {
            it.producedByOtp && !it.bamFile.withdrawn
        }.each {
            BamFileSubmissionObject bamFileSubmissionObject = new BamFileSubmissionObject(
                    bamFile: it.bamFile,
                    sampleSubmissionObject: it.sampleSubmissionObject,
            ).save(flush: false)
            submission.addToBamFilesToSubmit(bamFileSubmissionObject)
        }
        submission.save(flush: true)
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
            SeqType seqType = it[3]
            return new SampleAndSeqTypeProjection(
                    sampleId: it[0],
                    pid: it[1],
                    sampleTypeName: it[2],
                    seqTypeId: seqType.id,
                    seqTypeString: seqType.toString(),
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

    @TupleConstructor
    @EqualsAndHashCode
    class ExperimentalDataRow {
        SeqType seqType
        SeqPlatformModelLabel seqPlatformModelLabel
        String seqPlatformName
        LibraryPreparationKit libraryPreparationKit

        ExperimentalDataRow (DataFileSubmissionObject dataFileSubmissionObject) {
            seqType = dataFileSubmissionObject.dataFile.seqType
            seqPlatformModelLabel = dataFileSubmissionObject.dataFile.run.seqPlatform.seqPlatformModelLabel
            seqPlatformName = dataFileSubmissionObject.dataFile.run.seqPlatform.name
            libraryPreparationKit = dataFileSubmissionObject.dataFile.seqTrack.libraryPreparationKit
        }

        ExperimentalDataRow (SeqTrack seqTrack) {
            seqType = seqTrack.seqType
            seqPlatformModelLabel = seqTrack.seqPlatform.seqPlatformModelLabel
            seqPlatformName = seqTrack.seqPlatform.name
            libraryPreparationKit = seqTrack.libraryPreparationKit
        }
    }

    @CompileDynamic
    List getExperimentalMetadata(EgaSubmission submission) {
        List metadata = []
        List<ExperimentalDataRow> experimentalDataRows = []

        submission.dataFilesToSubmit.each { DataFileSubmissionObject dataFileSubmissionObject ->
            experimentalDataRows << new ExperimentalDataRow(dataFileSubmissionObject)
        }

        submission.bamFilesToSubmit.each { BamFileSubmissionObject bamFileSubmissionObject ->
            bamFileSubmissionObject.bamFile.containedSeqTracks.each {
                experimentalDataRows << new ExperimentalDataRow(it)
            }
        }
        experimentalDataRows.unique().each { ExperimentalDataRow experimentalDataRow ->
            metadata << [
                    libraryLayout             : experimentalDataRow.seqType.libraryLayout,
                    displayName               : experimentalDataRow.seqType.displayName,
                    libraryPreparationKit     : experimentalDataRow.libraryPreparationKit,
                    mappedEgaPlatformModel    : mapEgaPlatformModel(experimentalDataRow.seqPlatformModelLabel),
                    mappedEgaLibrarySource    : mapEgaLibrarySource(experimentalDataRow.seqType),
                    mappedEgaLibraryStrategy  : mapEgaLibraryStrategy(experimentalDataRow.seqType),
                    mappedEgaLibrarySelection : mapEgaLibrarySelection(experimentalDataRow.libraryPreparationKit),
            ]
        }
        return metadata
    }

    Map generateDefaultEgaAliasesForBamFiles(List<BamFileAndSampleAlias> bamFilesAndSampleAliases) {
        Map<String, String> bamFileAliases = [:]

        bamFilesAndSampleAliases.each {
            List aliasNameHelper = [
                    it.bamFile.seqType.displayName,
                    it.bamFile.seqType.libraryLayout,
                    it.sampleSubmissionObject.egaAliasName,
                    it.bamFile.md5sum,
            ].findAll()
            String aliasName = "${aliasNameHelper.join("_")}.bam"
            bamFileAliases.put(it.bamFile.bamFileName + it.sampleSubmissionObject.egaAliasName, aliasName)
        }

        return bamFileAliases
    }

    @CompileDynamic
    String mapEgaPlatformModel(SeqPlatformModelLabel seqPlatformModelLabel) {
        if (!seqPlatformModelLabel) {
            return 'TODO'
        }
        return EgaPlatformModel.createCriteria().get {
            seqPlatformModelLabels {
                'in'('id', seqPlatformModelLabel.id)
            }
        }?.platformModelEgaName ?: "${RAW_PREFIX} ${seqPlatformModelLabel.name}"
    }

    @CompileDynamic
    String mapEgaLibrarySelection(LibraryPreparationKit libraryPreparationKit) {
        if (!libraryPreparationKit) {
            return 'unspecified'
        }
        return EgaLibrarySelection.createCriteria().get {
            libraryPreparationKits {
                'in'('id', libraryPreparationKit.id)
            }
        }?.librarySelectionEgaName ?: "${RAW_PREFIX} ${libraryPreparationKit.name}"
    }

    @CompileDynamic
    String mapEgaLibrarySource(SeqType seqType) {
        return EgaLibrarySource.createCriteria().get {
            seqTypes {
                'in'('id', seqType.id)
            }
        }?.librarySourceEgaName ?: "${RAW_PREFIX} ${seqType.displayName}"
    }

    @CompileDynamic
    String mapEgaLibraryStrategy(SeqType seqType) {
        return EgaLibraryStrategy.createCriteria().get {
            seqTypes {
                'in'('id', seqType.id)
            }
        }?.libraryStrategyEgaName ?: "${RAW_PREFIX} ${seqType.displayName}"
    }
}

@CompileStatic
@Canonical
class DataFileAndSampleAlias implements Comparable<DataFileAndSampleAlias> {

    DataFile dataFile
    SampleSubmissionObject sampleSubmissionObject

    @Override
    int compareTo(DataFileAndSampleAlias other) {
        return this.dataFile.individual.displayName <=> other.dataFile.individual.displayName ?:
                this.dataFile.seqType.toString() <=> other.dataFile.seqType.toString() ?:
                        this.dataFile.sampleType.displayName <=> other.dataFile.sampleType.displayName ?:
                                this.dataFile.seqTrack.run.name <=> other.dataFile.seqTrack.run.name ?:
                                        this.dataFile.seqTrack.laneId <=> other.dataFile.seqTrack.laneId ?:
                                                this.dataFile.mateNumber <=> other.dataFile.mateNumber
    }
}

@CompileStatic
class BamFileAndSampleAlias implements Comparable<BamFileAndSampleAlias> {
    final AbstractMergedBamFile bamFile
    final SampleSubmissionObject sampleSubmissionObject

    final boolean selectionEditable
    final boolean defaultSelectionState
    final boolean producedByOtp

    @SuppressWarnings('Instanceof')
    BamFileAndSampleAlias(AbstractMergedBamFile bamFile, SampleSubmissionObject sampleSubmissionObject) {
        this.bamFile = bamFile
        this.sampleSubmissionObject = sampleSubmissionObject

        // For normal BAMs created by OTP, there is only one file and the decision was made on the previous page (disabled and checked)
        // Withdrawn BAMs can not be submitted (disabled and unchecked)
        // Imported BAMs are not supported (disabled and unchecked), regardless of withdrawn state.
        producedByOtp = !(bamFile instanceof ExternallyProcessedMergedBamFile)
        selectionEditable = false //Currently editable checkboxes are not supported
        defaultSelectionState = producedByOtp && !bamFile.withdrawn
    }

    @Override
    int compareTo(BamFileAndSampleAlias other) {
        return this.bamFile.individual.displayName <=> other.bamFile.individual.displayName ?:
                this.bamFile.seqType.toString() <=> other.bamFile.seqType.toString() ?:
                        this.bamFile.sampleType.displayName <=> other.bamFile.sampleType.displayName
    }
}

@CompileStatic
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
