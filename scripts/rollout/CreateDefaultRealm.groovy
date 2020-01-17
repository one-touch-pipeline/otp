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

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import grails.util.*
import de.dkfz.tbi.otp.dataprocessing.*

String realmName

assert realmName

ProcessingOptionService processingOptionService = ctx.ProcessingOptionService

//create default realm
Realm.withTransaction {
    Realm realm = new Realm([
            name                       : realmName,
            jobScheduler               : Realm.JobScheduler.PBS,
            host                       : 'test.host.invalid',
            port                       : -1,
            timeout                    : -1,
            defaultJobSubmissionOptions: '',
    ])

    assert realm.save(flush: true)

    processingOptionService.createOrUpdate(ProcessingOption.OptionName.REALM_DEFAULT_VALUE, realmName)
}
''
