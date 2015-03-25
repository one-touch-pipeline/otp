package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import org.junit.*

@TestFor(LibraryPreparationKit)
class LibraryPreparationKitTests {

    LibraryPreparationKit kit1
    LibraryPreparationKit kit2

    @Before
    void setUp() {
        kit1 = new LibraryPreparationKit(
                name: "kitName1"
                )
        kit2 = new LibraryPreparationKit(
                name: "kitName2"
                )
    }

    @After
    void tearDown() {
        kit1 = null
        kit2 = null
    }

    void testCreateCorrect() {
        assertTrue kit1.validate()
        assertNotNull kit1.save(flush: true)
        assertTrue kit2.validate()
        assertNotNull kit2.save(flush: true)
        assertTrue !kit1.toString().empty
        assertTrue !kit2.toString().empty
    }

    void testNameIsNull() {
        kit1.name = null
        assertFalse kit1.validate()
    }

    void testNameIsEmpty() {
        kit1.name = ""
        assertFalse kit1.validate()
    }

    void testNameNotUnique() {
        assertNotNull kit1.save(flush: true)
        kit2.name = "kitName1"
        assertFalse kit2.validate()
    }
}
