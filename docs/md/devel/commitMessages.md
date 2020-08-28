<!--
  ~ Copyright 2011-2019 The OTP authors
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy
  ~ of this software and associated documentation files (the "Software"), to deal
  ~ in the Software without restriction, including without limitation the rights
  ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  ~ copies of the Software, and to permit persons to whom the Software is
  ~ furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all
  ~ copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  ~ SOFTWARE.
  -->

[Back to index](index.md)

Commit Messages
===============

The commit messages are used for creating the changelog and to prepare sprint reviews.

To allow this automatically, the format of the commit header is fixed.
Since sometimes a commit should not be shown, therefor two structures exists:
- used commits: 'category keyword: otp reference: text'.
- ignored commits: 'ignore keyword: text'.

In both cases the parts are separated via colon and space.


Category
--------

A category is used for grouping commits in the changelog.

The following categories are available (with the given keywords case insensitive)

| Category  Header| Category Keywords | Description |
|:----------------|:------------------|:------------|
| New Features, Changes and Improvements | change, chg, add, new | New or improved features |
| Bugfixes | bug, bugfix, fix | Used for all types of bugfixes |
| Refactor | refactor | Code refactoring |
| Codenarc | codenarc | Fixes of codenarc |
| Scripts | scripts | New or changed scripts |
| Minor | minor | Very small changes |


Ignore keywords
---------------

The keywords to ignore a commit are:
- WIP
- ignore
- review

They are case insensitive and may be prefixed with '!'.


OTP Reference
-------------

A reference to an OTP issue in [Youtrack](https://one-touch-pipeline.myjetbrains.com)

It has the format: "otp-\d+"

Please note that '***otp***' have to be lowercase.


Text
----

A descriptive text what was done. It is used for the generated changelog.
