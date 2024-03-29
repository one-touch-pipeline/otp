/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.project.projectRequest

import groovy.transform.TupleConstructor

@TupleConstructor
enum ProjectRequestAction {

    SUBMIT_VIEW("submitView", "Submit", "primary"),
    SUBMIT_INDEX("submitIndex", "Submit", "primary"),
    SAVE_INDEX("saveIndex", "Save", "light"),
    SAVE_VIEW("saveView", "Save", "light"),
    SAVE_DRAFT("saveIndex", "Save as Draft", "light"),
    PASS_ON("passOn", "Pass On", "primary"),
    APPROVE("approve", "Approve", "primary"),
    EDIT("edit", "Edit", "light"),
    REJECT("reject", "Reject", "warning"),
    CREATE("create", "Create", "primary"),
    DELETE("delete", "Delete", "danger")

    String action
    String text
    String styling
}
