package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.junit.Assert.assertEquals

@TestFor(ChromosomeIdentifierSortingService)
@TestMixin(GrailsUnitTestMixin)
class ChromosomeIdentifierSortingServiceTests {

    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService

    @Test
    void testSortIdentifiers() {
        chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()
        List<String> identifierUnsorted = ["1", "10", "14", "X", "GH", "16", "33", "2", "23", "Y"]
        List<String> identifierSortedExp = ["1", "2", "10", "14", "16", "X", "Y", "23", "33", "GH"]
        List<String> identifierSortedAct = chromosomeIdentifierSortingService.sortIdentifiers(identifierUnsorted)
        assertEquals(identifierSortedExp, identifierSortedAct)
    }
}