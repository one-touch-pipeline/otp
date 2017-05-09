package de.dkfz.tbi.otp.utils

import groovy.transform.*


@ToString
class Result<T, U> {
    private T result
    private U error

    private Result() {}

    static Result<T, U> success(T value) {
        Result<T, U> r = new Result<T, U>()
        r.result = value
        return r
    }

    static Result<T, U> ofNullable(T value, U error) {
        Result<T, U> r = new Result<T, U>()
        if (value == null) {
            r.error = error
        } else {
            r.result = value
        }
        return r
    }

    static Result<T, U> failure(U error) {
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
