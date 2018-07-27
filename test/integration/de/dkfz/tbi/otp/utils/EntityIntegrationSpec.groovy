package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.acl.*
import org.codehaus.groovy.grails.commons.*
import seedme.*
import spock.lang.*

@SuppressWarnings("ComparisonWithSelf")
class EntityIntegrationSpec extends Specification {

    GrailsApplication grailsApplication

    private SeqType createProxy(SeqType seqType) {
        seqType.discard() //detach object from session to get proxy
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
        List<Class> nonOtpDomains = [AclClass, AclEntry, AclObjectIdentity, AclSid, SeedMeChecksum]

        when:
        List<String> domainsWithoutEntity = grailsApplication.domainClasses.findAll {
            !nonOtpDomains.contains(it.clazz) &&            //ignore non otp domains
                    !dbViews.contains(it.clazz) &&          //ignore the dbviews
                    !(Entity.isAssignableFrom(it.clazz))    //find class not implementing Entity
        }*.clazz*.name

        then:
        domainsWithoutEntity.empty
    }
}
