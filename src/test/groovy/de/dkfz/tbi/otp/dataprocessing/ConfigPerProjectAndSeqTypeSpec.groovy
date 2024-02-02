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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory

// !! This class is only to test the abstract class ConfigPerProjectAndSeqType
@Artefact(DomainClassArtefactHandler.TYPE)
@SuppressWarnings('EmptyClass')
class ConfigPerProjectAndSeqTypeMock extends ConfigPerProjectAndSeqType implements DomainClass, GormEntity<ConfigPerProjectAndSeqTypeMock>, Validateable {
}

class ConfigPerProjectAndSeqTypeSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ConfigPerProjectAndSeqTypeMock,
        ]
    }

    static final String PROGRAM_VERSION = "programVersion"

    void testSaveWithoutProject_shouldFail() {
        given:
        ConfigPerProjectAndSeqType configPerProject = new ConfigPerProjectAndSeqTypeMock(
                pipeline      : DomainFactory.createIndelPipelineLazy(),
                seqType       : DomainFactory.createSeqType(),
                programVersion: PROGRAM_VERSION,
        )

        expect:
        TestCase.assertValidateError(configPerProject, 'project', 'nullable', null)

        when:
        configPerProject.project = DomainFactory.createProject()
        configPerProject.validate()

        then:
        !configPerProject.errors.hasErrors()
    }

    void testSaveWithObsoleteDate() {
        given:
        ConfigPerProjectAndSeqType configPerProject = new ConfigPerProjectAndSeqTypeMock(
                project       : DomainFactory.createProject(),
                obsoleteDate  : new Date(),
                pipeline      : DomainFactory.createIndelPipelineLazy(),
                seqType       : DomainFactory.createSeqType(),
                programVersion: PROGRAM_VERSION,
        )

        when:
        configPerProject.validate()

        then:
        !configPerProject.errors.hasErrors()
    }

    void testSaveWithReferenceToPreviousConfigWithoutObsolete_shouldFail() {
        given:
        ConfigPerProjectAndSeqType validConfigPerProject = new ConfigPerProjectAndSeqTypeMock(
                project       : DomainFactory.createProject(),
                seqType       : DomainFactory.createSeqType(),
                pipeline      : DomainFactory.createIndelPipelineLazy(),
                programVersion: PROGRAM_VERSION,
        )

        expect:
        validConfigPerProject.save(flush: true)

        when:
        ConfigPerProjectAndSeqType newConfigPerProject = new ConfigPerProjectAndSeqTypeMock(
                project       : DomainFactory.createProject(),
                seqType       : DomainFactory.createSeqType(),
                pipeline      : DomainFactory.createIndelPipelineLazy(),
                previousConfig: validConfigPerProject,
                programVersion: PROGRAM_VERSION,
        )

        then:
        TestCase.assertValidateError(newConfigPerProject, 'previousConfig', 'validator.invalid', validConfigPerProject)

        when:
        validConfigPerProject.obsoleteDate = new Date()

        then:
        validConfigPerProject.validate()
        newConfigPerProject.validate()
    }
}
