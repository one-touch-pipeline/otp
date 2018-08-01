package de.dkfz.tbi.otp.config

import de.dkfz.tbi.otp.job.jobs.dataInstallation.*
import spock.lang.*

class TypeValidatorsIntegrationSpec extends Specification {

    private static String JOB_NAME = CopyFilesJob.class.getSimpleName()

    @Unroll
    void "check JOB_NAME for value '#value' should return '#ret'"() {
        expect:
        ret == TypeValidators.JOB_NAME_SEQ_TYPE.validate(value)

        where:
        value          || ret
        JOB_NAME       || true
        ''             || false
        null           || false
        'OtherJobName' || false
    }

}
