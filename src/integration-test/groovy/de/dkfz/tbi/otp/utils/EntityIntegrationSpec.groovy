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

import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*

@SuppressWarnings("ComparisonWithSelf")
@Rollback
@Integration
class EntityIntegrationSpec extends Specification {

    GrailsApplication grailsApplication

    private SeqType createProxy(SeqType seqType) {
        seqType.discard() // detach object from session to get proxy
        SeqType proxy = SeqType.proxy(seqType.id)
        assert !proxy.is(seqType)
        return proxy
    }

    void "test equals object is equal to itself"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()

        expect:
        seqType == seqType
    }

    void "test equals object is not equal to null"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()

        expect:
        seqType != null
    }

    void "test equals object is not equal to other instance of same class"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()
        SeqType otherSeqType = DomainFactory.createSeqType()

        expect:
        seqType != otherSeqType
    }

    void "test equals object is not equal to other instance of other class with same id"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()
        SampleType sampleType = DomainFactory.createSampleType()
        sampleType.id = seqType.id

        expect:
        seqType != sampleType
        seqType.id == sampleType.id
    }

    void "test equals object is not equal to non domain class"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()
        Long other = 1

        expect:
        seqType != other
    }

    void "test equals object is equal to proxy of itself"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()
        SeqType proxy = createProxy(seqType)

        expect:
        seqType == proxy
    }

    void "test equals proxy of object is equal to object "() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()
        SeqType proxy = createProxy(seqType)

        expect:
        proxy == seqType
    }

    void "test equals proxy of object is equal to other proxy of same object"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()
        SeqType proxy1 = createProxy(seqType)
        SeqType proxy2 = createProxy(seqType)

        expect:
        proxy1 == proxy2
    }

    void "test equals proxy of object is not equal to proxy of other object "() {
        setup:
        SeqType proxy1 = createProxy(DomainFactory.createSeqType())
        SeqType proxy2 = createProxy(DomainFactory.createSeqType())

        expect:
        proxy2 != proxy1
    }

    void "test equals unsaved object is equal to itself"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType([:], false)

        expect:
        seqType == seqType
    }

    void "test equals unsaved object is not equal to other unsaved object"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType([:], false)
        SeqType otherSeqType = DomainFactory.createSeqType([:], false)

        expect:
        seqType != otherSeqType
    }

    void "test equals unsaved object is not equal to a saved object"() {
        setup:
        SeqType seqTypeSaved = DomainFactory.createSeqType()
        SeqType seqTypeUnsaved = DomainFactory.createSeqType([:], false)

        expect:
        seqTypeSaved != seqTypeUnsaved
    }

    void "test equals saved object is not equal to an unsaved object"() {
        setup:
        SeqType seqTypeSaved = DomainFactory.createSeqType()
        SeqType seqTypeUnsaved = DomainFactory.createSeqType([:], false)

        expect:
        seqTypeUnsaved != seqTypeSaved
    }

    void "test hashcode is same as with proxy"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()

        expect:
        createProxy(seqType).hashCode() == seqType.id.hashCode()
    }

    void "test hashcode is same as hashcode of id"() {
        setup:
        SeqType seqType = DomainFactory.createSeqType()

        expect:
        seqType.hashCode() == seqType.id.hashCode()
    }

    void "test that all domain classes implement Entity"() {
        setup:
        List<Class> dbViews = [AggregateSequences, Sequence]
        List<Class> nonOtpDomains = []

        when:
        List<String> domainsWithoutEntity = grailsApplication.domainClasses.findAll {
            !nonOtpDomains.contains(it.clazz) &&            // ignore non otp domains
                    !dbViews.contains(it.clazz) &&          // ignore the dbviews
                    !(Entity.isAssignableFrom(it.clazz))    // find class not implementing Entity
        }*.clazz*.name

        then:
        domainsWithoutEntity.empty
    }
}
