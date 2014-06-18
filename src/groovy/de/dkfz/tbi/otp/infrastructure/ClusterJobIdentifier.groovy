package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.Realm

public interface ClusterJobIdentifier {

    public Realm getRealm()

    public String getClusterJobId()
}
