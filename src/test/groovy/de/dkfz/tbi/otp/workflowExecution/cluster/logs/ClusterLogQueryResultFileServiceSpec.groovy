/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.cluster.logs

import grails.testing.services.ServiceUnitTest

import java.nio.file.Files
import java.nio.file.Path

class ClusterLogQueryResultFileServiceSpec extends AbstractLogDirectoryServiceSpec implements ServiceUnitTest<ClusterLogQueryResultFileService> {

    @Override
    void setup() {
        configService.fixClockTo()
    }

    void "createAndGetLogDirectory, when path not exist, then create path and return it"() {
        given:
        Path expected = expectedPath()
        Path expectedFile = expectedFile()

        mockPathDoesNotExist(expected)

        when:
        Path path = service.logFileWithCreatingDirectory()

        then:
        path == expectedFile
    }

    void "createAndGetLogDirectory, when path exist, then reuse it and return it"() {
        given:
        Path expected = expectedPath()
        Path expectedFile = expectedFile()
        Files.createDirectories(expected)

        mockPathExist()

        when:
        Path path = service.logFileWithCreatingDirectory()

        then:
        path == expectedFile
    }

    private Path expectedPath() {
        return expectedPath(service.configService.currentDate, ClusterLogQueryResultFileService.CLUSTER_JOB_STATES_LOG_DIRECTORY)
    }

    private Path expectedFile() {
        Date date = service.configService.currentDate
        return expectedPath().resolve(date.format("HH-mm-ss'.txt'"))
    }
}
