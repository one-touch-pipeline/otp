/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.security.user.identityProvider

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ldap.NameNotFoundException
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.ldap.filter.*
import org.springframework.ldap.query.ContainerCriteria
import org.springframework.ldap.support.LdapUtils

import de.dkfz.tbi.otp.administration.LdapKey
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.utils.ldap.UserAccountControl

import javax.naming.NamingException
import javax.naming.directory.Attributes
import java.util.regex.Matcher

import static org.springframework.ldap.query.LdapQueryBuilder.query

@CompileDynamic
@SuppressWarnings(["ExplicitCallToAndMethod", "ExplicitCallToOrMethod"])
@Transactional
class LdapService implements InitializingBean, IdentityProvider {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ConfigService configService

    @Autowired
    ProcessingOptionService processingOptionService

    private LdapTemplate ldapTemplate

    // time between 1601.1.1 and 1970.1.1 12 am UTC
    static long gapEpochLdapTime = 11644473600000

    @Override
    void afterPropertiesSet() {
        LdapContextSource ldapContextSource = new LdapContextSource()
        ldapContextSource.url = configService.ldapServer
        ldapContextSource.base = configService.ldapSearchBase
        ldapContextSource.userDn = configService.ldapManagerDistinguishedName
        ldapContextSource.password = configService.ldapManagerPassword
        ldapContextSource.afterPropertiesSet()

        ldapTemplate = new LdapTemplate(ldapContextSource)
        ldapTemplate.ignorePartialResultException = true
    }

    @Override
    IdpUserDetails getIdpUserDetailsByUsername(String username) {
        if (username == null) {
            return null
        }
        return ldapTemplate.search(
                query().where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                        .and(configService.ldapSearchAttribute).is(username),
                new LdapUserDetailsAttributesMapper(ldapService: this))[0]
    }

    @Override
    List<IdpUserDetails> getIdpUserDetailsByUserList(List<User> otpUsers) {
        OrFilter innerFilter = new OrFilter()

        otpUsers.each { User user ->
            innerFilter.or(new EqualsFilter(configService.ldapSearchAttribute, user?.username))
        }

        AndFilter outerFilter = new AndFilter()
        outerFilter.and(new EqualsFilter(LdapKey.OBJECT_CATEGORY, LdapKey.USER)).and(innerFilter)

        return ldapTemplate.search(
                "",
                outerFilter.encode(),
                new LdapUserDetailsAttributesMapper(ldapService: this))
    }

    @Override
    List<IdpUserDetails> getListOfIdpUserDetailsBySearchString(String searchString) {
        int countLimit = 0

        if (searchString == null) {
            return []
        }
        String sanitizedSearchString = StringUtils.trimAndShortenWhitespace(searchString)

        String wildcardedSearch = "*${sanitizedSearchString}*"
        ContainerCriteria dynamicQuery = query()
                .where(configService.ldapSearchAttribute).like(wildcardedSearch)
                .or(LdapKey.MAIL).like(wildcardedSearch)
                .or(LdapKey.GIVEN_NAME).like(wildcardedSearch)
                .or(LdapKey.SURNAME).like(wildcardedSearch)

        if (sanitizedSearchString.contains(" ")) {
            String[] splitSearch = sanitizedSearchString.split(" ", 2)
            dynamicQuery = query()
                    .where(LdapKey.GIVEN_NAME).like("*${splitSearch[0]}*")
                    .and(LdapKey.SURNAME).like("*${splitSearch[1]}*")
        }

        return ldapTemplate.search(
                query().countLimit(countLimit)
                        .attributes(configService.ldapSearchAttribute, LdapKey.MAIL, LdapKey.GIVEN_NAME,
                                LdapKey.SURNAME, LdapKey.DEPARTMENT, LdapKey.USER_ACCOUNT_CONTROL)
                        .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.PERSON)
                        .and(LdapKey.MAIL).isPresent()
                        .and(dynamicQuery as ContainerCriteria),
                new LdapUserDetailsAttributesMapper(ldapService: this))
    }

    private String getDistinguishedNameByGroupName(String groupName) {
        if (groupName == null) {
            return ""
        }
        return ldapTemplate.search(
                query().where(LdapKey.OBJECT_CATEGORY).is(LdapKey.GROUP)
                        .and(configService.ldapSearchAttribute).is(groupName),
                new DistinguishedNameAttributesMapper())[0]
    }

    private List<String> getGroupMembersByDistinguishedName(String distinguishedName) {
        if (distinguishedName == null) {
            return []
        }
        // nested group memberships are not resolved
        return ldapTemplate.search(
                query().where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                        .and(LdapKey.MEMBER_OF).is(distinguishedName),
                new UsernameAttributesMapper(ldapService: this))
    }

    @Override
    List<String> getGroupMembersByGroupName(String groupName) {
        String distinguishedName = getDistinguishedNameByGroupName(groupName)
        return getGroupMembersByDistinguishedName(distinguishedName)
    }

    @Override
    List<String> getGroupsOfUser(User user) {
        if (!user.username) {
            return []
        }
        ContainerCriteria query = query()
                .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                .and(configService.ldapSearchAttribute).is(user.username)
        return ldapTemplate.search(query, new MemberOfAttributesMapper())[0]
    }

    @Override
    boolean exists(User user) {
        if (!user.username) {
            return false
        }
        ContainerCriteria query = query()
                .attributes(configService.ldapSearchAttribute)
                .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                .and(configService.ldapSearchAttribute).is(user.username)
        return ldapTemplate.search(query, new DistinguishedNameAttributesMapper()).size() >= 1
    }

    @Override
    Map<String, String> getAllUserAttributes(User user) {
        if (!user.username) {
            return [:]
        }
        ContainerCriteria query = query()
                .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                .and(configService.ldapSearchAttribute).is(user.username)
        return CollectionUtils.exactlyOneElement(ldapTemplate.search(query, new AllAttributesMapper()))
    }

    @Override
    boolean isUserDeactivated(User user) {
        if (!user.username) {
            return true
        }

        return isUserAccountDisabled(user) || isUserInDeletedUsersOu(user) || isUserAccountExpired(user)
    }

    @Override
    boolean isUserInIdpAndActivated(User user) {
        return (exists(user) && !isUserDeactivated(user))
    }

    @Override
    Integer getUserAccountControlOfUser(User user) {
        if (!user.username) {
            return null
        }
        ContainerCriteria query = query()
                .attributes(configService.ldapSearchAttribute, LdapKey.USER_ACCOUNT_CONTROL)
                .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                .and(configService.ldapSearchAttribute).is(user.username)
        return ldapTemplate.search(query, new UserAccountControlMapper())[0]
    }

    @Override
    Map<UserAccountControl, Boolean> getAllUserAccountControlFlagsOfUser(User user) {
        Integer value = getUserAccountControlOfUser(user)
        if (value == null) {
            return [:]
        }
        return UserAccountControl.values().collectEntries { UserAccountControl field ->
            [(field): UserAccountControl.isSet(field, value)]
        }
    }

    @Override
    String getLogoutUri() {
        return "${grailsApplication.config.getProperty("grails.serverURL")}/logout"
    }

    boolean getIsDeactivatedFromAttributes(Attributes attributes) {
        if (processingOptionService.findOptionAsBoolean(ProcessingOption.OptionName.LDAP_RESPECT_DEACTIVATED_USER)) {
            int uacValue = attributes.get(LdapKey.USER_ACCOUNT_CONTROL)?.get()?.toString()?.toInteger() ?: 0
            return UserAccountControl.isSet(UserAccountControl.ACCOUNTDISABLE, uacValue)
        }
        return false
    }

    boolean authenticateUser(String name, String credentials) {
        Filter filter = new EqualsFilter(configService.ldapSearchAttribute, name)
        return ldapTemplate.authenticate(LdapUtils.emptyLdapName(), filter.encode(), credentials)
    }

    /**
     * Check if the user is in the organizational unit DeletedUsers
     * E.g. an entry in ldap:
     * distinguishedName: cn=<username>,ou=DeletedUsers,dc=otpldap,dc=dev
     *
     * @param user, an OTP user
     * @return if user is in the ou=DeletedUsers
     */
    private Boolean isUserInDeletedUsersOu(User user) {
        try {
            ContainerCriteria query = query().base("${LdapKey.ORGANIZATIONAL_UNIT}=${LdapKey.DELETED_USERS}").countLimit(1)
                    .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                    .and(configService.ldapSearchAttribute).is(user.username)
            return !ldapTemplate.search(query, new IsUserDeactivatedMapper(ldapService: this)).empty
        } catch (NameNotFoundException ignored) {
            return false
        }
    }

    /**
     * Check if the user's account has expired by comparing the current timestamp with the accountExpires field
     * assuming all in UTC time zone (timezone are not critical in this case)
     * E.g. an entry in ldap:
     * accountExpires: 122756184000000000
     *
     * @param user, an OTP user
     * @return if user's accountExpires attribute is set earlier than now
     */
    private Boolean isUserAccountExpired(User user) {
        long ldapTimestamp = toLdapTimestamp100Nanos(System.currentTimeMillis())
        ContainerCriteria query = query().countLimit(1)
                .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                .and(configService.ldapSearchAttribute).is(user.username)
                .and(LdapKey.ACCOUNT_EXPIRES).gte('1')
                .and(LdapKey.ACCOUNT_EXPIRES).lte(ldapTimestamp.toString())
        return !ldapTemplate.search(query, new UsernameAttributesMapper(ldapService: this)).empty
    }

    /**
     * Check the userAccountControl to see if ACCOUNTDISABLE bit is set (2 in integer)
     *
     * @param user, an OTP user
     * @return true if user's ACCOUNTDISABLE bit is set
     */
    private Boolean isUserAccountDisabled(User user) {
        ContainerCriteria query = query()
                .attributes(configService.ldapSearchAttribute, LdapKey.USER_ACCOUNT_CONTROL)
                .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                .and(configService.ldapSearchAttribute).is(user.username)
        return ldapTemplate.search(query, new IsUserDeactivatedMapper(ldapService: this))[0]
    }

    /*
     * Convert Unix timestamp in (milli) (UTC) to Ldap timestamp (100nano)
     * Note: Ldap timestamp is an Integer8, a 64-bit number representing the
     * number of 100-nanosecond intervals since 12:00 am 1/1/1601
     * It is used e.g. in accountExpires attribute
     */
    long toLdapTimestamp100Nanos(long unixTimestampMillis) {
        return (unixTimestampMillis + gapEpochLdapTime) * 10000
    }
}

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AbstractLdapServiceAwareAttributesMapper<T> implements AttributesMapper<T> {
    LdapService ldapService
}

class LdapUserDetailsAttributesMapper extends AbstractLdapServiceAwareAttributesMapper<IdpUserDetails> {
    @Override
    IdpUserDetails mapFromAttributes(Attributes a) throws NamingException {
        List<String> memberOfList = a.get(LdapKey.MEMBER_OF)?.all?.collect {
            Matcher matcher = it =~ /CN=(?<group>[^,]*),.*/
            if (matcher.matches() && matcher.group("group")) {
                return matcher.group("group")
            }
        }
        boolean deactivated = ldapService.getIsDeactivatedFromAttributes(a)
        String givenName = a.get(LdapKey.GIVEN_NAME)?.get()
        String sn = a.get(LdapKey.SURNAME)?.get()
        boolean realNameCreatable = givenName && sn

        return new IdpUserDetails([
                username         : a.get(ldapService.configService.ldapSearchAttribute)?.get()?.toString(),
                realName         : realNameCreatable ? "${givenName} ${sn}" : null,
                mail             : a.get(LdapKey.MAIL)?.get()?.toString(),
                department       : a.get(LdapKey.DEPARTMENT) ? a.get(LdapKey.DEPARTMENT)?.get()?.toString() : "",
                thumbnailPhoto   : a.get(LdapKey.THUMBNAIL_PHOTO) ? a.get(LdapKey.THUMBNAIL_PHOTO)?.get()?.toString() : "",
                deactivated      : deactivated,
                memberOfGroupList: memberOfList,
        ])
    }
}

class IsUserDeactivatedMapper extends AbstractLdapServiceAwareAttributesMapper<Boolean> {
    @Override
    Boolean mapFromAttributes(Attributes a) throws NamingException {
        return ldapService.getIsDeactivatedFromAttributes(a)
    }
}

@CompileDynamic
class AllAttributesMapper implements AttributesMapper<Map<String, String>> {
    @Override
    Map<String, String> mapFromAttributes(Attributes a) throws NamingException {
        Map<String, String> map = [:]
        a.all.each {
            map[it.ID] = it.get().toString()
        }
        return map
    }
}

class DistinguishedNameAttributesMapper implements AttributesMapper<String> {
    @Override
    String mapFromAttributes(Attributes a) throws NamingException {
        return a.get(LdapKey.DISTINGUISHED_NAME)?.get()?.toString()
    }
}

class UsernameAttributesMapper extends AbstractLdapServiceAwareAttributesMapper<String> {
    @Override
    String mapFromAttributes(Attributes a) throws NamingException {
        return a.get(ldapService.configService.ldapSearchAttribute)?.get()?.toString()
    }
}

class UserAccountControlMapper implements AttributesMapper<Integer> {
    @Override
    Integer mapFromAttributes(Attributes a) throws NamingException {
        return a.get(LdapKey.USER_ACCOUNT_CONTROL)?.get()?.toString()?.toInteger()
    }
}

class MemberOfAttributesMapper implements AttributesMapper<List<String>> {
    @Override
    List<String> mapFromAttributes(Attributes a) throws NamingException {
        return a.get(LdapKey.MEMBER_OF)?.all?.collect {
            Matcher matcher = it =~ /CN=(?<group>[^,]*),.*/
            if (matcher.matches() && matcher.group("group")) {
                return matcher.group("group")
            }
        }
    }
}
