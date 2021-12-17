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
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class ExternallyProcessedMergedBamFileSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternallyProcessedMergedBamFile,
        ]
    }

    private Map createMap() {
        return [
                fileName           : 'bamfile.bam',
                importedFrom       : '/tmp/bamfile',
                insertSizeFile     : null,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum             : DomainFactory.DEFAULT_MD5_SUM,
                maximumReadLength  : 5,
                workPackage        : new ExternalMergingWorkPackage([
                        pipeline: new Pipeline([
                                name: Pipeline.Name.EXTERNALLY_PROCESSED,
                        ]),
                ]),
        ]
    }

    void "all constraint fine, create object"() {
        given:
        ExternallyProcessedMergedBamFile bamFile = new ExternallyProcessedMergedBamFile(createMap())

        when:
        bamFile.validate()

        then:
        !bamFile.errors.hasErrors()
    }

    @Unroll
    void "constraint, if property '#property' is '#value', then validation should not fail"() {
        given:
        ExternallyProcessedMergedBamFile bamFile = new ExternallyProcessedMergedBamFile(createMap())
        bamFile[property] = value

        when:
        bamFile.validate()

        then:
        !bamFile.errors.hasErrors()

        where:
        property              | value
        'importedFrom'        | null
        'insertSizeFile'      | null
        'insertSizeFile'      | 'tmp/tmp'
        'fileOperationStatus' | AbstractMergedBamFile.FileOperationStatus.DECLARED
        'maximumReadLength'   | null
    }

    @Unroll
    void "constraint, if property '#property' is '#value', then validation should fail for '#constraint'"() {
        given:
        ExternallyProcessedMergedBamFile bamFile = new ExternallyProcessedMergedBamFile(createMap())
        bamFile[property] = value

        expect:
        TestCase.assertAtLeastExpectedValidateError(bamFile, property, constraint, value)

        where:
        property            | constraint                 | value
        'insertSizeFile'    | 'blank'                    | ''
        'insertSizeFile'    | 'maxSize.exceeded'         | '0'.padRight(2000, '0')
        'insertSizeFile'    | 'validator.relative.path'  | '/tmp'
        'insertSizeFile'    | 'validator.relative.path'  | 'tmp//tmp'
        'insertSizeFile'    | 'validator.relative.path'  | 'tmp&tmp'

        'fileName'          | 'nullable'                 | null
        'fileName'          | 'blank'                    | ''
        'fileName'          | 'validator.path.component' | '/tmp'
        'fileName'          | 'validator.path.component' | 'tmp/tmp'
        'fileName'          | 'validator.path.component' | 'tmp&tmp'

        'importedFrom'      | 'blank'                    | ''
        'importedFrom'      | 'validator.absolute.path'  | 'tmp'
        'importedFrom'      | 'validator.absolute.path'  | '/tmp//tmp'
        'importedFrom'      | 'validator.absolute.path'  | '/tmp&tmp'

        'maximumReadLength' | 'min.notmet'               | -5
    }
}
