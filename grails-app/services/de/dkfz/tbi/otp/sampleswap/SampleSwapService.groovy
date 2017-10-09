package de.dkfz.tbi.otp.sampleswap

import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.*

class SampleSwapService {

    SpringSecurityService springSecurityService

    Map getPropertiesForSampleSwap(SeqTrack seqTrack) {
        assert seqTrack
        Map dataFiles = [:]
        DataFile.findAllBySeqTrack(seqTrack, [sort: "fileName", order: "asc"]).each {
            dataFiles.put("${it.id}", it.fileName)
        }

        return [
                project       : seqTrack.sample.project.name,
                pid           : seqTrack.sample.individual.pid,
                sampleType    : seqTrack.sample.sampleType.displayName,
                seqType       : seqTrack.seqType.displayName,
                libPrepKit    : seqTrack.libraryPreparationKit?.name ?: "",
                antibodyTarget: seqTrack instanceof ChipSeqSeqTrack ? seqTrack.antibodyTarget.name : "",
                antibody      : seqTrack instanceof ChipSeqSeqTrack && seqTrack.antibody ? seqTrack.antibody : "",
                libraryLayout : seqTrack.seqType.libraryLayout,
                run           : seqTrack.run.name,
                lane          : seqTrack.laneId,
                ilse          : seqTrack.ilseId ? Integer.toString(seqTrack.ilseId) : "",
                files         : dataFiles,
        ]
    }

    Map validateInput(Map data) {
        data.data.each { SampleSwapData it ->
            it.oldValues.each { k, v ->
                if (k != 'files') {
                    if (it.newValues."${k}" != v) {
                        it.sampleSwapInfos << new SampleSwapInfo(v, it.newValues."${k}", it.seqTrackId, k, "Value '${k}' of '${it.rowNumber}' changed from '${v}' to '" + it.newValues."${k}" + "'.", SampleSwapLevel.INFO)
                    }
                } else {
                    Map oldValues = it.oldValues."${k}"
                    it.newValues."${k}".each { key, value ->
                        if (value != oldValues.get(key)) {
                            it.sampleSwapInfos << new SampleSwapInfo(oldValues.get(key), value, it.seqTrackId, k, "Value 'datafile' of '${it.rowNumber}' changed from '${oldValues.get(key)}' to '${value}'.", SampleSwapLevel.INFO)
                        }
                    }
                }
            }
            it.sampleSwapInfos += validateAntibodyTarget(it)
            it.sampleSwapInfos += validateAntibody(it)
            it.sampleSwapInfos += validateSeqType(it)
            it.sampleSwapInfos.sort { a, b ->
                a.key <=> b.key ?: a.level.getValue() <=> b.level.getValue()
            }

        }
        if (data.comment == "") {
            data.data << new SampleSwapData(new SampleSwapInfo('', '', '', 'comment', 'Comment is not allowed to be empty', SampleSwapLevel.ERROR))
        }

        return data
    }

    void doSwap(Map data) {
        SwapInfo swapInfo = new SwapInfo(user: springSecurityService.getCurrentUser(), comment: data.comment, descriptionOfChanges: "")
        data.data.each { SampleSwapData it ->
            swapInfo.descriptionOfChanges += it.getSampleSwapInfosAsString()
            assert swapInfo.addToSeqTracks(SeqTrack.get(it.seqTrackId)).save(flush: true)
        }
    }

    private List validateAntibodyTarget(SampleSwapData data) {
        List output = []
        data.findByKey("antibodyTarget").each { SampleSwapInfo it ->
            if (it.newValue != "" && data.newValues.seqType != "ChIP") {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'antibodyTarget', "Value 'antibodyTarget' of '${data.rowNumber}' can't change from '${it.oldValue}' to '${it.newValue}' because 'SeqType' is '${data.newValues.seqType}' and not 'ChIP'.", SampleSwapLevel.WARNING)
            }
        }
        return output
    }

    private List validateAntibody(SampleSwapData data) {
        List output = []
        data.findByKey("antibody").each { SampleSwapInfo it ->
            if (it.newValue != "" && data.newValues.seqType != "ChIP") {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'antibody', "Value 'antibody' of '${data.rowNumber}' can't change from '${it.oldValue}' to '${it.newValue}' because 'SeqType' is '${data.newValues.seqType}' and not 'ChIP'.", SampleSwapLevel.WARNING)
            }
        }
        return output
    }

    private List validateSeqType(SampleSwapData data) {
        List output = []
        data.findByKey("seqType").each { SampleSwapInfo it ->
            if (it.oldValue == "ChIP" && it.newValue != "ChIP" && data.newValues.antibodyTarget != '') {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'antibody', "Value 'antibodyTarget' of '${data.rowNumber}'  will be set from '${data.newValues.antibodyTarget}' to '' because 'SeqType' was set from '${it.oldValue}' to '${it.newValue}'.", SampleSwapLevel.WARNING)
            }
            if (it.oldValue == "ChIP" && it.newValue != "ChIP" && data.newValues.antibody != '') {
                output << new SampleSwapInfo(it.oldValue, it.newValue, it.seqTrackId, 'antibody', "Value 'antibody' of '${data.rowNumber}'  will be set from '${data.newValues.antibody}' to '' because 'SeqType' was set from '${it.oldValue}' to '${it.newValue}'.", SampleSwapLevel.WARNING)
            }
        }
        return output
    }
}

enum SampleSwapLevel {
    ERROR(1000),
    WARNING(500),
    INFO(100),
    NOLEVEL(0),

    private final int id;

    SampleSwapLevel(int id) { this.id = id; }

    public int getValue() { return id; }
}

class SampleSwapInfo {
    String oldValue
    String newValue
    String seqTrackId
    String key
    String description

    SampleSwapLevel level

    SampleSwapInfo(String oldValue, String newValue, String seqTrackId, String key, String description, SampleSwapLevel level) {
        this.oldValue = oldValue
        this.newValue = newValue
        this.seqTrackId = seqTrackId
        this.key = key
        this.description = description
        this.level = level
    }
}

class SampleSwapData {
    Map oldValues
    Map newValues
    String seqTrackId
    int rowNumber
    List<SampleSwapInfo> sampleSwapInfos = []


    SampleSwapData(Map oldValues, Map newValues, String seqTrackId) {
        this.oldValues = oldValues
        this.newValues = newValues
        this.seqTrackId = seqTrackId

    }

    SampleSwapData(SampleSwapInfo sampleSwapInfo) {
        sampleSwapInfos << sampleSwapInfo
    }

    SampleSwapLevel getNormalizedLevel(String key, String fileId = "") {
        SampleSwapLevel level = SampleSwapLevel.NOLEVEL
        findByKey(key, fileId).each { it ->
            if (it.level.getValue() > level.getValue()) {
                level = it.level
            }
        }
        return level
    }


    List<SampleSwapInfo> findByKey(String key, String fileId = "") {
        List output = []
        sampleSwapInfos.each { it ->
            if (it.key == key) {
                if (fileId == "") {
                    output << it
                } else if (it.newValue == fileId) {
                    output << it
                }
            }
        }
        return output
    }

    String getSampleSwapInfosAsString() {
        return sampleSwapInfos*.description.join(", ")
    }
}
