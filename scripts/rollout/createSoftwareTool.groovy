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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.codehaus.groovy.grails.plugins.web.taglib.*

String programName = ''
String programVersion = ''

//****************************************

assert programName : 'Please provide a program name'
assert programVersion : 'Please provide a program version'

SoftwareTool softwareTool = CollectionUtils.atMostOneElement(
        SoftwareTool.findAllByProgramNameAndProgramVersionAndType(programName, programVersion, SoftwareTool.Type.BASECALLING))

assert !softwareTool: 'The software tool already exist'

SoftwareTool.withTransaction {
    softwareTool = new SoftwareTool(
            programName: programName,
            programVersion: programVersion,
            type: SoftwareTool.Type.BASECALLING,
    )
    println softwareTool.save(flush: true)

    println 'Please add an alias on ' + ctx.getBean(ApplicationTagLib).createLink(
            controller: 'softwareTool', action: 'list', absolute: 'true')
}
''
