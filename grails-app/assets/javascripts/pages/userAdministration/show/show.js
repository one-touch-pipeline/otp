/*
 * Copyright 2011-2019 The OTP authors
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

$.otp.userAdministration = {};
$.otp.userAdministration.editUser = {
  /*
     The Role Domain is split in "ROLE_*" and "GROUP_*" Authorities. These are
     kept in different places in the GUI, named after their respective authority.
     To select the right values the parameter "type" is used.
      */
  addOrRemoveUserRole(origin, userId, roleId, type) {
    'use strict';

    if (!(['Group', 'Role'].includes(type))) { return; }
    const action = $(origin).attr('class');
    if (!(['add', 'remove'].includes(action))) { return; }

    const labels = {
      add: { opposite: 'remove', appendToSection: 'user' },
      remove: { opposite: 'add', appendToSection: 'available' }
    };

    const messages = {
      'user.administration.role.addRole': 'Add Role to User',
      'user.administration.role.removeRole': 'Remove Role from User',
      'user.administration.role.addGroup': 'Add User to this Group',
      'user.administration.role.removeGroup': 'Remove User from this Group'
    };

    $.ajax({
      type: 'GET',
      url: $.otp.createLink({
        controller: 'userAdministration',
        action: action + 'Role',
        parameters: {
          user: userId,
          role: roleId
        }
      }),
      dataType: 'json',
      cache: 'false',
      success(data) {
        if (data.success) {
          const tableRow = $(origin).parents('tr');
          const { opposite } = labels[action];
          $('a', tableRow).text(messages['user.administration.role.' + opposite + '' + type]);
          $('a', tableRow).attr('class', opposite);
          tableRow.detach();
          tableRow.appendTo($('table tbody', $('#' + labels[action].appendToSection + '' + type)));
        } else {
          $.otp.warningMessage(data.error);
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        $.otp.warningMessage(textStatus + ' occurred while processing the data. Reason: ' + errorThrown);
      }
    });
  }
};
