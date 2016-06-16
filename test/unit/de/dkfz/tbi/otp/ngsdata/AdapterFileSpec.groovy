package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import spock.lang.*

class AdapterFileSpec extends Specification {


    void "test constraint, when all fine then no exception should be thrown"() {
        when:
        AdapterFile adapterFile = new AdapterFile(
                fileName: 'value'
        )

        then:
        adapterFile.validate()
    }

    @Unroll
    void "test constraint, when constraint #constraint is invalid then validate fails"() {
        when:
        AdapterFile adapterFile = new AdapterFile()
        adapterFile.fileName = value

        then:
        TestCase.assertValidateError(adapterFile, 'fileName', constraint, value)

        where:
        constraint          | value
        'nullable'          | null
        'blank'             | ''
        'validator.invalid' | 'invalid path element'
    }

    void "test constraint, when constraint unique is invalid then validate fails"() {
        given:
        String value = 'value'
        DomainFactory.createAdapterFile(fileName: value)

        when:
        AdapterFile adapterFile = new AdapterFile(fileName: value)

        then:
        TestCase.assertValidateError(adapterFile, 'fileName', 'unique', value)
    }

}
