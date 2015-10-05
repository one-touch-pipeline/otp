
package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PostFilter;

class SeqCenterService {

    /**
     *
     * @return List of all available SeqCenters
     */
    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'read')")
    public List<SeqCenter> allSeqCenters() {
        return SeqCenter.list(sort: "name", order: "asc")
    }
}
