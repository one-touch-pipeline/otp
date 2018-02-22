package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.ngsdata.*
import jdk.nashorn.internal.ir.annotations.*
import org.springframework.beans.factory.*
import org.springframework.ldap.core.*
import org.springframework.ldap.core.support.*
import org.springframework.ldap.query.*

import javax.naming.*
import javax.naming.directory.*
import java.util.regex.*

class LdapService implements InitializingBean {

    ConfigService configService
    private LdapTemplate ldapTemplate

    @Override
    void afterPropertiesSet() {
        LdapContextSource ldapContextSource = new LdapContextSource()
        ldapTemplate = new LdapTemplate(ldapContextSource)

        ldapContextSource.setUrl(configService.getLdapServer())
        ldapContextSource.setBase(configService.getLdapSearchBase())
        ldapContextSource.setUserDn(configService.getLdapManagerDistinguishedName())
        ldapContextSource.setPassword(configService.getLdapManagerPassword())
        ldapContextSource.afterPropertiesSet()

        ldapTemplate.setIgnorePartialResultException(true)
    }

    LdapUserDetails getUserMapByUsername(String username) {
        if (username == null) {
            return [:]
        }
        return ldapTemplate.search(
                LdapQueryBuilder.query().where("cn").is(username),
                new LdapuserDetailsAttributesMapper())[0]
    }

    String getDistinguishedNameOfGroupByGroupName(String groupName) {
        if (groupName == null) {
            return ""
        }
        return ldapTemplate.search(
                LdapQueryBuilder.query()
                        .where("objectCategory").is("group")
                        .and("cn").is(groupName),
                new DistinguishedNameAttributesMapper())[0]
    }

    List<String> getGroupMembersByDistinguishedName(String distinguishedName) {
        if (distinguishedName == null) {
            return []
        }
        // nested group memberships are not resolved
        return ldapTemplate.search(
                LdapQueryBuilder.query()
                        .where("objectCategory").is("user")
                        .and("memberOf").is(distinguishedName),
                new UsernameAttributesMapper())
    }
}

class LdapuserDetailsAttributesMapper implements AttributesMapper<LdapUserDetails> {
    @Override
    LdapUserDetails mapFromAttributes(Attributes a) throws NamingException {
        List<String> memberOfList = a.get("memberOf")?.getAll()?.collect {
            Matcher matcher = it =~ /CN=(?<cn>[^,]*),.*/
            if (matcher.matches() && matcher.group('cn')) return matcher.group('cn')
        }
        boolean deactivated = a.get("employeeType")?.get()?.toString() == "Ausgeschieden"
        return new LdapUserDetails([
                cn               : a.get("cn")?.get()?.toString(),
                givenName        : a.get("givenName")?.get()?.toString(),
                sn               : a.get("sn")?.get()?.toString(),
                mail             : a.get("mail")?.get()?.toString(),
                department       : a.get("department")?.get()?.toString(),
                thumbnailPhoto   : a.get("thumbnailPhoto")?.get() as byte[],
                deactivated      : deactivated,
                memberOfGroupList: memberOfList,
        ])
    }
}

class DistinguishedNameAttributesMapper implements AttributesMapper<String> {
    @Override
    String mapFromAttributes(Attributes a) throws NamingException {
        return a.get("distinguishedName")?.get()?.toString()
    }
}

class UsernameAttributesMapper implements AttributesMapper<String> {
    @Override
    String mapFromAttributes(Attributes a) throws NamingException {
        return a.get("cn")?.get()?.toString()
    }
}

@Immutable
class LdapUserDetails {
    String cn // commonName, holds the username in ldap
    String givenName
    String sn // surname
    String mail
    String department
    byte[] thumbnailPhoto
    boolean deactivated
    List<String> memberOfGroupList
}