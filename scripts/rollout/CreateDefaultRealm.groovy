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

package rollout

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*

/**
 * @see Realm
 * @see ProcessingOption
 *
 * Basic script for creating a default realm.
 *
 * The parameters for the processing options are defined in the form
 * of a {@code Map<ProcessingOption.OptionName, Object>} and are unsed to create
 * the desired processing options by the createProcessingOptions function.
 */

// inputs
// --------------------------------------------------------

String realmName = 'some-realm-name'
Realm.JobScheduler jobScheduler = Realm.JobScheduler.LSF
String host = 'invalid-host-name'
int port = -1
timeout = -1
String defaultJobSubmissionOptions = ''

//
// --------------------------------------------------------

Realm realm = new Realm([
        name                       : realmName,
        jobScheduler               : jobScheduler,
        host                       : host,
        port                       : port,
        timeout                    : timeout,
        defaultJobSubmissionOptions: defaultJobSubmissionOptions,
])

/**
 * Create a Realm and set it as default in the processing options.
 *
 * @param ctx ctx The AnnotationConfigEmbeddedWebApplicationContext, necessary if the function
 * is called from another script.
 * @param realm The realm to create
 * @return a String indicating a successfully creation.
 */
static String createDefaultRealm (AnnotationConfigEmbeddedWebApplicationContext ctx, Realm realm){
    Realm.withTransaction {
        assert realm.save(flush: true)
        ctx.processingOptionService.createOrUpdate(ProcessingOption.OptionName.REALM_DEFAULT_VALUE, realm.name)
        return "$realm.name created"
    }
}

createDefaultRealm(ctx, realm)
