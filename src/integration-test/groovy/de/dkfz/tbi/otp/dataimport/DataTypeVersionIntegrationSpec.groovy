/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.dataimport

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.dataimport.DataImportDomainFactory
import de.dkfz.tbi.otp.utils.validation.ImmutablePropertyValidator

@Rollback
@Integration
class DataTypeVersionIntegrationSpec extends Specification implements DataImportDomainFactory {

    private static final String OTHER_SCHEMA = '''{"type": "object", "properties": {
        "project": {"otpKeyType": "PROJECT"}, "file": {"otpKeyType": "FILE"}}, "required": ["project"]}'''

    @Autowired
    SessionFactory sessionFactory

    void "save, when dataType, dataTypeVersion and rawJsonSchema are changed, then the changes are not written to the database"() {
        given:
        DataTypeVersion version = createDataTypeVersion()
        DataType originalDataType = version.dataType
        int originalVersion = version.dataTypeVersion
        String originalSchema = version.rawJsonSchema
        DataType otherDataType = createDataType()

        when:
        version.dataType = otherDataType
        version.dataTypeVersion = originalVersion + 1
        version.rawJsonSchema = OTHER_SCHEMA
        version.save(validate: false, flush: true)
        sessionFactory.currentSession.clear()

        then:
        DataTypeVersion reloaded = DataTypeVersion.get(version.id)
        reloaded.dataType.id == originalDataType.id
        reloaded.dataTypeVersion == originalVersion
        reloaded.rawJsonSchema == originalSchema
    }

    @Unroll
    void "validate, when #property of a saved version is changed (detached: #detached), then it is invalid"() {
        given:
        DataTypeVersion version = reloadedVersion(detached)
        Object newValue = value(version)

        when:
        version[property] = newValue

        then:
        TestCase.assertAtLeastExpectedValidateError(version, property, ImmutablePropertyValidator.CHANGED, newValue)

        where:
        property          | detached | value
        'dataType'        | false    | { DataTypeVersion v -> createDataType() }
        'dataType'        | true     | { DataTypeVersion v -> createDataType() }
        'dataTypeVersion' | false    | { DataTypeVersion v -> v.dataTypeVersion + 1 }
        'dataTypeVersion' | true     | { DataTypeVersion v -> v.dataTypeVersion + 1 }
        'rawJsonSchema'   | false    | { DataTypeVersion v -> OTHER_SCHEMA }
        'rawJsonSchema'   | true     | { DataTypeVersion v -> OTHER_SCHEMA }
    }

    @Unroll
    void "validate, when the keys of a saved version are #name (detached: #detached), then it is invalid"() {
        given:
        DataTypeVersion version = reloadedVersion(detached)
        Key otherKey = createKey(dataType: version.dataType)

        when:
        change(version, otherKey)

        then:
        TestCase.assertValidateError(version, 'keys', ImmutablePropertyValidator.CHANGED, version.keys)

        where:
        name                 | detached | change
        'replaced'           | false    | { DataTypeVersion v, Key k -> v.keys = new HashSet<Key>(v.keys) }
        'replaced by others' | false    | { DataTypeVersion v, Key k -> v.keys = [k] as Set<Key> }
        'added'              | false    | { DataTypeVersion v, Key k -> v.addToKeys(k) }
        'removed'            | false    | { DataTypeVersion v, Key k -> v.removeFromKeys(v.keys.first()) }
        'removed directly'   | false    | { DataTypeVersion v, Key k -> v.keys.remove(v.keys.first()) }
        'cleared'            | false    | { DataTypeVersion v, Key k -> v.keys.clear() }
        'replaced'           | true     | { DataTypeVersion v, Key k -> v.keys = new HashSet<Key>(v.keys) }
        'replaced by others' | true     | { DataTypeVersion v, Key k -> v.keys = [k] as Set<Key> }
        'added'              | true     | { DataTypeVersion v, Key k -> v.addToKeys(k) }
        'removed'            | true     | { DataTypeVersion v, Key k -> v.removeFromKeys(v.keys.first()) }
        'removed directly'   | true     | { DataTypeVersion v, Key k -> v.keys.remove(v.keys.first()) }
        'cleared'            | true     | { DataTypeVersion v, Key k -> v.keys.clear() }
    }

    void "validate, when the keys of a version loaded read-only are replaced by the same keys, then it is invalid"() {
        given:
        DataTypeVersion version = createDataTypeVersion()
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        DataTypeVersion readOnly = DataTypeVersion.read(version.id)
        readOnly.keys.size()

        when:
        readOnly.keys = new HashSet<Key>(readOnly.keys)

        then:
        TestCase.assertValidateError(readOnly, 'keys', ImmutablePropertyValidator.CHANGED, readOnly.keys)
    }

    void "validate, when a version loaded read-only is unchanged, then it is valid"() {
        given:
        DataTypeVersion version = createDataTypeVersion()
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        DataTypeVersion readOnly = DataTypeVersion.read(version.id)

        expect:
        readOnly.keys.size() > 0
        readOnly.validate()
    }

    @Unroll
    void "validate, when a saved version is unchanged (detached: #detached), then it is valid"() {
        given:
        DataTypeVersion version = reloadedVersion(detached)

        expect:
        version.keys.size() > 0
        version.validate()

        where:
        detached << [false, true]
    }

    void "validate, when a new version is created, then the properties are not checked for changes"() {
        given:
        DataTypeVersion version = createDataTypeVersion([:], false)

        expect:
        ['dataType', 'dataTypeVersion', 'rawJsonSchema', 'keys'].every { !ImmutablePropertyValidator.isChanged(version, it) }
        version.validate()
    }

    /**
     * A saved version loaded in a new session, so that its keys are a loaded Hibernate collection.
     * If detached, the version and its keys are loaded and afterwards removed from the session.
     */
    private DataTypeVersion reloadedVersion(boolean detached = false) {
        DataTypeVersion version = createDataTypeVersion()
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        DataTypeVersion reloaded = DataTypeVersion.get(version.id)
        if (detached) {
            reloaded.keys.size()
            sessionFactory.currentSession.evict(reloaded)
        }
        return reloaded
    }
}
