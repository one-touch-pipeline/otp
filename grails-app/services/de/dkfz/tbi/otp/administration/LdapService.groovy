package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.ngsdata.*
import jdk.nashorn.internal.ir.annotations.*
import org.springframework.beans.factory.*
import org.springframework.ldap.core.*
import org.springframework.ldap.core.support.*
import org.springframework.ldap.query.ContainerCriteria

import javax.naming.*
import javax.naming.directory.*
import java.util.regex.*

import static org.springframework.ldap.query.LdapQueryBuilder.*

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

    LdapUserDetails getLdapUserDetailsByUsernameOrMailOrRealName(String searchString) {
        if (searchString == null) {
            return [:]
        }
        String sanitizedSearchString = searchString.trim().replaceAll(" +", " ")
        ContainerCriteria searchQuery = query().where("cn").is(sanitizedSearchString).or("mail").is(sanitizedSearchString)
        if (sanitizedSearchString.contains(" ")) {
            String[] splitSearch = sanitizedSearchString.split(" ", 2)
            searchQuery = query()
                    .where("givenName").like(splitSearch[0])
                    .and("sn").like(splitSearch[1])
                    .and("mail").isPresent()
        }
        return ldapTemplate.search(
                query().where("objectCategory").is("user")
                        .and(searchQuery),
                new LdapuserDetailsAttributesMapper())[0]
    }

    List<LdapUserDetails> getListOfLdapUserDetailsByUsernameOrMailOrRealName(String searchString, int countLimit = 0) {
        if (searchString == null) {
            return []
        }
        String sanitizedSearchString = searchString.trim().replaceAll(" +", " ")

        String wildcardedSearch = "*${sanitizedSearchString}*"
        ContainerCriteria dynamicQuery = query()
                .where("cn").like(wildcardedSearch)
                .or("mail").like(wildcardedSearch)
                .or("givenName").like(wildcardedSearch)
                .or("sn").like(wildcardedSearch)

        if (sanitizedSearchString.contains(" ")) {
            String[] splitSearch = sanitizedSearchString.split(" ", 2)
            dynamicQuery = query()
                    .where("givenName").like("*${splitSearch[0]}*")
                    .and("sn").like("*${splitSearch[1]}*")
        }

        return ldapTemplate.search(
                query().countLimit(countLimit)
                        .attributes("cn", "mail", "givenName", "sn", "department")
                        .where("objectCategory").is("person")
                        .and("mail").isPresent()
                        .and(dynamicQuery),
                new LdapuserDetailsAttributesMapper())
    }

    String getDistinguishedNameOfGroupByGroupName(String groupName) {
        if (groupName == null) {
            return ""
        }
        return ldapTemplate.search(
                query().where("objectCategory").is("group")
                       .and("cn").is(groupName),
                new DistinguishedNameAttributesMapper())[0]
    }

    List<String> getGroupMembersByDistinguishedName(String distinguishedName) {
        if (distinguishedName == null) {
            return []
        }
        // nested group memberships are not resolved
        return ldapTemplate.search(
                query().where("objectCategory").is("user")
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
        String givenName = a.get("givenName")?.get()
        String sn = a.get("sn")?.get()
        boolean realNameCreatable = givenName && sn
        return new LdapUserDetails([
                cn               : a.get("cn")?.get()?.toString(),
                realName         : realNameCreatable ? "${givenName} ${sn}" : null,
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
    String realName
    String mail
    String department
    byte[] thumbnailPhoto
    boolean deactivated
    List<String> memberOfGroupList
}