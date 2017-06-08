package de.dkfz.tbi.otp.job.processing

import spock.lang.*


class PbsOptionMergingServiceSpec extends Specification {

    void "test jsonStringToMap"() {
        expect:
        expected == PbsOptionMergingService.convertJsonStringToMap(jsonString)

        where:
        jsonString                           | expected
        ''                                   | [:]
        '{"WALLTIME": "30"}'                 | [(JobSubmissionOption.WALLTIME): "30"]
        '{"WALLTIME": "30", "NODES": "1"}'   | [(JobSubmissionOption.NODES): "1", (JobSubmissionOption.WALLTIME): "30"]
        '{"WALLTIME": "30", "NODES": "1", }' | [(JobSubmissionOption.NODES): "1", (JobSubmissionOption.WALLTIME): "30"]
    }

    void "test validateOptionString, valid string"() {
        expect:
        PbsOptionMergingService.validateJsonString(jsonString)

        where:
        jsonString                           | _
        ''                                   | _
        '{"WALLTIME": "30"}'                 | _
        '{"WALLTIME": "30", "NODES": "1"}'   | _
        '{"WALLTIME": "30", "NODES": "1", }' | _
    }

    void "test validateOptionString, invalid string"() {
        expect:
        !PbsOptionMergingService.validateJsonString(jsonString)

        where:
        jsonString                           | _
        'asdf'                               | _
        '"WALLTIME": "30"'                   | _
        '[]'                                 | _
        '["WALLTIME", "30"]'                 | _
        '{}'                                 | _
        '{"WALL_TIME": "30"}'                | _
        '{"WALLTIME": 30}'                   | _
        '{"WALLTIME": "30", "NODES: 1}'      | _
        '{"WALLTIME": "30", {"NODES": "1"}}' | _
    }
}
