package de.dkfz.tbi.otp.ngsdata



import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(SeqPlatform)
class SeqPlatformTests {

    void testToString() {
        SeqPlatform aPlatform = new SeqPlatform(name: "name", model: "1234")
        assertEquals(aPlatform.toString(), "name 1234")

        aPlatform = new SeqPlatform(name: "name", model: "12345")
        assertEquals(aPlatform.toString(), "12345")
    }
}
