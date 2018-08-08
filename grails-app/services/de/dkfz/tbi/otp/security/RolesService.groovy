package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.acl.*
import org.springframework.security.access.prepost.*
import org.springframework.security.acls.domain.*

class RolesService {

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<RolesWithUsersAndSeqCenters> getRolesAndUsers() {
        List<RolesWithUsersAndSeqCenters> roles = Role.list().collect {
            Role role -> new RolesWithUsersAndSeqCenters(role: role)
        }
        roles.each {
            it.users = UserRole.findAllByRole(it.role)*.user.flatten()
            it.users.sort { User user -> user.username }
        }
        roles.sort { it.role.authority }
        return roles
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void findSeqCenters(List<RolesWithUsersAndSeqCenters> roles) {
        List<SeqCenter> seqCenters = SeqCenter.list()

        AclEntry.createCriteria().list {
            eq("mask", BasePermission.READ.mask)
            aclObjectIdentity {
                'in'("objectId", seqCenters*.id)
                aclClass {
                    eq("className", SeqCenter.name)
                }
            }
            projections {
                sid {
                    property("sid")
                }
                aclObjectIdentity {
                    property("objectId")
                }
            }
        }.each { def result ->
            RolesWithUsersAndSeqCenters role = roles.find { it.role.authority == result[0] }
            role.seqCenters.add(SeqCenter.get(result[1]))
        }
        roles.each { it.seqCenters.sort { SeqCenter center -> center.name } }
    }
}

class RolesWithUsersAndSeqCenters {
    Role role
    List<User> users = []
    List<SeqCenter> seqCenters = []
}
