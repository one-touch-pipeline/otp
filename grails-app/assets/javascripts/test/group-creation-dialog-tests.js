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

module("GroupDialog removeErrors");
test("clears error on parent", function () {
    $('<div id="create-group-dialog"><div class="error"><input type="text"/></div><div class="error"><input type="checkbox"/></div></div>').appendTo($("#qunit-fixture"));
    var object = $.otp.userAdministration.editUser.newGroupDialog;
    $.otp.userAdministration.editUser.newGroupDialog.dialog = $("#create-group-dialog");
    // verify that the divs have the class error
    ok(!$("#create-group-dialog").hasClass("error"));
    ok($("#create-group-dialog div").first().hasClass("error"));
    ok($("#create-group-dialog div").last().hasClass("error"));

    object.removeErrors.call(object);

    ok(!$("#create-group-dialog").hasClass("error"));
    ok(!$("#create-group-dialog div").first().hasClass("error"));
    ok(!$("#create-group-dialog div").last().hasClass("error"));
});

test("removes title from input fields", function () {
    $('<div id="create-group-dialog" title="Test-Outer"><div class="error" title="Test-Inner"><input type="text" title="Test-Text"/></div><div class="error"><input type="checkbox" title="Test-Checkbox"/></div></div>').appendTo($("#qunit-fixture"));
    var object = $.otp.userAdministration.editUser.newGroupDialog;
    $.otp.userAdministration.editUser.newGroupDialog.dialog = $("#create-group-dialog");
    // verify that the text and checkbox input have the title
    equal($("#create-group-dialog input[type=text]").attr("title"), "Test-Text");
    equal($("#create-group-dialog input[type=checkbox]").attr("title"), "Test-Checkbox");
    equal($("#create-group-dialog div").attr("title"), "Test-Inner");
    equal($("#create-group-dialog").attr("title"), "Test-Outer");

    object.removeErrors.call(object);

    equal($("#create-group-dialog input[type=text]").attr("title"), undefined);
    equal($("#create-group-dialog input[type=checkbox]").attr("title"), undefined);
    // other elements should be unaffected
    equal($("#create-group-dialog div").attr("title"), "Test-Inner");
    equal($("#create-group-dialog").attr("title"), "Test-Outer");
});

test("does not affect other input fields", function () {
    $('<div id="create-group-dialog"><div class="error"><input type="radio" title="test"/></div></div>').appendTo($("#qunit-fixture"));
    var object = $.otp.userAdministration.editUser.newGroupDialog;
    $.otp.userAdministration.editUser.newGroupDialog.dialog = $("#create-group-dialog");
    // verify that radio field has a title and that parent of it has class error
    ok($("#create-group-dialog div").hasClass("error"));
    equal($("#create-group-dialog input").attr("title"), "test");

    object.removeErrors.call(object);

    // should not affect
    ok($("#create-group-dialog div").hasClass("error"));
    equal($("#create-group-dialog input").attr("title"), "test");
});

module("GroupDialog resetFields");
test("input text and checkbox get reset", function () {
    $('<div id="create-group-dialog"><div class="error"><input type="text" value="Test"/></div><div class="error"><input type="checkbox" checked="checked"/></div></div>').appendTo($("#qunit-fixture"));
});
