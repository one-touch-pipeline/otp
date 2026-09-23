/*
 * Copyright 2011-2026 The OTP authors
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

import { describe, it } from 'node:test';
import { RuleTester } from 'eslint';

import rule from './require-code-in-comments.mjs';

RuleTester.describe = describe;
RuleTester.it = it;

const ruleTester = new RuleTester();

/** A single report, the reference it names is the span the fix wraps. */
function error(reference) {
  return { messageId: 'requireBackticks', data: { reference } };
}

ruleTester.run('require-code-in-comments: calls', rule, {
  valid: [
    '// `should()` has to be wrapped',
    '// the word login is prose, so it stays unreported',
    // without its arguments there is no span to wrap
    "// a call left open such as foo('a' is not reported",
    // the plural a word is given in prose is no call, however much it looks like one
    '// the file(s) and sample(s) to import',
    // a sample over several lines is left to its author, who fences it
    "/*\n * ```js\n * cy.get('#id')\n *   .should('be.visible')\n * ```\n */",
    // a reference ends with its line: the call below is closed on the next one, so it is none
    '/*\n * foo(\n * ) x\n */'
  ],
  invalid: [
    {
      code: '// should() has to be wrapped',
      output: '// `should()` has to be wrapped',
      errors: [error('should()')]
    },
    {
      code: '// a.b.c.d() has to be wrapped',
      output: '// `a.b.c.d()` has to be wrapped',
      errors: [error('a.b.c.d()')]
    },
    {
      // a parenthesis inside a selector does not end the call early
      code: "// cy.get('#id(x)') has to be wrapped",
      output: "// `cy.get('#id(x)')` has to be wrapped",
      errors: [error("cy.get('#id(x)')")]
    },
    {
      // the nested call is already inside the span around the outer one
      code: '// expect(getName(user)) has to be wrapped',
      output: '// `expect(getName(user))` has to be wrapped',
      errors: [error('expect(getName(user))')]
    },
    {
      // a reference passed as an argument is covered by the span as well
      code: '// expect(cy.state) has to be wrapped',
      output: '// `expect(cy.state)` has to be wrapped',
      errors: [error('expect(cy.state)')]
    },
    {
      // a cascade is one reference, the dots between its links stay inside the span
      code: "// cy.get('#a').find('li').first() has to be wrapped",
      output: "// `cy.get('#a').find('li').first()` has to be wrapped",
      errors: [error("cy.get('#a').find('li').first()")]
    },
    {
      // a name without arguments carries the cascade on to the next call
      code: "// cy.get('a').first.click() has to be wrapped",
      output: "// `cy.get('a').first.click()` has to be wrapped",
      errors: [error("cy.get('a').first.click()")]
    },
    {
      // on its own such a name ends the cascade: a full stop in prose looks exactly like it
      code: "// cy.get('a').should has to be wrapped",
      output: "// `cy.get('a')`.should has to be wrapped",
      errors: [error("cy.get('a')")]
    },
    {
      code: '// the call is validate().Then the page reloads',
      output: '// the call is `validate()`.Then the page reloads',
      errors: [error('validate()')]
    },
    {
      // the same call twice is reported per occurrence, not per name
      code: '// foo() and foo() have to be wrapped',
      output: '// `foo()` and `foo()` have to be wrapped',
      errors: [error('foo()'), error('foo()')]
    },
    {
      code: '/* first line\n * bar() in the second\n */',
      output: '/* first line\n * `bar()` in the second\n */',
      errors: [error('bar()')]
    },
    {
      // a link left open ends the cascade, the links up to it are still wrapped
      code: "// cy.get('a').should('exist breaks off",
      output: "// `cy.get('a')`.should('exist breaks off",
      errors: [error("cy.get('a')")]
    },
    {
      // an escaped quote does not end the argument it stands in
      code: "// foo('a\\'b') has to be wrapped",
      output: "// `foo('a\\'b')` has to be wrapped",
      errors: [error("foo('a\\'b')")]
    },
    {
      // every line of a sample left unfenced is read on its own, the cascade is not followed down
      code: "/*\n * cy.get('#id')\n *   .should('be.visible')\n */",
      output: "/*\n * `cy.get('#id')`\n *   `.should('be.visible')`\n */",
      errors: [error("cy.get('#id')"), error(".should('be.visible')")]
    },
    {
      // a name may open with a '$', which no word boundary holds in front of
      code: "// use $('#id').not('.x') to select",
      output: "// use `$('#id').not('.x')` to select",
      errors: [error("$('#id').not('.x')")]
    },
    {
      code: '// call $scope.apply() here',
      output: '// call `$scope.apply()` here',
      errors: [error('$scope.apply()')]
    },
    {
      // an argument of one or two letters is an argument, only '(s)' is the plural of a word
      code: '// call init(db) to start',
      output: '// call `init(db)` to start',
      errors: [error('init(db)')]
    },
    {
      // a literal is read where one can stand, inside an argument list; a '/' outside of one
      // divides or is part of a path, so what looks like a pattern in prose is not read as one
      code: '// matches /get(All)?Users/ in the router',
      output: '// matches /`get(All)`?Users/ in the router',
      errors: [error('get(All)')]
    },
    {
      // two slashes around a call make no regular expression of it
      code: "// cy.visit('/a') and cy.intercept('GET', '/b')",
      output: "// `cy.visit('/a')` and `cy.intercept('GET', '/b')`",
      errors: [error("cy.visit('/a')"), error("cy.intercept('GET', '/b')")]
    },
    {
      code: '// under /grails/app the init() runs in /src/main',
      output: '// under /grails/app the `init()` runs in /src/main',
      errors: [error('init()')]
    },
    {
      code: '// checks read/write and foo(x) then in/out',
      output: '// checks read/write and `foo(x)` then in/out',
      errors: [error('foo(x)')]
    },
    {
      // an '@example' in a line comment opens no block, nothing renders it as code
      code: '// @example cy.get("a") should be wrapped',
      output: '// @example `cy.get("a")` should be wrapped',
      errors: [error('cy.get("a")')]
    }
  ]
});

ruleTester.run('require-code-in-comments: regular expressions', rule, {
  valid: [
    '// `cy.contains(/[)]/)` has to be wrapped',
    '/* a ratio of a / b is prose, not a call */',
    '// a path such as /grails/app is prose, not a call'
  ],
  invalid: [
    {
      // a parenthesis inside a regular expression must not end the call early
      code: '// cy.contains(/[)]/) has to be wrapped',
      output: '// `cy.contains(/[)]/)` has to be wrapped',
      errors: [error('cy.contains(/[)]/)')]
    },
    {
      // an escaped delimiter or slash inside the expression does not close it either
      code: '// cy.get(/a\\/b(c)/i) has to be wrapped',
      output: '// `cy.get(/a\\/b(c)/i)` has to be wrapped',
      errors: [error('cy.get(/a\\/b(c)/i)')]
    },
    {
      // a division inside the arguments stays part of the call
      code: '// round(width / 2) has to be wrapped',
      output: '// `round(width / 2)` has to be wrapped',
      errors: [error('round(width / 2)')]
    },
    {
      code: "// cy.get(/[)]/).should('exist') has to be wrapped",
      output: "// `cy.get(/[)]/).should('exist')` has to be wrapped",
      errors: [error("cy.get(/[)]/).should('exist')")]
    }
  ]
});

ruleTester.run('require-code-in-comments: cypress references', rule, {
  valid: [
    '// `cy.intercept` has to be wrapped',
    '// cy and Cypress on their own stay unreported',
    // a reference whose call is written with a space is left to the call pattern, which wants none
    '// cy.intercept () is not a bare reference',
    // a name of three segments is held together, no prefix of it is reported on its own
    "// Cypress.Commands.add ('login') registers it",
    // a name opening with a '$' is one name, no word boundary stands inside it
    '// use $cy.state internally',
    '// $Cypress.env is set',
    // and a dot, a hyphen or a slash in front of it says the name goes on to the left
    '// window.Cypress.env is set',
    '// this.cy.get here',
    '// see cypress/e2e/login.cy.js for details',
    '// non-cy.get alternatives'
  ],
  invalid: [
    {
      // a bare reference is reported although it has no parentheses
      code: '// cy.intercept has to be wrapped',
      output: '// `cy.intercept` has to be wrapped',
      errors: [error('cy.intercept')]
    },
    {
      code: '// Cypress.Commands.add has to be wrapped',
      output: '// `Cypress.Commands.add` has to be wrapped',
      errors: [error('Cypress.Commands.add')]
    },
    {
      // a name may end on a '$' as well, where no word boundary stands either
      code: '// cy.state$ emits the value',
      output: '// `cy.state$` emits the value',
      errors: [error('cy.state$')]
    },
    {
      // the call pattern takes the whole path of names, so a call keeps what stands to its left
      code: "// window.Cypress.env('x') is set",
      output: "// `window.Cypress.env('x')` is set",
      errors: [error("window.Cypress.env('x')")]
    },
    {
      // the call behind a name of three segments is one too, the name is not cut short
      code: "// Cypress.Commands.add('login') registers it",
      output: "// `Cypress.Commands.add('login')` registers it",
      errors: [error("Cypress.Commands.add('login')")]
    },
    {
      // a called reference is reported once, as the call it is
      code: '// cy.intercept() has to be wrapped',
      output: '// `cy.intercept()` has to be wrapped',
      errors: [error('cy.intercept()')]
    },
    {
      code: "// cy.intercept('GET', '/api').as('req') has to be wrapped",
      output: "// `cy.intercept('GET', '/api').as('req')` has to be wrapped",
      errors: [error("cy.intercept('GET', '/api').as('req')")]
    }
  ]
});

ruleTester.run('require-code-in-comments: what the rule leaves alone', rule, {
  valid: [
    // a fenced block spans the lines up to its closing fence
    '/* example:\n * ```js\n * cy.visit(\'/\')\n * ```\n */',
    // an unclosed marker marks the rest of its line, which keeps the fix idempotent
    '// a stray ` and should() behind it',
    "// `cy.get('a').should('exist')` is already wrapped",
    // a reference running into markup that is already there is left to its author
    '// const a = `${q}${list.join(`x`)}${q}`;',
    // one starting where a span ends would set its backticks against those of the span
    "// see `x`cy.get('a') here",
    // a reference reaching into an address is left alone as well
    "// see https://docs.cypress.io/api/get#cy.get('sel') for details",
    // comments the tooling reads itself
    '// istanbul ignore next -- getName(user) stays unchecked',
    '/* global cy, Cypress */',
    '/* exported setUp */',
    // what an '@example' introduces is a sample the tooling renders as code itself
    "/**\n * @example\n * cy.get('a').click()\n */",
    // a decorator inside the sample is indented past the tag, so it ends nothing
    '/**\n * @example\n * class A {\n *   @Input() name;\n * }\n * cy.get("b").click()\n */',
    '/**\n * @example\n * class A {\n *   @observable value = 1;\n * }\n * cy.get("b").click()\n */',
    // whatever the comment indents by, the sample runs to the next tag standing no deeper
    "/**\n *  @example\n *  cy.get('a')\n *  cy.get('b')\n */",
    // the plural of a word ending on a consonant
    '// the propert(ies) of the entit(ies)',
    "/**\n * @example cy.get('a').click()\n */",
    '/**\n * @example { not a type\n * cy.get("a").click()\n * }\n */',
    // a type is read by the tooling, a pair of backticks would break it for both
    '/** @param {function(string)} cb */',
    '/** @param {{a: {b: function(x)}}} opts */',
    '/**\n * @param {{\n *   cb: function(x)\n * }} opts\n */',
    '/** @implements {Foo(1)} x */',
    '/** {@link cy.get()} */',
    '/** @see {@link cy.get()} */',
    // a marker names a person, not a function
    '// TODO(sbo): fix this',
    '// todo(sbo) and FIXME(sbo) are markers as well',
    '// NOTE(sbo), XXX(sbo) and HACK(sbo) are markers as well',
    // an address carries its parentheses itself
    '// see https://host/path/foo(bar) for the details',
    '// see HTTPS://HOST/path/foo(1) for the details',
    // a reference carrying backticks is delimited by a longer run of them
    '// ``cy.wrap(`x`)`` is already wrapped'
  ],
  invalid: [
    {
      // a marker opening behind the reference does not cover it
      code: '// should() before a stray `',
      output: '// `should()` before a stray `',
      errors: [error('should()')]
    },
    {
      code: "// `cy.get('a')` and should() are mixed",
      output: "// `cy.get('a')` and `should()` are mixed",
      errors: [error('should()')]
    },
    {
      // a template literal in the arguments is delimited by a run of two backticks
      code: '// cy.contains(`${name}`) has to be wrapped',
      output: '// ``cy.contains(`${name}`)`` has to be wrapped',
      errors: [error('cy.contains(`${name}`)')]
    },
    {
      // a JSDoc tag types what stands in its braces, the rest of the line is prose
      code: '/** @param {string} name of cy.get(name) */',
      output: '/** @param {string} name of `cy.get(name)` */',
      errors: [error('cy.get(name)')]
    },
    {
      // a brace left open types nothing, so what follows it is read as usual
      code: '/** @param {string name of cy.get(name) */',
      output: '/** @param {string name of `cy.get(name)` */',
      errors: [error('cy.get(name)')]
    },
    {
      // a sample indented deeper still ends at its tag, opening and closing use the same rule
      code: "/**\n *  @example\n *  cy.get('a')\n *  @param {string} x see foo()\n */",
      output: "/**\n *  @example\n *  cy.get('a')\n *  @param {string} x see `foo()`\n */",
      errors: [error('foo()')]
    },
    {
      // the sample ends at the tag that follows it, what stands behind that is prose again
      code: "/**\n * @example\n * cy.get('a').click()\n *\n * @param {string} x see foo()\n */",
      output: "/**\n * @example\n * cy.get('a').click()\n *\n * @param {string} x see `foo()`\n */",
      errors: [error('foo()')]
    },
    {
      // and it begins at the tag, not before it
      code: "/**\n * calls foo() first\n * @example\n * cy.get('a')\n */",
      output: "/**\n * calls `foo()` first\n * @example\n * cy.get('a')\n */",
      errors: [error('foo()')]
    },
    {
      // an '@example' named in a sentence describes the tag, it opens no sample
      code: '// see @example below and foo() here',
      output: '// see @example below and `foo()` here',
      errors: [error('foo()')]
    },
    {
      // a type left open ends at the next tag, it does not reach across it
      code: '/**\n * @param {Foo bar()\n * @returns x} y cy.get("a")\n */',
      output: '/**\n * @param {Foo `bar()`\n * @returns x} y `cy.get("a")`\n */',
      errors: [error('bar()'), error('cy.get("a")')]
    },
    {
      // an address the reference holds whole does not stop it from being wrapped
      code: "// cy.visit('https://localhost/x');",
      output: "// `cy.visit('https://localhost/x')`;",
      errors: [error("cy.visit('https://localhost/x')")]
    },
    {
      // as the first word of a line comment 'global' and 'exported' open a sentence, not a directive
      code: '// global state is read by getName(x) here',
      output: '// global state is read by `getName(x)` here',
      errors: [error('getName(x)')]
    },
    {
      code: '// exported so the test can call foo()',
      output: '// exported so the test can call `foo()`',
      errors: [error('foo()')]
    },
    {
      // an '@' inside a word opens no type, the brace behind it holds prose
      code: '// mail sbo@dkfz {about foo() below} first',
      output: '// mail sbo@dkfz {about `foo()` below} first',
      errors: [error('foo()')]
    }
  ]
});
