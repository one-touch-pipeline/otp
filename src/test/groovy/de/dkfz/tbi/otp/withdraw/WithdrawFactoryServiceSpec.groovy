/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.withdraw

import groovy.transform.ToString
import spock.lang.Specification

import de.dkfz.tbi.otp.utils.Entity

class WithdrawFactoryServiceSpec extends Specification {

    @ToString
    class DummyEntity1 implements Entity {
    }

    @ToString
    class DummyEntity2a implements Entity {
    }

    @ToString
    class DummyEntity2b implements Entity {
    }

    void "test findProcessingWithdrawService returns correct service"() {
        given:
        ProcessingWithdrawService dummyService1 = Stub {
            supportedClasses >> [DummyEntity1]
        }
        ProcessingWithdrawService dummyService2 = Stub {
            supportedClasses >> [DummyEntity2a, DummyEntity2b]
        }
        WithdrawFactoryService factory = new WithdrawFactoryService()
        factory.withdrawServices = [dummyService1, dummyService2]

        DummyEntity1 dummyEntity1 = new DummyEntity1()
        DummyEntity2a dummyEntity2a = new DummyEntity2a()
        DummyEntity2b dummyEntity2b = new DummyEntity2b()

        expect:
        factory.findProcessingWithdrawService(dummyEntity1) == dummyService1
        factory.findProcessingWithdrawService(dummyEntity2a) == dummyService2
        factory.findProcessingWithdrawService(dummyEntity2b) == dummyService2
    }

    void "test findProcessingWithdrawService returns null for unsupported entity"() {
        given:
        ProcessingWithdrawService dummyService1 = Stub {
            supportedClasses >> [DummyEntity1]
        }
        WithdrawFactoryService factory = new WithdrawFactoryService()
        factory.withdrawServices = [dummyService1]

        DummyEntity2a dummyEntity2a = new DummyEntity2a()

        expect:
        factory.findProcessingWithdrawService(dummyEntity2a) == null
    }
}
