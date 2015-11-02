
package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize

class SeqCenterService {

    /**
     *
     * @return List of all available SeqCenters
     */
    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'read')")
    public List<SeqCenter> allSeqCenters() {
        return SeqCenter.list(sort: "name", order: "asc")
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public SeqCenter createSeqCenter(String name, String dirName){
        assert name : "the input name '${name}' must not be null"
        assert dirName : "the input dirname '${dirName}' must not be null"
        assert !SeqCenter.findByName(name) : "The SeqCenter '${name}' exists already"
        assert !SeqCenter.findByDirName(dirName) : "The SeqCenter dirname '${dirName}' exists already"
        SeqCenter seqCenter = new SeqCenter(
            name: name,
            dirName: dirName
        )
        assert seqCenter.save(flush: true, failOnError: true)
        return seqCenter
    }
}