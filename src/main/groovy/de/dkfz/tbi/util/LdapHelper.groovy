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
package de.dkfz.tbi.util

import de.dkfz.tbi.otp.config.ConfigService

import javax.naming.directory.Attributes
import java.time.Instant
import java.time.LocalDateTime

class LdapHelper {

    static long convertAdTimestampToUnixTimestampInMs(long windowsEpoch) {
        // http://meinit.nl/convert-active-directory-lastlogon-time-to-unix-readable-time
        // in milliseconds because Date.getTime() also returns milliseconds
        return ((windowsEpoch / 10000000) - 11644473600) * 1000
    }

    static LocalDateTime getLocalDateTimeFromMs(long ms) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ConfigService.instance.timeZoneId)
    }

    @SuppressWarnings("UnusedMethodParameter")
    static boolean getIsDeactivatedFromAttributes(Attributes a) {
        // temporary: disabled until it is clear what has to be checked
        return false
        /*
        long accountExpires = (a.get(LdapKey.ACCOUNT_EXPIRES)?.get()?.toString()?.toLong()) ?: 0
        return convertAdTimestampToUnixTimestampInMs(accountExpires) < new Date().time
         */
    }
}
