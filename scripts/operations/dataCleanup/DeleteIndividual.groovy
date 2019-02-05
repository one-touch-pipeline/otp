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

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*

// input area
//----------------------

String pid

boolean check = true


//script area
//-----------------------------

assert pid

DataSwapService dataSwapService = ctx.dataSwapService

String baseOutputDir = "${ConfigService.getInstance().getScriptOutputPath()}/sample_swap/"

Individual.withTransaction {
    List<String> allFilesToRemove = dataSwapService.deleteIndividual(pid, check)

    File deleteFileCmd = dataSwapService.createFileSafely(baseOutputDir, "Delete_${pid}.sh")
    File deleteFileSecondUserCmd = dataSwapService.createFileSafely(baseOutputDir, "DeleteSecondUser_${pid}.sh")

    deleteFileCmd << allFilesToRemove[0]
    deleteFileSecondUserCmd << allFilesToRemove[1]

    assert false
}
