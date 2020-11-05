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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class BamFileAnalysisServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                FileType,
                Individual,
                MergingWorkPackage,
                Pipeline,
                Project,
                Realm,
                ReferenceGenome,
                RoddyBamFile,
                RoddySnvCallingInstance,
                RoddyWorkflowConfig,
                FastqImportInstance,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqType,
        ]
    }


    void "test withdraw"() {
        given:
        BamFileAnalysisService service = new SnvCallingService()
        RoddySnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()

        when:
        service.withdraw(snvCallingInstance)

        then:
        snvCallingInstance.withdrawn
    }
}
