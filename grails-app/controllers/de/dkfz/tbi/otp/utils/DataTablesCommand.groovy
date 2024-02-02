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

import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.ToString

class DataTablesCommand {
    int draw
    int start
    int length

    boolean isPagingEnabled() {
        return length != -1
    }

    /**
     * DataTables passes the ordering information in a way that can't be automatically bound to a command object,
     * so if you want to use ordering information, you need to pass the params object the processParams method first
     */
    List<Order> orderList

    void processParams(GrailsParameterMap parameterMap) {
        Map<Integer, Order> map = [:].withDefault { new Order() }
        parameterMap.findAll { String k, v ->  k.startsWith("order") }.each { String key, String value ->
            List keyFields = key.replace(']', '').split(/\[/)
            Order order = map[keyFields[1] as int]
            if (keyFields[2] == "column") {
                order.column = value as int
            } else if (keyFields[2] == "dir") {
                order.direction = Order.Dir.valueOf(value)
            }
        }
        orderList = map.sort { it.key }*.value
    }

    Map getDataToRender(List data, Integer recordsTotal = null, Integer recordsFiltered = null, Map<String, Object> additionalData = null,
                        String error = null) {
        int totalRecords = (recordsTotal != null) ? recordsTotal : data.size()
        Map result = [
                draw: draw,
                data: data,
                recordsTotal: totalRecords,
                recordsFiltered: (recordsFiltered != null) ? recordsFiltered : totalRecords,
        ]
        if (error) {
            result["error"] = error
        }
        if (additionalData) {
            assert !additionalData.keySet().any { it in ["draw", "data", "recordsTotal", "recordsFiltered", "error"] }
            result.putAll(additionalData)
        }
        return result
    }

    @ToString
    class Order {
        int column
        Dir direction

        @SuppressWarnings("FieldName")
        enum Dir {
            asc, desc
        }
    }
}
