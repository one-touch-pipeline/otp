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
package de.dkfz.tbi.otp.utils

import groovy.transform.ToString

@ToString
class Result<T, U> {
    private T result
    private U error

    private Result() { }

    static <T, U> Result<T, U> success(T value) {
        Result<T, U> r = new Result<T, U>()
        r.result = value
        return r
    }

    static <T, U> Result<T, U> ofNullable(T value, U error) {
        Result<T, U> r = new Result<T, U>()
        if (value == null) {
            r.error = error
        } else {
            r.result = value
        }
        return r
    }

    static <T, U> Result<T, U> failure(U error) {
        Result<T, U> r = new Result<T, U>()
        r.error = error
        return r
    }

    boolean isSuccess() {
        return result
    }

    boolean isFailure() {
        return error
    }

    T getValue() {
        return result
    }
    U getError() {
        return error
    }

    Result<T, U> onSuccess(Closure function) {
        if (result != null) {
            function.call(result)
        }
        return this
    }

    Result<T, U> onFailure(Closure function) {
        if (error) {
            function.call(error)
        }
        return this
    }

    Result<T, U> onBoth(Closure function) {
        function.call(this)
        return this
    }

    Result<T, U> ensure(Closure<Boolean> function, U error) {
        if (result != null) {
            if (!function.call(result)) {
                this.error = error
                this.result = null
            }
        }
        return this
    }

    def <V> Result<V, U> map(Closure<V> function) {
        Result<V, U> r = new Result<V, U>()
        if (result != null) {
            r.result = function.call(result)
        } else {
            r.error = error
        }
        return r
    }
}
