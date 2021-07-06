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

/**
 * Abstract base class for {@link EndStateAwareJob}s.
 *
 * @see EndStateAwareJob
 */
abstract class AbstractEndStateAwareJobImpl extends AbstractJobImpl implements EndStateAwareJob {

    private ExecutionState endState = null

    /**
     * Can be used by an implementing Job to set the Job as failed.
     * @see #succeed
     * @deprecated Throw an exception with a meaningful message instead.
     */
    @Deprecated
    protected final void fail() {
        this.endState = ExecutionState.FAILURE
    }

    /**
     * Can be used by an implementing Job to set the Job as succeeded.
     * @see #fail
     */
    protected final void succeed() {
        this.endState = ExecutionState.SUCCESS
    }

    @Override
    final ExecutionState getEndState() throws InvalidStateException {
        if (!endState) {
            throw new InvalidStateException("EndState accessed without end state being set")
        }
        if (getState() != State.FINISHED) {
            throw new InvalidStateException("EndState accessed but not in finished state")
        }
        return endState
    }
}
