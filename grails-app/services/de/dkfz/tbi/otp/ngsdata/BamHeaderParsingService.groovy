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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional

@Transactional
class BamHeaderParsingService {

    SeqTrack seqTrackFromTokens(List<String> tokens) {
        SeqTrack track = null
        track = parseTokensV1(tokens)
        if (track) {
            return track
        }
        track = parseTokensV2(tokens)
        if (track) {
            return track
        }
        track = parseTokensV3(tokens)
        if (track) {
            return track
        }
        track = parseTokensV4(tokens)
        if (track) {
            return track
        }
        track = parseTokensV5(tokens)
        if (track) {
            return track
        }
    }

    /*
     * parse format ID:runName_s_lane
     */
    private SeqTrack parseTokensV1(List<String> tokens) {
        for (String token in tokens) {
            if (token.startsWith("ID:")) {
                int sepId = token.indexOf("_s_")
                if (sepId > -1) {
                    String runName = token.substring(3, sepId)
                    String lane = token.substring(token.lastIndexOf("_") + 1)
                    log.info("${runName} ${lane}")
                    return getSeqTrack(runName, lane)
                }
            }
        }
        return null
    }

    private SeqTrack parseTokensV2(List<String> tokens) {
        for (String token in tokens) {
            if (token.startsWith("ID:")) {
                int sepId = token.indexOf("_lane")
                if (sepId > -1) {
                    String runName = token.substring(3, sepId)
                    String lane = token.substring(sepId + 5, sepId + 6)
                    log.info("${runName} ${lane}")
                    return getSeqTrack(runName, lane)
                }
            }
        }
        return null
    }

    private SeqTrack parseTokensV3(List<String> tokens) {
        String runName
        String lane
        for (String token in tokens) {
            if (token.startsWith("SM:")) {
                runName = token.substring(3)
            }
            if (token.startsWith("ID:")) {
                int idxFrom = token.indexOf("_") + 1
                int idxTo = token.indexOf(".")
                if (idxTo > 0) {
                    lane = token.substring(idxFrom, idxTo)
                } else {
                    lane = token.substring(idxFrom)
                }
            }
        }
        log.info("${runName} ${lane}")
        if (runName && lane) {
            return getSeqTrack(runName, lane)
        }
        return null
    }

    private SeqTrack parseTokensV4(List<String> tokens) {
        for (String token in tokens) {
            if (token.contains("DS:")) {
                token = token.substring(token.indexOf("DS:"))
            }
            if (token.startsWith("DS:")) {
                String runName = token.substring(3)
                int idx = runName.indexOf("_FC") + 3
                if (idx < runName.size()) {
                    String lane = runName.substring(idx)
                    log.info("${runName} ${lane}")
                    return getSeqTrack(runName, lane)
                }
            }
        }
        return null
    }

    private SeqTrack parseTokensV5(List<String> tokens) {
        for (String token in tokens) {
            if (token.startsWith("ID:")) {
                int sepId = token.indexOf("_L")
                if (sepId > -1) {
                    String runName = token.substring(3, sepId)
                    String lane = token.substring(sepId + 2)
                    int laneNo = lane as int
                    log.info("${runName} ${laneNo}")
                    return getSeqTrack(runName, laneNo as String)
                }
            }
        }
        return null
    }

    private SeqTrack getSeqTrack(String runName, String lane) {
        String runString = runName
        if (runName.startsWith("run")) {
            runString = runName.substring(3)
        }
        Run run = Run.findByName(runString)
        SeqTrack seqTrack = SeqTrack.findByRunAndLaneId(run, lane)
        return seqTrack
    }
}
