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
package de.dkfz.tbi.otp.workflowExecution

/**
 * This enum defines different types of configuration to help with filtering.
 *
 * It should not be used to switch any hard logic and only for loose filtering in the GUI.
 * This is because every config should be written with a common structured specification,
 * and theoretically be interchangeable between these types - disregarding unhandled
 * parameters.
 *
 * There are no expected formats or parameters for a type.
 */
enum SelectorType {

    /** Generic workflow configurations and parameters */
    GENERIC,

    /** Resource configuration for workflow jobs */
    RESOURCE,

    /** Bed file specific configuration */
    BED_FILE,

    /** Adapter file specific configurations */
    ADAPTER_FILE,

    /** Reverse sequence specific configuration */
    REVERSE_SEQUENCE,

    /** Default values that cannot be modified by users */
    DEFAULT_VALUES,
}
