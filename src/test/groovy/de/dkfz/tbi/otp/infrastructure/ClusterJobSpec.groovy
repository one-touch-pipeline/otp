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
package de.dkfz.tbi.otp.infrastructure

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

import static de.dkfz.tbi.otp.utils.HelperUtils.getUniqueString

class ClusterJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ClusterJob,
        ]
    }

    void "test findByClusterJobIdentifier"() {
        given:
        ClusterJob clusterJob = DomainFactory.createClusterJob()
        ClusterJobIdentifier identifier = new ClusterJobIdentifier(clusterJob)
        DomainFactory.createClusterJob(
                realm: identifier.realm,
                clusterJobId: getUniqueString(),
        )
        DomainFactory.createClusterJob(
                realm: DomainFactory.createRealm(),
                clusterJobId: identifier.clusterJobId,
        )

        expect:
        clusterJob.findByClusterJobIdentifier(identifier, clusterJob.processingStep) == clusterJob
    }
}
