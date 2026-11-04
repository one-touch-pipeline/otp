/*
 * Copyright 2011-2026 The OTP authors
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

import spock.lang.Specification
import spock.lang.Unroll

class SeqPlatformSpec extends Specification {

    @Unroll
    void "formatFullName, with model label '#model' and kit label '#kit', returns '#expected', the same as getFullName"() {
        given:
        SeqPlatform seqPlatform = new SeqPlatform(
                name: 'Illumina',
                seqPlatformModelLabel: model ? new SeqPlatformModelLabel(name: model) : null,
                sequencingKitLabel: kit ? new SequencingKitLabel(name: kit) : null,
        )

        expect:
        SeqPlatform.formatFullName('Illumina', model, kit) == expected
        seqPlatform.fullName == expected

        where:
        model     | kit    || expected
        'HiSeq X' | 'V2.5' || 'Illumina HiSeq X V2.5'
        'HiSeq X' | null   || 'Illumina HiSeq X unknown kit'
        null      | null   || 'Illumina unknown kit'
    }
}
