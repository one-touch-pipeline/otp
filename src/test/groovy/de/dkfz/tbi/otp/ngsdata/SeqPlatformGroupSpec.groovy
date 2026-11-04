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

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.project.Project

class SeqPlatformGroupSpec extends Specification {

    void "formatDisplayName, for a project and seq type specific group, returns the same as toString"() {
        given:
        SeqType seqType = new SeqType(displayName: 'WGS', libraryLayout: SequencingReadType.PAIRED, singleCell: false)
        SeqPlatformGroup group = new SeqPlatformGroup(
                mergingCriteria: new MergingCriteria(project: new Project(name: 'project'), seqType: seqType),
                seqPlatforms: [new SeqPlatform(name: 'B'), new SeqPlatform(name: 'A')] as Set,
        )
        String expected = "'project WGS PAIRED bulk' seq platform group with 'A unknown kit', 'B unknown kit'"

        expect:
        SeqPlatformGroup.formatDisplayName('project', 'WGS PAIRED bulk', ['B unknown kit', 'A unknown kit']) == expected
        group.toString() == expected
    }

    void "formatDisplayName, for an OTP global group without seq platforms, returns the same as toString"() {
        given:
        SeqPlatformGroup group = new SeqPlatformGroup()
        String expected = "'OTP global' seq platform group with  no seq platforms"

        expect:
        SeqPlatformGroup.formatDisplayName(null, null, []) == expected
        group.toString() == expected
    }

    void "formatDisplayName, does not change the order of the given seq platform names"() {
        given:
        List<String> seqPlatformFullNames = ['B', 'A']

        when:
        SeqPlatformGroup.formatDisplayName(null, null, seqPlatformFullNames)

        then:
        seqPlatformFullNames == ['B', 'A']
    }
}
