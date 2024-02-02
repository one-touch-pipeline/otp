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
package de.dkfz.tbi.otp.dataprocessing

import grails.artefact.Artefact
import grails.artefact.DomainClass
import grails.testing.gorm.DataTest
import grails.validation.Validateable
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.GormEntity
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*

class AbstractBamFileSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FileType,
                MergingWorkPackage,
                RawSequenceFile,
                MockAbstractBamFile,
        ]
    }

    void "test save"() {
        given:
        AbstractBamFile bamFile = new MockAbstractBamFile(numberOfMergedLanes: 1, workPackage: new MergingWorkPackage())

        expect:
        bamFile.validate()
        bamFile.save(flush: true)
    }

    void "test save, when coverage is not null"() {
        given:
        AbstractBamFile bamFile = new MockAbstractBamFile(numberOfMergedLanes: 1, workPackage: new MergingWorkPackage())
        bamFile.coverage = 30.0

        expect:
        bamFile.validate()
        bamFile.save(flush: true)
    }

    void "validate, if dateFromFileSystem is null, then validation should pass"() {
        given:
        AbstractBamFile bamFile = new MockAbstractBamFile(numberOfMergedLanes: 1, workPackage: new MergingWorkPackage())

        when:
        // dateFromFileSystem is nullable
        bamFile.dateFromFileSystem = null
        bamFile.validate()

        then:
        !bamFile.errors.hasErrors()
    }
}

@Artefact(DomainClassArtefactHandler.TYPE)
class MockAbstractBamFile extends AbstractBamFile implements DomainClass, GormEntity<MockAbstractBamFile>, Validateable {

    final MergingWorkPackage mergingWorkPackage = null
    final AbstractQualityAssessment qualityAssessment = null
    final String bamFileName = null
    final String baiFileName = null
    final AlignmentConfig alignmentConfig = null
    final File finalInsertSizeFile = null
    final Integer maximalReadLength = null

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return [] as Set
    }

    @Override
    boolean isMostRecentBamFile() {
        return false
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return null
    }
}
