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

import js from "@eslint/js";
import globals from "globals";

export default [
  js.configs.recommended,
  {
    files: ["**/*.{js,mjs,cjs}"],
    languageOptions: {
      ecmaVersion: 2020,
      sourceType: "module",
      globals: {
        ...globals.browser,
        $: "readonly",
        Chart: "readonly",
        ChartDataLabels: "readonly",
        DataTable: "readonly",
        bootstrap: "readonly",
        WorkflowEdit: "readonly"
      }
    },
    rules: {
      'function-paren-newline': ['error', 'multiline-arguments'],
      'operator-linebreak': ['error', 'after'],
      'max-len': [
        'error',
        {
          comments: 120,
          code: 120
        }
      ],
      'comma-dangle': ['error', 'never'],
      'no-plusplus': ['error', { allowForLoopAfterthoughts: true }],
      'func-names': 'off',
      'no-use-before-define': 'off',
      strict: 'off',
      'no-console': 'error'
    }
  },
  {
    files: ["**/pages/workflowRunDetails/**/*.js", "**/pages/workflowRunList/index.js"],
    languageOptions: {
      globals: {
        statusToClassName: "readonly",
        button: "readonly"
      }
    }
  },
  // For Cypress tests: adding Cypress globals for IntelliJ's error highlighting
  {
    files: ["cypress/e2e/**/*.js", "cypress/support/**/*.js"],
    languageOptions: {
      globals: {
        // keep browser globals in tests
        ...globals.browser,
        // Mocha globals: describe, it, beforeEach, context, etc.
        ...globals.mocha,
        // Cypress-specific globals
        cy: "readonly",
        Cypress: "readonly",
        expect: "readonly"
      }
    }
  }
];
