package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Mock([
        ExternallyProcessedMergedBamFile,
])
class ExternallyProcessedMergedBamFileSpec extends Specification {


    private Map createMap() {
        return [
                fileName           : 'bamfile.bam',
                importedFrom       : '/tmp/bamfile',
                insertSizeFile     : null,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum             : DomainFactory.DEFAULT_MD5_SUM,
                maximumReadLength  : 5,
                workPackage        : new ExternalMergingWorkPackage([
                        pipeline: new Pipeline([
                                name: Pipeline.Name.EXTERNALLY_PROCESSED,
                        ]),
                ]),
        ]
    }


    void "all constraint fine, create object"() {
        given:
        ExternallyProcessedMergedBamFile bamFile = new ExternallyProcessedMergedBamFile(createMap())

        when:
        bamFile.validate()

        then:
        !bamFile.errors.hasErrors()
    }

    @Unroll
    void "constraint, if property '#property' is '#value', then validation should not fail"() {
        given:
        ExternallyProcessedMergedBamFile bamFile = new ExternallyProcessedMergedBamFile(createMap())
        bamFile[property] = value

        when:
        bamFile.validate()

        then:
        !bamFile.errors.hasErrors()

        where:
        property              | value
        'importedFrom'        | null
        'insertSizeFile'      | null
        'insertSizeFile'      | 'tmp/tmp'
        'fileOperationStatus' | AbstractMergedBamFile.FileOperationStatus.DECLARED
        'maximumReadLength'   | null
    }

    @Unroll
    void "constraint, if property '#property' is '#value', then validation should fail for '#constraint'"() {
        given:
        ExternallyProcessedMergedBamFile bamFile = new ExternallyProcessedMergedBamFile(createMap())
        bamFile[property] = value

        expect:
        TestCase.assertAtLeastExpectedValidateError(bamFile, property, constraint, value)

        where:
        property             | constraint          | value
        'insertSizeFile'     | 'blank'             | ''
        'insertSizeFile'     | 'maxSize.exceeded'  | '0'.padRight(2000, '0')
        'insertSizeFile'     | 'validator.invalid' | '/tmp'
        'insertSizeFile'     | 'validator.invalid' | 'tmp//tmp'
        'insertSizeFile'     | 'validator.invalid' | 'tmp&tmp'

        'fileName'           | 'nullable'          | null
        'fileName'           | 'blank'             | ''
        'fileName'           | 'validator.invalid' | '/tmp'
        'fileName'           | 'validator.invalid' | 'tmp/tmp'
        'fileName'           | 'validator.invalid' | 'tmp&tmp'

        'importedFrom'       | 'blank'             | ''
        'importedFrom'       | 'validator.invalid' | 'tmp'
        'importedFrom'       | 'validator.invalid' | '/tmp//tmp'
        'importedFrom'       | 'validator.invalid' | '/tmp&tmp'

        'maximumReadLength'  | 'min.notmet'        | -5
    }


}
