package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import spock.lang.*

class SnvCallingServiceSpec extends Specification {


    void "validateInputBamFiles, when all okay, return without exception"() {
        given:
        SnvCallingInstance instance = new SnvCallingInstance([
                sampleType1BamFile: new RoddyBamFile(),
                sampleType2BamFile: new RoddyBamFile(),
        ])
        SnvCallingService service = new SnvCallingService([
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    2 * getExistingBamFilePath(_) >> TestCase.uniqueNonExistentPath
                },
        ])

        when:
        service.validateInputBamFiles(instance)

        then:
        notThrown()
    }

    void "validateInputBamFiles, when path throw an exception, throw a new runtime exception"() {
        given:
        SnvCallingInstance instance = new SnvCallingInstance([
                sampleType1BamFile: new RoddyBamFile(),
                sampleType2BamFile: new RoddyBamFile(),
        ])
        SnvCallingService service = new SnvCallingService([
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    2 * getExistingBamFilePath(_) >> TestCase.uniqueNonExistentPath >> { assert false }
                },
        ])

        when:
        service.validateInputBamFiles(instance)

        then:
        RuntimeException e = thrown()
        e.message.contains('The input BAM files have changed on the file system while this job processed them.')
    }
}
