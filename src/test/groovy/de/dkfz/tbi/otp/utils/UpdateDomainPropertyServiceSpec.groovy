/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.utils

import grails.artefact.Artefact
import grails.artefact.DomainClass
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.GormEntity
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

import java.time.LocalDate
import java.time.format.DateTimeParseException

class UpdateDomainPropertyServiceSpec extends Specification implements ServiceUnitTest<UpdateDomainPropertyService>, DataTest, DomainFactoryCore {

    enum TestEnum {
        VALUE1,
        VALUE2,
        VALUE3,
    }

    @Artefact(DomainClassArtefactHandler.TYPE)
    class TestDomain implements GormEntity<TestDomain>, DomainClass {
        String stringValue
        Integer integerValue
        Long longValue
        Boolean booleanValue
        Double doubleValue
        Float floatValue
        LocalDate localDateValue
        TestEnum testEnumValue

        Set<String> stringValueSet
        Set<Integer> integerValueSet
        Set<Long> longValueSet
        Set<Boolean> booleanValueSet
        Set<Double> doubleValueSet
        Set<Float> floatValueSet
        Set<LocalDate> localDateValueSet
        Set<TestEnum> testEnumValueSet

        List<String> stringValueList
        List<Integer> integerValueList
        List<Long> longValueList
        List<Boolean> booleanValueList
        List<Double> doubleValueList
        List<Float> floatValueList
        List<LocalDate> localDateValueList
        List<TestEnum> testEnumValueList

        // grails DSL
        @SuppressWarnings('NoDef')
        static constraints = {
            stringValue nullable: true
            integerValue nullable: true
            longValue nullable: true
            booleanValue nullable: true
            doubleValue nullable: true
            floatValue nullable: true
            localDateValue nullable: true
            testEnumValue nullable: true
        }
    }

    @Override
    Class[] getDomainClassesToMock() {
        return [
                TestDomain,
        ]
    }

    @Unroll
    void "updateProperty, when property '#property' set to value '#value', then the property is updated"() {
        given:
        TestDomain testDomain = new TestDomain().save(flush: true)

        when:
        service.updateProperty(TestDomain, testDomain.id, property, value)

        then:
        testDomain[property] == expected

        where:
        property         | value        | expected
        'stringValue'    | 'test'       | 'test'
        'stringValue'    | ''           | null

        'integerValue'   | '0'          | 0
        'integerValue'   | '3'          | 3
        'integerValue'   | '-3'         | -3
        'integerValue'   | ''           | null

        'booleanValue'   | 'true'       | true
        'booleanValue'   | 'false'      | false
        'booleanValue'   | ''           | null
        'booleanValue'   | 't'          | false
        'booleanValue'   | 'f'          | false

        'longValue'      | '0'          | 0L
        'longValue'      | '3'          | 3L
        'longValue'      | '-3'         | -3L
        'longValue'      | ''           | null

        'doubleValue'    | '0'          | 0D
        'doubleValue'    | '3.4'        | 3.4D
        'doubleValue'    | '-3.4'       | -3.4D
        'doubleValue'    | ''           | null

        'floatValue'     | '0'          | 0F
        'floatValue'     | '3.4'        | 3.4F
        'floatValue'     | '-3.4'       | -3.4F
        'floatValue'     | ''           | null

        'localDateValue' | '2020-05-04' | LocalDate.of(2020, 5, 4)
        'localDateValue' | ''           | null

        'testEnumValue'  | 'VALUE1'     | TestEnum.VALUE1
        'testEnumValue'  | 'VALUE2'     | TestEnum.VALUE2
        'testEnumValue'  | ''           | null
    }

    @Unroll
    void "updateProperties, when property '#property' set to value '#values', then the property is updated"() {
        given:
        TestDomain testDomain = new TestDomain().save(flush: true)

        when:
        service.updateProperties(TestDomain, testDomain.id, property, values)

        then:
        testDomain[property] == expected

        where:
        property             | values                       | expected
        'stringValueSet'     | ['test']                     | ['test'] as Set
        'stringValueSet'     | ['test', 'test3']            | ['test', 'test3'] as Set
        'stringValueSet'     | []                           | [] as Set
        'stringValueSet'     | ['test', '', 'test3', '']    | ['test', 'test3'] as Set
        'stringValueSet'     | ['test', 'test']             | ['test'] as Set

        'integerValueSet'    | ['3', '4', '5']              | [3, 4, 5] as Set
        'booleanValueSet'    | ['true', 'false']            | [true, false] as Set
        'longValueSet'       | ['3', '5', '-7']             | [3L, 5L, -7L] as Set
        'doubleValueSet'     | ['-3.4', '0', '3.4']         | [3.4D, 0D, -3.4D] as Set
        'floatValueSet'      | ['-3.4', '0', '3.4']         | [3.4F, 0F, -3.4F] as Set
        'localDateValueSet'  | ['2020-05-04', '2020-08-04'] | [LocalDate.of(2020, 8, 4), LocalDate.of(2020, 5, 4)] as Set
        'testEnumValueSet'   | ['VALUE2', 'VALUE3']         | [TestEnum.VALUE2, TestEnum.VALUE3] as Set

        'stringValueList'    | ['test']                     | ['test']
        'stringValueList'    | ['test', 'test3']            | ['test', 'test3']
        'stringValueList'    | []                           | []
        'stringValueList'    | ['test', '', 'test3', '']    | ['test', 'test3']
        'stringValueList'    | ['test', 'test']             | ['test', 'test']

        'integerValueList'   | ['3', '4', '5']              | [3, 4, 5]
        'booleanValueList'   | ['true', 'false']            | [true, false]
        'longValueList'      | ['3', '5', '-7']             | [3L, 5L, -7L]
        'doubleValueList'    | ['-3.4', '0', '3.4']         | [-3.4D, 0D, 3.4D]
        'floatValueList'     | ['-3.4', '0', '3.4']         | [-3.4F, 0F, 3.4F]
        'localDateValueList' | ['2020-05-04', '2020-08-04'] | [LocalDate.of(2020, 5, 4), LocalDate.of(2020, 8, 4)]
        'testEnumValueList'  | ['VALUE2', 'VALUE3']         | [TestEnum.VALUE2, TestEnum.VALUE3]
    }

    @Unroll
    void "updateProperty, when parameter '#parameter' is '#value', then throw #ExceptionClass"() {
        given:
        TestDomain testDomain = new TestDomain().save(flush: true)

        when:
        service.updateProperty(
                parameter == 'clazz' ? value : TestDomain,
                parameter == 'entityId' ? value : testDomain.id,
                parameter == 'property' ? value : 'testEnumValue',
                parameter == 'value' ? value : TestEnum.VALUE1.name()
        )

        then:
        thrown(ExceptionClass)

        where:
        parameter  | value             || ExceptionClass
        'clazz'    | null              || AssertionError
        'clazz'    | String            || AssertionError

        'entityId' | null              || AssertionError
        'entityId' | -5L               || AssertionError

        'property' | null              || AssertionError
        'property' | 'unknown'         || AssertionError
        'property' | 'integerValue'    || NumberFormatException
        'property' | 'longValue'       || NumberFormatException
        'property' | 'doubleValue'     || NumberFormatException
        'property' | 'floatValue'      || NumberFormatException
        'property' | 'localDateValue'  || DateTimeParseException
        'property' | 'stringValueSet'  || AssertionError
        'property' | 'stringValueList' || AssertionError

        'value'    | 'value'           || IllegalArgumentException
    }

    @Unroll
    void "updateProperties, when parameter '#parameter' is '#value', then throw #ExceptionClass"() {
        given:
        TestDomain testDomain = new TestDomain().save(flush: true)

        when:
        service.updateProperties(
                parameter == 'clazz' ? value : TestDomain,
                parameter == 'entityId' ? value : testDomain.id,
                parameter == 'property' ? value : 'testEnumValueSet',
                parameter == 'value' ? value : [TestEnum.VALUE1.name()]
        )

        then:
        thrown(ExceptionClass)

        where:
        parameter  | value                || ExceptionClass
        'clazz'    | null                 || AssertionError
        'clazz'    | String               || AssertionError

        'entityId' | null                 || AssertionError
        'entityId' | -5L                  || AssertionError

        'property' | null                 || AssertionError
        'property' | 'unknown'            || AssertionError
        'property' | 'stringValue'        || AssertionError

        'property' | 'integerValueSet'    || NumberFormatException
        'property' | 'longValueSet'       || NumberFormatException
        'property' | 'doubleValueSet'     || NumberFormatException
        'property' | 'floatValueSet'      || NumberFormatException
        'property' | 'localDateValueSet'  || DateTimeParseException

        'property' | 'integerValueList'   || NumberFormatException
        'property' | 'longValueList'      || NumberFormatException
        'property' | 'doubleValueList'    || NumberFormatException
        'property' | 'floatValueList'     || NumberFormatException
        'property' | 'localDateValueList' || DateTimeParseException

        'value'    | ['value']            || IllegalArgumentException
    }
}
