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

package rollout.docker

import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * Calls ../CreateDefaultRealm.groovy with configuration for Docker setup.
 */

GroovyShell shell = new GroovyShell()
def rollout = shell.parse(new File('scripts/rollout/CreateDefaultRealm.groovy'))

Realm realm = new Realm([
        name                       : 'dev_realm',
        jobScheduler               : Realm.JobScheduler.LSF,
        host                       : 'cluster-host',
        port                       : 2222,
        timeout                    : 0,
        defaultJobSubmissionOptions: '{"WALLTIME":"PT48H", "MEMORY":"1g"}',
])

rollout.createDefaultRealm(ctx, realm)
