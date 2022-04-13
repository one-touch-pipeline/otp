/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.artefact.Artefact
import grails.artefact.DomainClass
import grails.testing.gorm.DataTest
import grails.validation.Validateable
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.GormEntity
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class AbstractBamFileSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                FileType,
                DataFile,
                MockAbstractBamFile,
                ProcessedBamFile,
        ]
    }

    void testSave() {
        given:
        AbstractBamFile bamFile = new MockAbstractBamFile()

        expect:
        bamFile.validate()
        bamFile.save(flush: true)
    }

    void testSaveCoverageNotNull() {
        given:
        AbstractBamFile bamFile = new MockAbstractBamFile()
        bamFile.coverage = 30.0

        expect:
        bamFile.validate()
        bamFile.save(flush: true)
    }

    void testWithdraw_ChangeStatusFromNeedsProcessingToDeclared() {
        given:
        AbstractBamFile bamFile = new MockAbstractBamFile([
                status: AbstractBamFile.State.NEEDS_PROCESSING,
        ])
        expect:
        bamFile.status == AbstractBamFile.State.NEEDS_PROCESSING

        when:
        LogThreadLocal.withThreadLog(System.out) {
            bamFile.withdraw()
        }

        then:
        bamFile.withdrawn
        bamFile.status == AbstractBamFile.State.DECLARED
    }
}

@Artefact(DomainClassArtefactHandler.TYPE)
class MockAbstractBamFile extends AbstractBamFile implements DomainClass, GormEntity<MockAbstractBamFile>, Validateable {

    final MergingWorkPackage mergingWorkPackage = null
    final AbstractQualityAssessment overallQualityAssessment = null

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return [] as Set
    }
}
