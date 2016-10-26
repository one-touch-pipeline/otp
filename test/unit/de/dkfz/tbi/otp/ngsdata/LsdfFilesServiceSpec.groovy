package de.dkfz.tbi.otp.ngsdata

import spock.lang.*

class LsdfFilesServiceSpec extends Specification {

    void "normalizePathForCustomers, when path starts with MOUNTPOINT_WITH_ICGC, then replace start with MOUNTPOINT_WITH_LSDF"() {
        given:
        File originalFile = new File(LsdfFilesService.MOUNTPOINT_WITH_ICGC, 'something')
        File expectedFile = new File(LsdfFilesService.MOUNTPOINT_WITH_LSDF, 'something')

        when:
        File file = LsdfFilesService.normalizePathForCustomers(originalFile)

        then:
        expectedFile == file
    }

    void "normalizePathForCustomers, when path not starts with icgc, then path should be unchanged"() {
        given:
        File originalFile = new File(LsdfFilesService.MOUNTPOINT_WITH_LSDF, 'something')

        when:
        File file = LsdfFilesService.normalizePathForCustomers(originalFile)

        then:
        originalFile == file
    }
}
