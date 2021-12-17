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

import de.dkfz.tbi.otp.dataprocessing.*

/**
 * Calls ../CreateProcessingOptions.groovy with configuration for Docker setup.
 */

GroovyShell shell = new GroovyShell()
def rollout = shell.parse(new File('scripts/rollout/CreateProcessingOptions.groovy'))

Map<ProcessingOption.OptionName, String> processingOptions = [
        (ProcessingOption.OptionName.EMAIL_SENDER)                     : 'sender@otp.de',
        (ProcessingOption.OptionName.EMAIL_REPLY_TO)                   : 'reply-to@otp.de',
        (ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM)              : 'ticket-system@otp.de',
        (ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX)      : 'dev',
        (ProcessingOption.OptionName.PROCESSING_PRIORITY_DEFAULT_NAME) : 'MINIMAL',
        (ProcessingOption.OptionName.BASE_PATH_REFERENCE_GENOME) : '/otp/reference_genomes',
]

rollout.createProcessingOptions(ctx, processingOptions)
