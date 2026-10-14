<!--
  ~ Copyright 2011-2026 The OTP authors
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

# Require a response status assertion for Cypress network waits

`local/require-cypress-response-status` reports network waits whose inline chain
does not assert an expected HTTP response status. It runs on Cypress E2E and
support files through `npm run lint`.

Accepted examples:

```javascript
cy.wait('@data').its('response.statusCode').should('eq', 200);
cy.wait('@save').then((request) => {
  expect(request.response.statusCode).to.eq(302);
});
cy.wait(['@first', '@second']).each((request) => {
  expect(request.response.statusCode).to.eq(200);
});
```

The rule recognizes status comparisons in inline `then`, `should`, `and`, `each`
and `spread` callbacks, direct status assertions, property assertions and
destructured response parameters. Callback assertions must reference the matching
parameter binding. An assertion on a nested request or an unrelated response does
not satisfy the outer wait. Alias arrays need checks for every response.

Use the expected status for the endpoint. Redirects and tests of rejected requests
can legitimately expect statuses other than 200. No automatic fix is provided.

When a status assertion adds nothing, disable the rule for that line and give the
reason after `--`. Typical cases:

- a deliberate network failure with no HTTP response
- an intercept stubbed with a static response, whose status the test sets itself
- a wait that only synchronizes with the page, followed by a UI assertion that
  proves the outcome

```javascript
// eslint-disable-next-line local/require-cypress-response-status -- Network failure has no response status
cy.wait('@data').its('error').should('exist');

// eslint-disable-next-line local/require-cypress-response-status -- Stubbed with a fixture
cy.wait('@stubbedData');
```

Do not disable the rule for a whole file.

Limits: dynamic alias names and return-only helper chains are not checked.
Cross-file helpers, local aliases of response properties, callback variables and
assertions inside nested functions are not analyzed. This is syntax/binding
inspection; it does not prove that every control-flow path executes an assertion.

Run the rule tests with `npm run test-eslint-rules`.
