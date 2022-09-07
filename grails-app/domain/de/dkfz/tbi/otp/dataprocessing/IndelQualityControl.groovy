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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
class IndelQualityControl implements Entity {

    IndelCallingInstance indelCallingInstance

    String file

    int numIndels

    int numIns

    int numDels

    @SuppressWarnings("PropertyName")
    int numSize1_3

    @SuppressWarnings("PropertyName")
    int numSize4_10

    int numSize11plus

    @SuppressWarnings("PropertyName")
    int numInsSize1_3

    @SuppressWarnings("PropertyName")
    int numInsSize4_10

    int numInsSize11plus

    @SuppressWarnings("PropertyName")
    int numDelsSize1_3

    @SuppressWarnings("PropertyName")
    int numDelsSize4_10

    int numDelsSize11plus

    double percentIns

    double percentDels

    @SuppressWarnings("PropertyName")
    double percentSize1_3

    @SuppressWarnings("PropertyName")
    double percentSize4_10

    double percentSize11plus

    @SuppressWarnings("PropertyName")
    double percentInsSize1_3

    @SuppressWarnings("PropertyName")
    double percentInsSize4_10

    @SuppressWarnings("PropertyName")
    double percentInsSize11plus

    @SuppressWarnings("PropertyName")
    double percentDelsSize1_3

    @SuppressWarnings("PropertyName")
    double percentDelsSize4_10

    double percentDelsSize11plus

    static constraints = {
        file shared: "absolutePath"
        indelCallingInstance unique: true
    }

    static belongsTo = [
        indelCallingInstance: IndelCallingInstance,
    ]

    static Closure mapping = {
        file type: "text"
        indelCallingInstance index: "indel_quality_control_indel_calling_instance_idx"
    }
}
