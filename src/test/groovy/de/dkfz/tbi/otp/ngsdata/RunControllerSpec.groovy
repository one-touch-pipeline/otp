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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.security.*

class RunControllerSpec extends Specification implements ControllerUnitTest<RunController>, DataTest, UserAndRoles {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqPlatformGroup,
                Role,
                User,
                RawSequenceFile,
                UserRole,
                Run,
                SeqCenter,
                MetaDataKey,
                ProcessParameter,
        ]
    }

    void setupData() {
        createUserAndRoles()
        controller.runService = new RunService()
        controller.lsdfFilesService = new LsdfFilesService()
        controller.fastqcResultsService = new FastqcResultsService()
    }

    void "testDisplayRedirect"() {
        given:
        setupData()

        when:
        controller.display()

        then:
        controller.response.redirectedUrl == "/run/show"
    }

    void "testShowRunNonExisting"() {
        given:
        setupData()

        when:
        controller.params.id = "0"
        controller.show()

        then:
        controller.response.status == 404
    }

    void "testShowRunMissingId"() {
        given:
        setupData()

        when:
        controller.show()

        then:
        controller.response.status == 404
    }

    void "testShowRunIdNoLong"() {
        given:
        setupData()

        when:
        controller.params.id = "test"
        controller.show()

        then:
        controller.response.status == 404
    }

    void "testShowRunMinimalData"() {
        given:
        setupData()

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assert seqCenter.save(flush: true)
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assert run.save(flush: true)

        when:
        controller.params.id = run.id.toString()
        def model = controller.show()

        then:
        model.run == run
        model.finalPaths.size() == 0
        model.keys[0] == null
        model.keys[1] == null
        model.nextRun == null
        model.previousRun == null
        model.seqTracks.isEmpty()
        model.metaDataFileWrapper.isEmpty()
        model.processParameters.isEmpty()
    }

    void "testShowRunByName"() {
        given:
        setupData()

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assert seqCenter.save(flush: true)
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assert run.save(flush: true)

        when:
        controller.params.id = "test"
        def model = controller.show()

        then:
        model.run == run
        model.finalPaths.size() == 0
        model.keys[0] == null
        model.keys[1] == null
        model.nextRun == null
        model.previousRun == null
        model.seqTracks.isEmpty()
        model.metaDataFileWrapper.isEmpty()
        model.processParameters.isEmpty()
    }
}
