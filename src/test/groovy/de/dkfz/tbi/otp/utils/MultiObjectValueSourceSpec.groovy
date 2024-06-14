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

import grails.testing.gorm.DataTest
import groovy.transform.TupleConstructor
import spock.lang.Specification

class MultiObjectValueSourceSpec extends Specification implements DataTest {

    @TupleConstructor
    class DummyClassA {
        Object common
        Object rare
        Object unique
    }

    @TupleConstructor
    class DummyClassB {
        Object common
        Object rare
    }

    @TupleConstructor
    class DummyClassC {
        Object common
    }

    void "getPropertyByNameHelper, properly differentiates between Map and Object"() {
        expect:
        MultiObjectValueSource.getPropertyByNameHelper(target, "common") == "value"

        where:
        target << [
                new DummyClassC("value"),
                [common: "value"],
        ]
    }

    void "getByFieldName, returns the first non-null value or null"() {
        given:
        MultiObjectValueSource source = new MultiObjectValueSource(
                new DummyClassA("commonA", null, null),
                new DummyClassB("commonB", "rareB"),
                new DummyClassC("commonC"),
                [common: "mapCommon", mapOnly: "mapOnlyValue"],
        )

        expect:
        source.getByFieldName(name) == expected

        where:
        name      | expected
        "mapOnly" | "mapOnlyValue"
        "common"  | "commonA"
        "rare"    | "rareB"
        "unique"  | null
    }

    void "getByFieldName, checks explicitly for null values, not false evaluating"() {
        given:
        MultiObjectValueSource source = new MultiObjectValueSource(
                new DummyClassC(expected),
                [common: "mapCommon"]
        )

        expect:
        source.getByFieldName("common") == expected

        where:
        expected << [false, "", 0]
    }

    void "getAllByFieldName, returns a list of values"() {
        given:
        List<Object> expected = [
                "string",
                ["item1", "item2"],
                [key1: "value1", key2: "value2"],
                1,
        ]
        MultiObjectValueSource source = new MultiObjectValueSource(
                new DummyClassA(expected[0], null, null),
                new DummyClassB(expected[1], "rareB"),
                new DummyClassC(expected[2] as Map),
                [common: expected[3], mapOnly: "mapOnlyValue"],
        )

        expect:
        source.getAllByFieldName("common") == expected
    }
}
