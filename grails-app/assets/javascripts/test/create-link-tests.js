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

/*global module, test, equal, $*/
module("create-links");
test("addComponent-empty", function () {
    "use strict";
    equal($.otp.addLinkComponent(), undefined);
    equal($.otp.addLinkComponent(""), "");
    equal($.otp.addLinkComponent("", undefined), "");
    equal($.otp.addLinkComponent("", null), "");
    equal($.otp.addLinkComponent("", ""), "");
});
test("addComponent-one-part", function () {
    "use strict";
    equal($.otp.addLinkComponent("test"), "test");
    equal($.otp.addLinkComponent("test", undefined), "test");
    equal($.otp.addLinkComponent("test", null), "test");
    equal($.otp.addLinkComponent("test", ""), "test");
    equal($.otp.addLinkComponent(1234, ""), "1234");
});
test("addComponent-no-slash", function () {
    "use strict";
    equal($.otp.addLinkComponent("test", "bar"), "test/bar");
    equal($.otp.addLinkComponent("/test", "bar"), "/test/bar");
    equal($.otp.addLinkComponent(1234, 5678), "1234/5678");
});
test("addComponent-one-slash", function () {
    "use strict";
    equal($.otp.addLinkComponent("test/", "bar"), "test/bar");
    equal($.otp.addLinkComponent("/test/", "bar/"), "/test/bar/");
    equal($.otp.addLinkComponent("test", "/bar"), "test/bar");
    equal($.otp.addLinkComponent("/test", "/bar/"), "/test/bar/");
    equal($.otp.addLinkComponent(1234, "/bar/"), "1234/bar/");
    equal($.otp.addLinkComponent("/test/", 5678), "/test/5678");
});
test("addComponent-two-slashes", function () {
    "use strict";
    equal($.otp.addLinkComponent("test/", "/bar"), "test/bar");
    equal($.otp.addLinkComponent("/test/", "/bar/"), "/test/bar/");
});
test("create-link-no-options", function () {
    "use strict";
    $.otp.contextPath = "/test/";
    equal($.otp.createLink(), "/test/");
    equal($.otp.createLink(null), "/test/");
    equal($.otp.createLink(undefined), "/test/");
    equal($.otp.createLink({}), "/test/");
    equal($.otp.createLink({test: 'incorrect'}), "/test/");
});
test("create-link-controller-action-id", function () {
    "use strict";
    $.otp.contextPath = "/test/";
    equal($.otp.createLink({
        controller: "foo"
    }), "/test/foo");
    equal($.otp.createLink({
        controller: "foo",
        action: "bar"
    }), "/test/foo/bar");
    equal($.otp.createLink({
        controller: "foo",
        action: "bar",
        id: "baz"
    }), "/test/foo/bar/baz");
    equal($.otp.createLink({
        controller: "foo",
        action: "bar",
        id: "baz",
        useless: "something"
    }), "/test/foo/bar/baz");
});
test("create-link-parameters", function () {
    "use strict";
    $.otp.contextPath = "/test/";
    equal($.otp.createLink({
        parameters: undefined
    }), "/test/");
    equal($.otp.createLink({
        parameters: null
    }), "/test/");
    equal($.otp.createLink({
        parameters: {}
    }), "/test/");
    equal($.otp.createLink({
        parameters: {
            foo: "bar"
        }
    }), "/test/?foo=bar");
    equal($.otp.createLink({
        controller: "testing",
        parameters: {
            foo: "bar",
            baz: "foobar"
        }
    }), "/test/testing?foo=bar&baz=foobar");
});
test("create-link-markup-empty", function () {
    "use strict";
    $.otp.contextPath = "/test/";
    equal($.otp.createLinkMarkup(), '<a href="/test"></a>');
    equal($.otp.createLinkMarkup(undefined), '<a href="/test"></a>');
    equal($.otp.createLinkMarkup(null), '<a href="/test"></a>');
    equal($.otp.createLinkMarkup({}), '<a href="/test"></a>');
    equal($.otp.createLinkMarkup({foo: 'bar'}), '<a href="/test"></a>');
    equal($.otp.createLinkMarkup({text: undefined}), '<a href="/test"></a>');
    equal($.otp.createLinkMarkup({text: null}), '<a href="/test"></a>');
    equal($.otp.createLinkMarkup({title: undefined}), '<a href="/test"></a>');
    equal($.otp.createLinkMarkup({title: null}), '<a href="/test"></a>');
});
test("create-link-markup", function () {
    "use strict";
    $.otp.contextPath = "/test/";
    equal($.otp.createLinkMarkup({
        controller: 'foo',
        text: "bar"
    }), '<a href="/test/foo">bar</a>');
    equal($.otp.createLinkMarkup({
        controller: 'foo',
        title: "foobar"
    }), '<a href="/test/foo" title="foobar"></a>');
    equal($.otp.createLinkMarkup({
        controller: 'foo',
        text: "bar",
        title: "foobar"
    }), '<a href="/test/foo" title="foobar">bar</a>');
    equal($.otp.createLinkMarkup({
        controller: 'foo',
        text: "bar",
        target: "_blank"
    }), '<a href="/test/foo" target="_blank">bar</a>');
    equal($.otp.createLinkMarkup({
        controller: 'foo',
        title: "foobar",
        target: "_blank"
    }), '<a href="/test/foo" title="foobar" target="_blank"></a>');
    equal($.otp.createLinkMarkup({
        controller: 'foo',
        text: "bar",
        title: "foobar",
        target: "_blank"
    }), '<a href="/test/foo" title="foobar" target="_blank">bar</a>');
});
