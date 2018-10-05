package de.dkfz.tbi.otp.job.plan

class JobConnection {
    Long from
    Long to

    JobConnection(Long from, Long to) {
        this.from = from
        this.to = to
    }

}
