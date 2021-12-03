/*
 * Copyright 2011-2021 The OTP authors
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

const lintRules = {
  'operator-linebreak': ['error', 'after'],
  'max-len': [
    'error',
    {
      comments: 120,
      code: 120
    }
  ],
  'comma-dangle': ['error', 'never'],
  'no-plusplus': ['error', { allowForLoopAfterthoughts: true }]
};

if (process.env.CI) {
  Object.assign(lintRules, {
    strict: 'off',
    'func-names': 'off',
    'no-use-before-define': 'off',
    'no-param-reassign': 'off',
    'no-unused-vars': 'off',
    'no-undef': 'off',
    'no-restricted-globals': 'off',
    'default-case': 'off',
    'prefer-const': 'off',
    'no-alert': 'off',
    'array-callback-return': 'off',
    radix: 'off',
    'no-new': 'off',
    'vars-on-top': 'off',
    'no-loop-func': 'off',
    'no-unused-expressions': 'off',
    'no-restricted-syntax': 'off',
    'no-useless-concat': 'off',
    'block-scoped-var': 'off',
    'no-useless-call': 'off',
    'no-self-assign': 'off',
    'no-underscore-dangle': 'off',
    'no-useless-escape': 'off'
  });
}

module.exports = {
  env: {
    browser: true,
    commonjs: true,
    jquery: true
  },
  extends: [
    'airbnb-base'
  ],
  parserOptions: {
    ecmaVersion: 6
  },
  rules: lintRules,
  overrides: [
    {
      files: ['grails-app/assets/javascripts/**/*.js']
    }
  ]
};
