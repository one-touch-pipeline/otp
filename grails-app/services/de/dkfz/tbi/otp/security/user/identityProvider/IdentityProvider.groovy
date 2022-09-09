/*
 * Copyright 2011-2022 The OTP authors
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

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.util.ldap.UserAccountControl

interface IdentityProvider {

    IdpUserDetails getIdpUserDetailsByUsername(String username)

    /**
     * Get a list of IdpUserDetails for every otp user who is given.
     *
     * @param otpUsers, for those the idp details are wanted
     * @return List of IdpUserDetails for the given users
     */
    List<IdpUserDetails> getIdpUserDetailsByUserList(List<User> otpUsers)

    /**
     * Find users by username or mail or real name. Only a few start letters are required.
     *
     * @param searchString  (contains username or mail or real name)
     * @return list of matching IdpUserDetails
     */
    List<IdpUserDetails> getListOfIdpUserDetailsBySearchString(String searchString)

    String getDistinguishedNameOfGroupByGroupName(String groupName)

    List<String> getGroupMembersByDistinguishedName(String distinguishedName)

    List<String> getGroupsOfUser(User user)

    boolean exists(User user)

    Map<String, String> getAllUserAttributes(User user)

    boolean isUserDeactivated(User user)

    boolean isUserInIdpAndActivated(User user)

    Integer getUserAccountControlOfUser(User user)

    Map<UserAccountControl, Boolean> getAllUserAccountControlFlagsOfUser(User user)
}
