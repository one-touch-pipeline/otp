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
package de.dkfz.tbi.otp.utils.validation

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataimport.Key
import de.dkfz.tbi.otp.dataimport.KeyType
import de.dkfz.tbi.otp.domainFactory.dataimport.DataImportDomainFactory

@Rollback
@Integration
class ImmutablePropertyValidatorIntegrationSpec extends Specification implements DataImportDomainFactory {

    @Autowired
    SessionFactory sessionFactory

    void "isChanged, when the object is not saved, then return false"() {
        given:
        Key key = createKey([:], false)

        when:
        key.name = "other"

        then:
        !ImmutablePropertyValidator.isChanged(key, 'name')
    }

    @Unroll
    void "isChanged, when the property of a saved object is #name (detached: #detached), then return #expected"() {
        given:
        Key key = reloadedKey(detached)

        when:
        change(key)

        then:
        ImmutablePropertyValidator.isChanged(key, 'name') == expected

        where:
        name                    | detached | change                                  || expected
        'unchanged'             | false    | { Key k -> }                            || false
        'unchanged'             | true     | { Key k -> }                            || false
        'set to the same value' | false    | { Key k -> k.name = new String(k.name) } || false
        'set to the same value' | true     | { Key k -> k.name = new String(k.name) } || false
        'changed'               | false    | { Key k -> k.name = "other" }           || true
        'changed'               | true     | { Key k -> k.name = "other" }           || true
    }

    void "isChanged, when another property of a saved object is changed, then return false"() {
        given:
        Key key = reloadedKey()

        when:
        key.keyType = KeyType.FILE

        then:
        !ImmutablePropertyValidator.isChanged(key, 'name')
    }

    @Unroll
    void "validator, when the property of a saved object is changed is #changed, then return #expected"() {
        given:
        Key key = reloadedKey()
        if (changed) {
            key.name = "other"
        }

        expect:
        ImmutablePropertyValidator.validator('name').call(key.name, key) == expected

        where:
        changed || expected
        false   || true
        true    || ImmutablePropertyValidator.CHANGED
    }

    /** a saved key loaded in a new session, optionally removed from the session afterwards */
    private Key reloadedKey(boolean detached = false) {
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
