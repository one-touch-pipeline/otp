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

module("format-timespan");
test("msec", function () {
    equal($.otp.workflows.formatTimespan(0), "0 msec");
    equal($.otp.workflows.formatTimespan(999), "999 msec");
});
test("sec", function () {
    equal($.otp.workflows.formatTimespan(1000), "1 sec 0 msec");
    equal($.otp.workflows.formatTimespan(59999), "59 sec 999 msec");
});
test("min", function () {
    equal($.otp.workflows.formatTimespan(60000), "1 min 0 sec");
    equal($.otp.workflows.formatTimespan(3599999), "59 min 59 sec");
});
test("hour", function () {
    equal($.otp.workflows.formatTimespan(3600000), "1 h 0 min");
    equal($.otp.workflows.formatTimespan(86399999), "23 h 59 min");
});
test("day", function () {
    equal($.otp.workflows.formatTimespan(86400000), "1 day(s) 0 h");
    equal($.otp.workflows.formatTimespan(950399000), "10 day(s) 23 h");
});
