/*
 * Copyright 2011-2025 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

import java.nio.file.Paths

@Rollback
@Integration
class MergedAlignmentDataFileServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    MergedAlignmentDataFileService mergedAlignmentDataFileService

    void "test buildRelativePath"() {
        given:
        SeqType seqType = DomainFactory.createRnaPairedSeqType()
        Sample sample = createSample()
        String expectedPath = """${Paths.get(
                sample.project.dirName,
                "sequencing",
                "rna_sequencing",
                "view-by-pid",
                sample.individual.pid,
                sample.sampleType.dirName,
                "paired",
                "merged-alignment")}${File.separator}"""

        when:
        String actualPath = mergedAlignmentDataFileService.buildRelativePath(seqType, sample)

        then:
        expectedPath == actualPath
    }
}
