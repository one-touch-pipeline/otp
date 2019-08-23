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
package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ProcessParameter implements Entity {
    String value
    String className
    Process process

    static constraints = {
        process(nullable: false, unique: true)
        className(nullable: false, validator: { String name ->
            ProcessParameterObject.isAssignableFrom(ClassLoader.getSystemClassLoader().loadClass(name)) &&
            !name.contains('$')
        })
    }

    static mapping = {
        process index: "process_parameter_process_idx"
    }

    /**
     * Retrieves the domain object instance this ProcessParameter points to in case className is not null.
     *
     * If the object does not exists, this method returns null.
     * @return The domain object instance or null
     */
    ProcessParameterObject toObject() {
        if (className) {
            List resultList = ProcessParameter.executeQuery("FROM ${className} WHERE id=${value}".toString())
            if (resultList) {
                return exactlyOneElement(resultList)
            }
        }
        return null
    }
}
