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

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue
import de.dkfz.tbi.otp.utils.Entity

class IndelQualityControl implements Entity, QcTrafficLightValue {

    IndelCallingInstance indelCallingInstance

    String file

    @QcThresholdEvaluated
    int numIndels

    @QcThresholdEvaluated
    int numIns

    @QcThresholdEvaluated
    int numDels

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numSize4_10

    @QcThresholdEvaluated
    int numSize11plus

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numInsSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numInsSize4_10

    @QcThresholdEvaluated
    int numInsSize11plus

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numDelsSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numDelsSize4_10

    @QcThresholdEvaluated
    int numDelsSize11plus

    @QcThresholdEvaluated
    double percentIns

    @QcThresholdEvaluated
    double percentDels

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentSize4_10

    @QcThresholdEvaluated
    double percentSize11plus

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentInsSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentInsSize4_10

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentInsSize11plus

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentDelsSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentDelsSize4_10

    @QcThresholdEvaluated
    double percentDelsSize11plus


    static constraints = {
        file(validator: { OtpPath.isValidAbsolutePath(it) })
        indelCallingInstance unique: true
    }

    static belongsTo = [
        indelCallingInstance: IndelCallingInstance,
    ]

    static mapping = {
        file type: "text"
    }
}
