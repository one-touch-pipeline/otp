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
package de.dkfz.tbi.otp.job.scheduler

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.xml.MarkupBuilder
import org.apache.commons.io.FileUtils

import de.dkfz.tbi.otp.config.ConfigService

import static org.springframework.util.Assert.notNull

/**
 * Service for logging exceptions to files
 *
 * This Service gets exceptions from jobs and stores them in files.
 * Each job should write each different exception type to a dedicated file.
 * As file name the MD5 sum of job's name and the occurred are taken to ensure this.
 * The content of the files is organized as xml to have a clear structure.
 * If a specific exception occurs several times for each job only the timestamp
 * with the point in time when the exception occurred is appended as exception itself
 * is by definition of the file name completely equivalent to the already stored.
 */
@CompileDynamic
@Transactional
class ErrorLogService {

    ConfigService configService

    File getStackTracesFile(final String stackTraceIdentifier) {
        notNull stackTraceIdentifier, "stackTraceIdentifier must not be null."
        return new File(configService.stackTracesDirectory, stackTraceIdentifier + ".xml")
    }

    /**
     * The central method coordinating the service's functionality.
     *
     * The method makes the functionality the service provides publicly available.
     * The MD5 sum of the stacktrace and the error message is calculated and taken as name of the file storing
     * the stacktrace of occurred exception.
     * Here are the methods triggered forming the xml around contents to be stored
     * and writing the file to the file system.
     *
     * @param jobClassName The name of the job of which an exception is to be stored
     * @param thrownException The thrown exception
     * @return Unique hash of caught exception.
     */
    String log(Throwable thrownException) {
        String exceptionElements = thrownException.message
        thrownException.stackTrace.each {
            exceptionElements += it.toString()
        }
        String md5sum = (exceptionElements).encodeAsMD5()
        contentToXml(thrownException, getStackTracesFile(md5sum))
        return md5sum
    }

    /**
     * Retrieves the stacktrace identified by given identifier.
     * @param identifier The stacktrace's identifier
     * @return The stacktrace if found otherwise an exception is thrown with the reason why the stacktrace can not be returned
     */
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    String loggedError(String identifier) {
        File stacktraceFile = getStackTracesFile(identifier)
        if (!stacktraceFile.isFile()) {
            throw new RuntimeException("${stacktraceFile.path} is not a file ")
        }
        try {
            def records = new XmlSlurper().parse(stacktraceFile)
            return records.stacktrace.text()
        } catch (Exception e) {
            throw new RuntimeException("The XML file could not be parsed", e)
        }
    }

    /**
     * Writes the exception to a file
     *
     * The exception is stored to a file which is written to the file system
     * to a configurable directory.
     *
     * @param file The file with its full path
     * @param content The content to be stored
     */
    private void writeToFile(File file, String content) {
        file.withWriter { out -> out.println content }
    }

    /**
     * Transforms the exception to a string
     *
     * @param thrownException The exception to store
     * @return String containing the Exception
     */
    private String stackToString(Throwable thrownException) {
        try {
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            thrownException.printStackTrace(pw)
            return sw.toString()
        } catch (Exception e) {
            return e.toString()
        }
    }

    /**
     * Wraps a new combination of job name and exception in xml
     *
     * The method is called when a new combination of job and exception
     * occurs and therefore a file containing this combination is not yet existent.
     * It wraps the exception's stacktrace together with the job's name and a timestamp
     * in xml.
     *
     * @param thrownException The ThrownException to be stored
     * @return String with complete xml file.
     */
    private void contentToXml(final Throwable thrownException, final File xmlFile) {
        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        String stackString = stackToString(thrownException)
        xml.stacktraceElement(exceptionMessage: thrownException.message) {
            stacktrace(stackString)
            timestamp(new Date())
        }
        FileUtils.forceMkdir(xmlFile.parentFile)
        writeToFile(xmlFile, writer.toString())
    }
}
