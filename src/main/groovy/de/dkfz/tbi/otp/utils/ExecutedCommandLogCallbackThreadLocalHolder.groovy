/*
 * Copyright 2011-2021 The OTP authors
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

/**
 * A {@link ThreadLocal} based solution to log the executed command/scripts and their produced results to an location defined earlier in the hierarchy.
 * Therefore the Callback interface {@link CommandLogCallback} is defined, which gets the information and should the do the logging.
 *
 * The main purpose is the correct logging of the commands send to cluster via the library BatchEuphoria to the corresponding workflow.
 */
class ExecutedCommandLogCallbackThreadLocalHolder {

    /**
     * Holds the threadLocals
     */
    private static final ThreadLocal<CommandLogCallback> THREAD_LOCAL = new ThreadLocal<CommandLogCallback>()

    /**
     * return the {@link CommandLogCallback} assosiated with the thread.
     */
    static CommandLogCallback get() {
        return THREAD_LOCAL.get()
    }

    /**
     * Binds the given commandLogCallback to the thread for the time of the execution of the code.
     *
     * After the execution of the code the commandLogCallback is removed from the thread.
     * Also it is not allow to bind multiple commandLogCallback to the thread.
     *
     * @param commandLogCallback The CommandLogCallback to be bind on the current thread.
     * @param code the code to be executed with the connected callback
     * @return the result of the code
     */
    static <V> V withCommandLogCallback(CommandLogCallback commandLogCallback, Closure<V> code) {
        assert commandLogCallback
        assert !get()

        try {
            THREAD_LOCAL.set(commandLogCallback)
            return code()
        } finally {
            THREAD_LOCAL.remove()
        }
    }
}
