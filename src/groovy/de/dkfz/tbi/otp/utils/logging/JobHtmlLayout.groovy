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

package de.dkfz.tbi.otp.utils.logging

import org.apache.log4j.HTMLLayout
import org.apache.log4j.Level
import org.apache.log4j.helpers.Transform
import org.apache.log4j.spi.LocationInfo
import org.apache.log4j.spi.LoggingEvent

/**
 * This class is provides a groovy re-implementation of log4j's {@link HTMLLayout#format(LoggingEvent)}.
 * This {@link #format(LoggingEvent)}-method only conceptual difference is the minor tweak of
 * adding "<pre>" tags around the logged message, since in our case, that is usually
 * multiline <code>stdout</code> output
 *
 * The original implementation was taken from Log4j 1.2.17, which is licensed under
 * Apache License, Version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 * This license expressly allows creation and distribution of derivative works, with
 * attribution conditions and notifications of change.
 */
class JobHtmlLayout extends HTMLLayout {

    private StringBuffer msgBuffer = new StringBuffer(BUF_SIZE);

    /**
     * LICENSE_ISSUE
     *
     * Provides an alternative implementation of the log4j's {@link HTMLLayout#format(LoggingEvent)}.
     * Basically everything is the same, except we add HTML pre-tags around the message for better reading.
     */
    @Override
    String format(LoggingEvent event) {
        if (msgBuffer.capacity() > MAX_CAPACITY) {
            msgBuffer = new StringBuffer(BUF_SIZE)
        } else {
            msgBuffer.setLength(0)
        }

        msgBuffer << "<tr><td>${new Date(event.timeStamp).format("EEE, d MMM yyyy HH:mm:ss.SSS Z")}</td>"

        String escapedThread = Transform.escapeTags(event.getThreadName())
        msgBuffer << "<td title=\"${escapedThread} thread\">${escapedThread}</td>"

        String escapedLevel = Transform.escapeTags(String.valueOf(event.getLevel()))
        msgBuffer << "<td title=\"Level\">"
        if (event.getLevel().equals(Level.DEBUG)) {
            msgBuffer << "<font color=\"#339933\">${escapedLevel}</font>"
        } else if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
            msgBuffer << "<font color=\"#993300\"><strong>${escapedLevel}</strong></font>"
        } else {
            msgBuffer << escapedLevel
        }
        msgBuffer << "</td>"

        String escapedLogger = Transform.escapeTags(event.getLoggerName())
        msgBuffer << "<td title=\"${escapedLogger} category\">${escapedLogger.split("\\.").last()}</td>"

        if (locationInfo) {
            LocationInfo locInfo = event.getLocationInformation()
            msgBuffer << "<td>${Transform.escapeTags(locInfo.getFileName())}:${locInfo.getLineNumber()}</td>"
        }

        msgBuffer << "<td title=\"Message\"><pre>${Transform.escapeTags(event.getRenderedMessage())}</pre></td></tr>"

        if (event.getNDC() != null) {
            msgBuffer << "<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : xx-small;\" colspan=\"6\" title=\"Nested Diagnostic Context\">"
            msgBuffer << "NDC: ${Transform.escapeTags(event.getNDC())}"
            msgBuffer << "</td></tr>"
        }

        String[] s = event.getThrowableStrRep()
        if (s != null) {
            msgBuffer << "<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : xx-small;\" colspan=\"6\">"
            appendThrowableAsHTML(s, msgBuffer)
            msgBuffer.append << "</td></tr>"
        }

        return msgBuffer.toString()
    }
}
