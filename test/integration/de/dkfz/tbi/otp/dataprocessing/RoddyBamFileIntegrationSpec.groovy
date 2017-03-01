package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

class RoddyBamFileIntegrationSpec extends Specification {

    void "test getNumberOfReadsFromQa"() {
        given:
        long pairedRead = DomainFactory.counter++
        long numberOfReads = 2 * pairedRead
        RoddyMergedBamQa qa = DomainFactory.createRoddyMergedBamQa([
                pairedRead1: pairedRead,
                pairedRead2: pairedRead,
                referenceLength: 0,
        ])
        RoddyBamFile roddyBamFile = qa.roddyBamFile

        expect:
        numberOfReads == roddyBamFile.getNumberOfReadsFromQa()
    }
}
