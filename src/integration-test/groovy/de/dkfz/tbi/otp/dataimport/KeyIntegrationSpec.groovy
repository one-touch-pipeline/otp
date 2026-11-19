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
class KeyIntegrationSpec extends Specification implements DataImportDomainFactory {

    @Autowired
    SessionFactory sessionFactory

    @Unroll
    void "validate, when #property of a saved key is changed (detached: #detached), then it is invalid"() {
        given:
        Key key = reloadedKey(detached)
        Object newValue = value()

        when:
        key[property] = newValue

        then:
        TestCase.assertValidateError(key, property, ImmutablePropertyValidator.CHANGED, newValue)

        where:
        property   | detached | value
        'name'     | false    | { -> "otherName" }
        'name'     | true     | { -> "otherName" }
        'keyType'  | false    | { -> KeyType.FILE }
        'keyType'  | true     | { -> KeyType.FILE }
        'dataType' | false    | { -> createDataType() }
        'dataType' | true     | { -> createDataType() }
    }

    void "save, when name, keyType and dataType are changed, then the changes are not written to the database"() {
        given:
        Key key = createKey(keyType: KeyType.DATA)
        String originalName = key.name
        DataType originalDataType = key.dataType
        DataType otherDataType = createDataType()

        when:
        key.name = "otherName"
        key.keyType = KeyType.FILE
        key.dataType = otherDataType
        key.save(validate: false, flush: true)
        sessionFactory.currentSession.clear()

        then:
        Key reloaded = Key.get(key.id)
        reloaded.name == originalName
        reloaded.keyType == KeyType.DATA
        reloaded.dataType.id == originalDataType.id
    }

    /** a saved key loaded in a new session, optionally removed from the session afterwards */
    private Key reloadedKey(boolean detached) {
        Key key = createKey(keyType: KeyType.DATA)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        Key reloaded = Key.get(key.id)
        if (detached) {
            sessionFactory.currentSession.evict(reloaded)
        }
        return reloaded
    }
}
