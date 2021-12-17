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
package de.dkfz.tbi.otp.parser

import groovy.transform.TupleConstructor

@TupleConstructor
enum SampleIdentifierParserBeanName {
    NO_PARSER('', 'No Parser'),
    DEEP('deepSampleIdentifierParser', 'DEEP'),
    HIPO('hipoSampleIdentifierParser', 'HIPO'),
    HIPO2('hipo2SampleIdentifierParser', 'HIPO2'),
    HIPO2_SPL('hipo2SamplePreparationLabSampleIdentifierParser', 'HIPO2_SPL'),
    INFORM('informSampleIdentifierParser', 'INFORM'),
    OE0290_EORTC('OE0290_EORTC_SampleIdentifierParser', 'OE0290_EORTC'),
    SIMPLE('simpleProjectIndividualSampleTypeParser', 'Simple'),
    ITCC_4P('iTCC_4P_Parser', 'ITCC-4P'),
    PEDION('pedionParser', 'PeDiOn'),
    COVID19('covid19SampleIdentifierParser', 'Covid-19'),

    final String beanName
    final String displayName
}
