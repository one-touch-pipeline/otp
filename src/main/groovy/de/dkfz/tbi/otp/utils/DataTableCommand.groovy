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
package de.dkfz.tbi.otp.utils

import grails.validation.Validateable

/**
 * SuppressWarnings PropertyName: Unclear if the property names play a larger role for DataTables, do not change during major violation cleanup.
 */
class DataTableCommand implements Validateable {

    int iDisplayStart = 0
    int iDisplayLength = 10

    @SuppressWarnings("PropertyName")
    int iSortCol_0 = 0

    @SuppressWarnings("PropertyName")
    String sSortDir_0 = "asc"
    String sEcho
    String sSearch = ""

    static constraints = {
    }

    void setIDisplayStart(int start) {
        this.iDisplayStart = start
    }

    void setIDisplayLength(int length) {
        if (length <= 100) {
            this.iDisplayLength = length
        } else {
            this.iDisplayLength = 100
        }
    }

    Map dataToRender() {
        return [
            sEcho: this.sEcho,
            offset: this.iDisplayStart,
            iSortCol_0: this.iSortCol_0,
            sSortDir_0: this.sSortDir_0,
            aaData: [],
        ]
    }

    boolean getSortOrder() {
        return this.sSortDir_0 == "asc"
    }
}
