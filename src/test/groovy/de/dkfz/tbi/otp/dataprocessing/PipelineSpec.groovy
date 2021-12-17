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

class PipelineSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Pipeline
        ]
    }

    @Unroll
    void "test name type constraint"() {
        given:
        Pipeline pipeline = new Pipeline(
                name: name,
                type: type,
        )

        expect:
        valid == pipeline.validate()

        where:
        name                               | type                    || valid
        Pipeline.Name.RODDY_SNV            | Pipeline.Type.SNV       || true
        Pipeline.Name.RODDY_SNV            | Pipeline.Type.ALIGNMENT || false
        Pipeline.Name.PANCAN_ALIGNMENT     | Pipeline.Type.SNV       || false
        Pipeline.Name.PANCAN_ALIGNMENT     | Pipeline.Type.ALIGNMENT || true
        Pipeline.Name.RODDY_RNA_ALIGNMENT  | Pipeline.Type.SNV       || false
        Pipeline.Name.RODDY_RNA_ALIGNMENT  | Pipeline.Type.ALIGNMENT || true
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Type.ALIGNMENT || true
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Type.SNV       || false
    }
}
