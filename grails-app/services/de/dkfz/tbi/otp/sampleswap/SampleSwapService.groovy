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
package de.dkfz.tbi.otp.sampleswap

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Files

@Transactional
class SampleSwapService {

    SpringSecurityService springSecurityService
    SampleIdentifierService sampleIdentifierService
    LsdfFilesService lsdfFilesService


    Map getPropertiesForSampleSwap(SeqTrack seqTrack) {
        assert seqTrack
        Map dataFiles = [:]
        DataFile.findAllBySeqTrack(seqTrack, [sort: "fileName", order: "asc"]).each {
            dataFiles.put((it.id), it.fileName)
        }

        return [
                project       : seqTrack.sample.project.name,
                pid           : seqTrack.sample.individual.pid,
                sampleType    : seqTrack.sample.sampleType.displayName,
                seqType       : seqTrack.seqType.displayName,
                libPrepKit    : seqTrack.libraryPreparationKit?.name ?: "",
                antibodyTarget: seqTrack.antibodyTarget?.name ?: "",
                antibody      : seqTrack.antibody ?: "",
                libraryLayout : seqTrack.seqType.libraryLayout,
                run           : seqTrack.run.name,
                lane          : seqTrack.laneId,
                ilse          : seqTrack.ilseId ? Integer.toString(seqTrack.ilseId) : "",
                files         : dataFiles,
        ]
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map validateInput(Map data) {
        data.data.each { SampleSwapData it ->
            it.oldValues.each { k, v ->
                if (k != 'files') {
                    if (it.newValues.get(k) != v) {
                        it.sampleSwapInfos << new SampleSwapInfo(v, it.newValues.get(k), it.seqTrackId, k, "Column '${k}' of '${it.rowNumber}' changed from " +
                                "'${v}' to '${it.newValues.get(k)}'.", SampleSwapLevel.CHANGE)
                        if (k == "project" && it.oldValues.pid == it.newValues.pid) {
                            it.sampleSwapInfos << new SampleSwapInfo(it.oldValues.pid, it.newValues.pid, it.seqTrackId,
                                    "pid", "Column 'pid' of '${it.rowNumber}' didn't change from '"
                                    + it.oldValues.pid + "', but '${k}' changed from '${v}' to '${it.newValues.get(k)}'.", SampleSwapLevel.INFO)
                        }
                    }
                } else {
                    Map oldValues = it.oldValues.get(k)
                    it.newValues.get(k).eachWithIndex { key, value, index ->
                        if (value != oldValues.get(key)) {
                            it.sampleSwapInfos << new SampleSwapInfo(oldValues.get(key), value, it.seqTrackId, k,
                                    "Column 'dataFile ${index + 1}' of '${it.rowNumber}' changed from '${oldValues.get(key)}' to '${value}'.",
                                    SampleSwapLevel.CHANGE, index + 1)
                        }
                    }
                }
            }
            it.sampleSwapInfos += validatePid(it)
            it.sampleSwapInfos += validateSampleType(it)
            it.sampleSwapInfos += validateSeqType(it)
            it.sampleSwapInfos += validateAntibodyTarget(it)
            it.sampleSwapInfos += validateAntibody(it)
            it.sampleSwapInfos += validateLibPrepKit(it)
            it.sampleSwapInfos += validateFiles(it)
            if (it.sampleSwapInfos.size() > 0) {
                it.sampleSwapInfos += checkFastq(it)
            }
            it.sampleSwapInfos.sort { a, b ->
                b.level.getValue() <=> a.level.getValue() ?: a.key <=> b.key
            }
        }

        data.data << new SampleSwapData(validateProject(data) + validateSample(data) + checkAnalysis(data) +
                (data.comment == "" ? [new SampleSwapInfo('', '', '', 'comment',
                        'Comment is not allowed to be empty.', SampleSwapLevel.ERROR)] : []))

        return data
    }

    @SuppressWarnings('DeadCode')
    void doSwap(Map data) {
        throw new UnsupportedOperationException("This Method is not yet implemented")
        SwapInfo swapInfo = new SwapInfo(user: springSecurityService.getCurrentUser(), comment: data.comment, descriptionOfChanges: "")
        data.data.each { SampleSwapData it ->
            swapInfo.descriptionOfChanges += it.getSampleSwapInfosAsString()
            assert swapInfo.addToSeqTracks(SeqTrack.get(it.seqTrackId)).save(flush: true)
        }
    }

    private List checkAnalysis(Map data) {
        List sampleSwapInfoData = []
        List rows = []
        List seqTrackIds = []
        data.data.each {
            if (it.sampleSwapInfos.size() > 0) {
                SeqTrack seqTrack = SeqTrack.get(it.seqTrackId)
                List<RoddyBamFile> roddyBamFiles = RoddyBamFile.createCriteria().list {
                    seqTracks {
                        eq("id", seqTrack.id)
                    }
                }
                if (roddyBamFiles && BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(roddyBamFiles, roddyBamFiles)) {
                    seqTrackIds << seqTrack.id
                    rows << it.rowNumber
                }
            }
        }

        if (rows) {
            sampleSwapInfoData << new SampleSwapInfo('', '', seqTrackIds.join(","), 'analysis',
                    "Analysis for rows '${rows.join("', '")}' will be deleted.", SampleSwapLevel.INFO)
        }
        return sampleSwapInfoData
    }

    private List checkFastq(SampleSwapData data) {
        List output = []
        data.newValues.files.eachWithIndex { k, v, index ->
            DataFile dataFile = DataFile.get(k)
            if (dataFile) {
                File file = new File(lsdfFilesService.getFileFinalPath(dataFile))
                if (file.exists()) {
                    if (Files.isSymbolicLink(file.toPath())) {
                        SeqType seqType = SeqType.findByDisplayNameAndLibraryLayout(data.newValues.seqType, data.newValues.libraryLayout)
                        Project project = Project.findByName(data.newValues.project)
                        Pipeline pipeline = Pipeline.Name.forSeqType(seqType)?.getPipeline()
                        if (seqType && project && pipeline) {
                            RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
                            if (SeqTypeService.getAllAlignableSeqTypes().contains(seqType) &&
                                    project.alignmentDeciderBeanName != AlignmentDeciderBeanName.NO_ALIGNMENT && config) {
                                output << new SampleSwapInfo(data.oldValues.files.get(k), v, data.seqTrackId, 'files',
                                        "'datafile ${index + 1}' with value '${v}' will be realigned and is linked and not copied, " +
                                                "therefore make sure that it is not deleted by the GPCF.", SampleSwapLevel.WARNING, index + 1)
                            } else {
                                output << new SampleSwapInfo(data.oldValues.files.get(k), v, data.seqTrackId, 'files',
                                        "'datafile ${index + 1}' with value '${v}' will not be realigned and is linked and not copied, " +
                                                "therefore the file has to be copied manually.", SampleSwapLevel.ERROR, index + 1)
                            }
                        } else {
                            output << new SampleSwapInfo(data.oldValues.files.get(k), v, data.seqTrackId, 'files',
                                    "'datafile ${index + 1}' with value '${v}' will not be realigned and is linked and not copied, " +
                                            "therefore the file has to be copied manually.", SampleSwapLevel.ERROR, index + 1)
                        }
                    }
                } else {
                    output << new SampleSwapInfo(data.oldValues.files.get(k), v, data.seqTrackId, 'files',
                            "'datafile ${index + 1}' with value '${v}' does not exists.", SampleSwapLevel.ERROR, index + 1)
                }
            }
        }
        return output
    }

    private List validateProject(Map data) {
        List sampleSwapInfoData = []
        boolean individualDeleted = false

        Map changedData = data.data.findAll { it.findByKey("project") || it.findByKey("pid") }.groupBy {
            it.newValues.pid
        }
        changedData.each { k, v ->
            if (k != "") {
                if (v.unique(false) { it.newValues.project }.size() == 1) {
                    if (v.first().oldValues.pid == v.first().newValues.pid) {
                        if (v.size() == data.data.size()) {
                            sampleSwapInfoData << new SampleSwapInfo('', '', '', 'individual',
                                    "'individual' '${k}' will be moved from 'project' '${v.first().oldValues.project}' to '${v.first().newValues.project}'.",
                                    SampleSwapLevel.INFO)
                            v.each { SampleSwapData sampleSwapData ->
                                sampleSwapData.removeSampleSwapInfos("project", [SampleSwapLevel.INFO, SampleSwapLevel.ERROR])
                            }
                        }
                    } else {
                        Project project = Project.findByName(v.first().newValues.project)
                        Individual individual = Individual.findByPid(k)
                        if (changedData.get(data.individual.pid) == null && data.data.findAll {
                            it.findByKey("project") || it.findByKey("pid")
                        }.size() == data.data.size()) {
                            if (v.size() == data.data.size() && !individual) {
                                sampleSwapInfoData << new SampleSwapInfo('', '', '', 'individual',
                                        "'individual' 'pid' will be changed from '${v.first().oldValues.pid}' to '${k}'.", SampleSwapLevel.INFO)
                                v.each { SampleSwapData sampleSwapData ->
                                    sampleSwapData.removeSampleSwapInfos("pid", [SampleSwapLevel.INFO, SampleSwapLevel.ERROR])
                                }
                                return
                            }
                            individualDeleted = true
                        }

                        if (individual?.project != project) {
                            sampleSwapInfoData << new SampleSwapInfo('', '', '', 'individual',
                                    "'individual' '${k}' will be created in 'project' '${v.first().newValues.project}'.", SampleSwapLevel.INFO)
                        }
                    }
                } else {
                    sampleSwapInfoData << new SampleSwapInfo(v.first().oldValues.project, v.first().newValues.project, '', 'individual',
                            "You are trying to move the 'individual' '${k}' into multiple projects, this is not allowed. Corresponding rows: " +
                                    "'${(v*.rowNumber).join(", ")}'.", SampleSwapLevel.ERROR)
                    v.each { SampleSwapData sampleSwapData ->
                        sampleSwapData.removeSampleSwapInfos("project", [SampleSwapLevel.INFO, SampleSwapLevel.ERROR])
                        sampleSwapData.removeSampleSwapInfos("pid", [SampleSwapLevel.INFO, SampleSwapLevel.ERROR])
                        sampleSwapData.sampleSwapInfos << new SampleSwapInfo(sampleSwapData.oldValues.project, sampleSwapData.oldValues.project,
                                sampleSwapData.seqTrackId, 'project', '', SampleSwapLevel.ERROR)
                        sampleSwapData.sampleSwapInfos << new SampleSwapInfo(sampleSwapData.oldValues.pid, sampleSwapData.oldValues.pid,
                                sampleSwapData.seqTrackId, 'pid', '', SampleSwapLevel.ERROR)
                    }
                }
            }
        }
        if (individualDeleted) {
            sampleSwapInfoData << new SampleSwapInfo('', '', '', 'individual',
                    "'individual' '${data.individual.pid}' will be deleted.", SampleSwapLevel.INFO)
        }
        return sampleSwapInfoData.sort { a, b ->
            b.level.getValue() <=> a.level.getValue() ?: a.key <=> b.key
        }
    }

    private List validatePid(SampleSwapData data) {
        List output = []
        data.findByKey("pid").each { SampleSwapInfo it ->
            Project project = Project.findByName(data.newValues.project)
            if (it.newValue == "") {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'pid',
                        "Column 'pid' of '${data.rowNumber}' can't be empty.", SampleSwapLevel.ERROR)
            } else if (project &&
                    project.sampleIdentifierParserBeanName != SampleIdentifierParserBeanName.NO_PARSER &&
                    !sampleIdentifierService.getSampleIdentifierParser(project.sampleIdentifierParserBeanName).tryParse(it.newValue)) {
                if (it.oldValue == it.newValue) {
                    output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'pid',
                            "Column 'pid' of '${data.rowNumber}' didn't change from '${it.oldValue}', but 'project' is '${data.newValues.project}' " +
                                    "and the 'pid' doesn't conform to the naming scheme conventions.", SampleSwapLevel.WARNING)
                } else {
                    output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'pid',
                            "Column 'pid' of '${data.rowNumber}' can't change from '${it.oldValue}' to '${it.newValue}' because 'project' is " +
                                    "'${data.newValues.project}' and the 'pid' doesn't conform to the naming scheme conventions.", SampleSwapLevel.WARNING)
                }
            }
            if (hasExternallyProcessedMergedBamFiles(SeqTrack.get(it.seqTrackId).individual)) {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'pid', "Column 'pid' of '${data.rowNumber}' with value " +
                        "'${it.oldValue}' can't be changed because there are 'ExternallyProcessedMergedBamFiles' registered in the OTP database.",
                        SampleSwapLevel.ERROR)
            }
            Individual individual = Individual.findByPid(it.newValue)
            if (individual) {
                if (individual.project.name != data.newValues.project) {
                    if (data.oldValues.project != data.newValues.project) {
                        output << new SampleSwapInfo(individual.project.name, data.newValues.project, it.seqTrackId, 'project',
                                "Column 'project' of '${data.rowNumber}' can't change from '${individual.project.name}' to " +
                                        "'${data.newValues.project}' because pid '${it.newValue}' already exists in project '${individual.project.name}'.",
                                SampleSwapLevel.ERROR)
                    }
                    if (it.oldValue != it.newValue) {
                        output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'pid', "Column 'pid' of '${data.rowNumber}' " +
                                "can't change from '${it.oldValue}' to '${it.newValue}' because '${it.newValue}' already exists in 'project' " +
                                "'${individual.project.name}'.", SampleSwapLevel.ERROR)
                    }
                }
            }
            data.newValues.files.eachWithIndex { k, v, index ->
                if (data.oldValues.files.get(k).startsWith(data.oldValues.pid)) {
                    if (!v.startsWith(data.newValues.pid)) {
                        output << new SampleSwapInfo(data.oldValues.files.get(k), v, it.seqTrackId, 'files', "Column 'datafile ${index + 1}' " +
                                "of '${data.rowNumber}' with value '${v}' has to start with '${data.newValues.pid}'.", SampleSwapLevel.ERROR, index + 1)
                        output << new SampleSwapInfo(data.oldValues.pid, data.newValues.pid, it.seqTrackId, 'pid', "", SampleSwapLevel.ERROR)
                    }
                }
            }
        }
        return output
    }

    private boolean hasExternallyProcessedMergedBamFiles(Individual individual) {
        return ExternallyProcessedMergedBamFile.createCriteria().list {
            workPackage {
                sample {
                    eq('individual', individual)
                }
            }
        }
    }

    private List validateSampleType(SampleSwapData data) {
        List output = []
        data.findByKey("sampleType").each { SampleSwapInfo it ->
            SampleType sampleType = SampleType.findByName(it.newValue)
            if (!sampleType) {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'sampleType', "Column 'sampleType' of '${data.rowNumber}' " +
                        "with value '${it.newValue}' is not registered in the OTP database.", SampleSwapLevel.ERROR)
            }
        }
        return output
    }

    private List validateSample(Map data) {
        List output = []
        Map changedData = data.data.findAll { it.findByKey("sampleType") || it.findByKey("pid") }.groupBy {
            [it.newValues.pid, it.newValues.sampleType]
        }
        changedData.each { k, v ->
            Individual individual = Individual.findByPid(k[0])
            SampleType sampleType = SampleType.findByName(k[1])
            if (sampleType) {
                if (individual) {
                    Sample sample = Sample.findByIndividualAndSampleType(individual, sampleType)
                    if (!sample) {
                        output << new SampleSwapInfo('', '', v.first().seqTrackId, 'sample', "'sample' '${individual.pid} " +
                                "${sampleType.displayName}' will be created", SampleSwapLevel.INFO)
                    }
                } else {
                    output << new SampleSwapInfo('', '', v.first().seqTrackId, 'sample', "'sample' '${k[0]} " +
                            "${sampleType.displayName}' will be created", SampleSwapLevel.INFO)
                }
            }
        }
        return output
    }

    private List validateSeqType(SampleSwapData data) {
        List output = []
        data.findByKey("seqType").each { SampleSwapInfo it ->
            SeqType seqType = SeqType.findByDisplayName(it.newValue)
            if (!seqType) {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'seqType', "Column 'seqType' of '${data.rowNumber}' with value " +
                        "'${it.newValue}' is not registered in the OTP database.", SampleSwapLevel.ERROR)
            }

            if (it.oldValue == "ChIP" && it.newValue != "ChIP" && data.oldValues.antibodyTarget == data.newValues.antibodyTarget &&
                    data.newValues.antibodyTarget != '') {
                output << new SampleSwapInfo(data.oldValues.antibodyTarget, data.newValues.antibodyTarget, it.seqTrackId, 'antibodyTarget',
                        "Column 'antibodyTarget' of '${data.rowNumber}' will be set from '${data.newValues.antibodyTarget}' to '' because 'seqType' " +
                                "was set from '${it.oldValue}' to '${it.newValue}'.", SampleSwapLevel.WARNING)
            }
            if (it.oldValue == "ChIP" && it.newValue != "ChIP" && data.oldValues.antibody == data.newValues.antibody && data.newValues.antibody != '') {
                output << new SampleSwapInfo(data.oldValues.antibody, data.newValues.antibody, it.seqTrackId, 'antibody', "Column 'antibody' of " +
                        "'${data.rowNumber}' will be set from '${data.newValues.antibody}' to '' because 'seqType' was set from '${it.oldValue}' to " +
                        "'${it.newValue}'.", SampleSwapLevel.WARNING)
            }

            if (it.newValue == "ChIP" && data.newValues.antibodyTarget == '') {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'seqType', "Column 'antibodyTarget' of '${data.rowNumber}'" +
                        " must be filled in because 'seqType' was set from '${it.oldValue}' to '${it.newValue}'.", SampleSwapLevel.ERROR)
                output << new SampleSwapInfo(data.oldValues.antibodyTarget, data.oldValues.antibodyTarget, it.seqTrackId, 'antibodyTarget',
                        "", SampleSwapLevel.ERROR)
            }
            if ((it.newValue == "ChIP" || it.newValue == "EXOME" || it.newValue == "WGBS" || it.newValue == "WGBS_TAG" || it.newValue == "RNA" ||
                    it.newValue == "RNA_TAG") && data.newValues.libPrepKit == '') {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'seqType', "Column 'libPrepKit' of '${data.rowNumber}'" +
                        " must be filled in because 'seqType' was set from '${it.oldValue}' to '${it.newValue}'.", SampleSwapLevel.ERROR)
                output << new SampleSwapInfo(data.oldValues.libPrepKit, data.newValues.libPrepKit, it.seqTrackId, 'libPrepKit',
                        "", SampleSwapLevel.ERROR)
            }
        }
        return output
    }

    private List validateAntibodyTarget(SampleSwapData data) {
        List output = []
        data.findByKey("antibodyTarget").each { SampleSwapInfo it ->
            AntibodyTarget antibodyTarget = AntibodyTarget.findByName(it.newValue)
            if (!antibodyTarget && it.newValue != "") {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'antibodyTarget', "Column 'antibodyTarget' of " +
                        "'${data.rowNumber}' with value '${it.newValue}' is not registered in the OTP database.", SampleSwapLevel.ERROR)
            }
            if (it.level == SampleSwapLevel.CHANGE) {
                if (it.newValue != "" && data.newValues.seqType != "ChIP") {
                    output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'antibodyTarget', "Column 'antibodyTarget' of " +
                            "'${data.rowNumber}' can't change from '${it.oldValue}' to '${it.newValue}' because 'seqType' is '${data.newValues.seqType}' " +
                            "and not 'ChIP'.", SampleSwapLevel.WARNING)
                } else if (it.newValue == "" && data.newValues.seqType == "ChIP" && data.findByKey("seqType").size() == 0) {
                    output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'antibodyTarget', "Column 'antibodyTarget' of " +
                            "'${data.rowNumber}' must be filled in because 'seqType' is 'ChIP'.", SampleSwapLevel.ERROR)
                }
            }
        }
        return output
    }

    private List validateAntibody(SampleSwapData data) {
        List output = []
        data.findByKey("antibody").each { SampleSwapInfo it ->
            if (it.level == SampleSwapLevel.CHANGE && it.newValue != "" && data.newValues.seqType != "ChIP") {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'antibody', "Column 'antibody' of '${data.rowNumber}' " +
                        "can't change from '${it.oldValue}' to '${it.newValue}' because 'seqType' is '${data.newValues.seqType}' and not 'ChIP'.",
                        SampleSwapLevel.WARNING)
            }
        }
        return output
    }

    private List validateLibPrepKit(SampleSwapData data) {
        List output = []
        data.findByKey("libPrepKit").each { SampleSwapInfo it ->
            LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.findByName(it.newValue)
            if (!libraryPreparationKit && it.newValue != "") {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'libPrepKit', "Column 'libPrepKit' of '${data.rowNumber}' " +
                        "with value '${it.newValue}' is not registered in the OTP database.", SampleSwapLevel.ERROR)
            }
            if (it.level == SampleSwapLevel.CHANGE && it.newValue == "" && (data.newValues.seqType == "ChIP" || data.newValues.seqType == "EXOME" ||
                    data.newValues.seqType == "WGBS" || data.newValues.seqType == "WGBS_TAG" || data.newValues.seqType == "RNA" ||
                    data.newValues.seqType == "RNA_TAG") && data.findByKey("seqType").size() == 0) {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'libPrepKit', "Column 'libPrepKit' of '${data.rowNumber}' " +
                        "must be filled in because 'seqType' is '${data.newValues.seqType}'.", SampleSwapLevel.ERROR)
            }
        }
        return output
    }

    private List validateFiles(SampleSwapData data) {
        List output = []
        data.findByKey("files").each { SampleSwapInfo it ->
            if (data.findByKey("pid").size() == 0) {
                if (it.oldValue.startsWith(data.oldValues.pid)) {
                    if (!it.newValue.startsWith(data.newValues.pid)) {
                        output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'files', "Column 'datafile ${it.index}' of " +
                                "'${data.rowNumber}' with value '${it.newValue}' has to start with '${data.newValues.pid}'.", SampleSwapLevel.ERROR, it.index)
                    }
                }
            }
        }
        return output
    }
}

enum SampleSwapLevel {
    ERROR(1000),
    WARNING(500),
    INFO(100),
    CHANGE(50),
    NOLEVEL(0),

    private final int id

    SampleSwapLevel(int id) { this.id = id; }

    int getValue() { return id; }
}

class SampleSwapInfo {
    String oldValue
    String newValue
    String seqTrackId
    String key
    String description
    int index

    SampleSwapLevel level

    SampleSwapInfo(String oldValue, String newValue, String seqTrackId, String key, String description, SampleSwapLevel level, int index = -1) {
        this.oldValue = oldValue
        this.newValue = newValue
        this.seqTrackId = seqTrackId
        this.key = key
        this.description = description
        this.level = level
        this.index = index
    }
}

class SampleSwapData {
    Map oldValues
    Map newValues
    String seqTrackId
    int rowNumber
    List<SampleSwapInfo> sampleSwapInfos = []

    SampleSwapData(Map oldValues, Map newValues, String seqTrackId, int rowNumber) {
        this.oldValues = oldValues
        this.newValues = newValues
        this.seqTrackId = seqTrackId
        this.rowNumber = rowNumber
    }

    SampleSwapData(List<SampleSwapInfo> sampleSwapInfos) {
        this.sampleSwapInfos = sampleSwapInfos
    }

    SampleSwapLevel getNormalizedLevel(String key, String fileId = "") {
        return findByKey(key, fileId).max { it.level.value }?.level ?: SampleSwapLevel.NOLEVEL
    }

    List<SampleSwapInfo> findByKey(String key, String fileId = "") {
        return sampleSwapInfos.findAll {
            it.key == key && ((key == "files" && fileId != "") ? it.newValue == fileId : true)
        }
    }

    String getSampleSwapInfosAsString() {
        return sampleSwapInfos*.description.join(", ")
    }

    void removeSampleSwapInfos(String key, List<SampleSwapLevel> levels) {
        sampleSwapInfos.removeAll {
            it.key == key && levels.contains(it.level) && !it.description.contains("ExternallyProcessedMergedBamFiles")
        }
    }
}
