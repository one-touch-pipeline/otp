package de.dkfz.tbi.otp.sampleswap

import de.dkfz.tbi.otp.utils.Entity

class FileSystemChanges implements Entity {

    String command
    boolean executed = false

    static belongsTo = [swapInfo: SwapInfo]

    static mapping = {
        command type: "text"
    }
}
