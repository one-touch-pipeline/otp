package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

@TestFor(ExomeEnrichmentKitIdentifier)
@Mock([ExomeEnrichmentKit])
class ExomeEnrichmentKitIdentifierTests {

    ExomeEnrichmentKit kit
    ExomeEnrichmentKitIdentifier kitId1
    ExomeEnrichmentKitIdentifier kitId2

    @Before
    void setUp() {
        kit = new ExomeEnrichmentKit(
                name: "kitName1"
                )
        assertNotNull kit.save(flush: true)
        kitId1 = new ExomeEnrichmentKitIdentifier(
                name: "kitID1",
                exomeEnrichmentKit: kit
                )
        kitId2 = new ExomeEnrichmentKitIdentifier(
                name: "kitID2",
                exomeEnrichmentKit: kit
                )
    }

    @After
    void tearDown() {
        kit =  null
        kitId1 = null
        kitId2 = null
    }

    @Test
    void testCorrect() {
        assertTrue kitId1.validate()
        assertNotNull kitId1.save(flush: true)
        assertTrue kitId2.validate()
        assertNotNull kitId2.save(flush: true)
    }

    @Test
    void testNameNull() {
        kitId1.name = null
        assertFalse kitId1.validate()
    }

    @Test
    void testNameEmpty() {
        kitId1.name = ""
        assertFalse kitId1.validate()
    }

    @Test
    void testNameNotUnique() {
        assertNotNull kitId1.save(flush: true)
        kitId2.name = "kitID1"
        assertFalse kitId2.validate()
    }
}
