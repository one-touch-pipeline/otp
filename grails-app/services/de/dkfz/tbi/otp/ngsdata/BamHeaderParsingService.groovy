package de.dkfz.tbi.otp.ngsdata

import java.util.List;

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
        for(String token in tokens) {
            if (token.startsWith("ID:")) {
                int sepId = token.indexOf("_s_")
                if (sepId > -1) {
                    String runName = token.substring(3, sepId);
                    String lane = token.substring(token.lastIndexOf("_")+1)
                    println "${runName} ${lane}"
                    return getSeqTrack(runName, lane)
                }
            }
        }
        return null
    }

    private SeqTrack parseTokensV2(List<String> tokens) {
        for(String token in tokens) {
            if (token.startsWith("ID:")) {
                int sepId = token.indexOf("_lane")
                if (sepId > -1) {
                    String runName = token.substring(3, sepId);
                    String lane = token.substring(sepId+5, sepId+6)
                    println "${runName} ${lane}"
                    return getSeqTrack(runName, lane)
                }
            }
        }
        return null
    }

    private SeqTrack parseTokensV3(List<String> tokens) {
        String runName
        String lane
        for(String token in tokens) {
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
        println "${runName} ${lane}"
        return getSeqTrack(runName, lane)
    }

    private SeqTrack parseTokensV4(List<String> tokens) {
        for(String token in tokens) {
            if (token.contains("DS:")) {
                token = token.substring(token.indexOf("DS:"))
            }
            if (token.startsWith("DS:")) {
                String runName = token.substring(3)
                int idx = runName.indexOf("_FC") + 3
                String lane = runName.substring(idx)
                println "${runName} ${lane}"
                return getSeqTrack(runName, lane)
            }
        }
        return null
    }

    private SeqTrack parseTokensV5(List<String> tokens) {
        println "parseTokensV5"
        for(String token in tokens) {
            if (token.startsWith("ID:")) {
                int sepId = token.indexOf("_L")
                if (sepId > -1) {
                    String runName = token.substring(3, sepId);
                    String lane = token.substring(sepId+2)
                    int laneNo = lane as int
                    println "${runName} ${laneNo}"
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
