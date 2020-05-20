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
package de.dkfz.tbi.otp.ngsdata


import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.project.Project

class RunServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
                FileType,
                Individual,
                MetaDataFile,
                Project,
                Realm,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    RunService runService = new RunService()

    void 'retrieveMetaDataFiles finds correct MetaDataFiles'() {
        given:
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup(seqPlatformGroups: null)

        Run runWithoutDataFile = DomainFactory.createRun(seqPlatform: seqPlatform)

        Run run1 = DomainFactory.createRun(seqPlatform: seqPlatform)
        MetaDataFile run1MetaDataFileA = DomainFactory.createMetaDataFile()
        DomainFactory.createDataFile(run: run1, fastqImportInstance: run1MetaDataFileA.fastqImportInstance)
        MetaDataFile run1MetaDataFileB = DomainFactory.createMetaDataFile()
        MetaDataFile run1MetaDataFileC = DomainFactory.createMetaDataFile(fastqImportInstance: run1MetaDataFileB.fastqImportInstance)
        DomainFactory.createDataFile(run: run1, fastqImportInstance: run1MetaDataFileB.fastqImportInstance)

        Run run2 = DomainFactory.createRun(seqPlatform: seqPlatform)
        MetaDataFile run2MetaDataFile = DomainFactory.createMetaDataFile()
        DomainFactory.createDataFile(run: run2, fastqImportInstance: run2MetaDataFile.fastqImportInstance)
        DomainFactory.createDataFile(run: run2, fastqImportInstance: run2MetaDataFile.fastqImportInstance)

        expect:
        runService.retrieveMetaDataFiles(runWithoutDataFile).isEmpty()
        TestCase.assertContainSame(runService.retrieveMetaDataFiles(run1), [run1MetaDataFileA, run1MetaDataFileB, run1MetaDataFileC])
        TestCase.assertContainSame(runService.retrieveMetaDataFiles(run2), [run2MetaDataFile])
    }
}
