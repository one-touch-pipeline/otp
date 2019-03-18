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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*

// !! This class is only to test the abstract class ConfigPerProjectAndSeqType
class ConfigPerProjectAndSeqTypeImpl extends ConfigPerProjectAndSeqType { }

@TestFor(ConfigPerProjectAndSeqTypeImpl)
@Mock([Pipeline, Project, ProjectCategory, Realm, SeqType])
class ConfigPerProjectAndSeqTypeUnitTests {


    @Test
    void testSaveWithoutProject_shouldFail() {
        ConfigPerProjectAndSeqType configPerProject = new ConfigPerProjectAndSeqTypeImpl(
                pipeline: DomainFactory.createIndelPipelineLazy(),
                seqType: DomainFactory.createSeqType(),
        )
        TestCase.assertValidateError(configPerProject, 'project', 'nullable', null)

        configPerProject.project = DomainFactory.createProject()
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSaveWithObsoleteDate() {
        ConfigPerProjectAndSeqType configPerProject = new ConfigPerProjectAndSeqTypeImpl(
                project: DomainFactory.createProject(),
                obsoleteDate: new Date(),
                pipeline: DomainFactory.createIndelPipelineLazy(),
                seqType: DomainFactory.createSeqType(),
        )
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSaveWithReferenceToPreviousConfigWithoutObsolete_shouldFail() {
        ConfigPerProjectAndSeqType validConfigPerProject = new ConfigPerProjectAndSeqTypeImpl(
                project: DomainFactory.createProject(),
                seqType: DomainFactory.createSeqType(),
                pipeline: DomainFactory.createIndelPipelineLazy(),
        )
        validConfigPerProject.save()

        ConfigPerProjectAndSeqType newConfigPerProject = new ConfigPerProjectAndSeqTypeImpl(
                project: DomainFactory.createProject(),
                seqType: DomainFactory.createSeqType(),
                pipeline: DomainFactory.createIndelPipelineLazy(),
                previousConfig: validConfigPerProject,
        )
        TestCase.assertValidateError(newConfigPerProject, 'previousConfig', 'validator.invalid', validConfigPerProject)

        validConfigPerProject.obsoleteDate = new Date()
        assertTrue(validConfigPerProject.validate())
        assertTrue(newConfigPerProject.validate())
    }
}
