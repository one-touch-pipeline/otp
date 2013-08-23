package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

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