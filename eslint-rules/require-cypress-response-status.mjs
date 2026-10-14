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

function isNetworkAlias(node) {
  if (node?.type === 'ArrayExpression') {
    return node.elements.some(isNetworkAlias);
  }

  const value = node?.type === 'Literal' ? node.value :
    node?.type === 'TemplateLiteral' && node.expressions.length === 0 ? node.quasis[0].value.cooked : null;
  return typeof value === 'string' && value.startsWith('@');
}

const comparisons = new Set([
  'eq', 'equal', 'equals', 'eql', 'strictEqual', 'oneOf', 'within', 'gt', 'gte', 'lt', 'lte'
]);

function memberPath(node) {
  if (node?.type === 'Identifier') {
    return { root: node, parts: [] };
  }
  if (node?.type !== 'MemberExpression') {
    return null;
  }
  const parent = memberPath(node.object);
  const key = node.computed ? node.property.value : node.property.name;
  return parent && key !== undefined ? { root: parent.root, parts: [...parent.parts, String(key)] } : null;
}

function bindingForPath(parameter, parts, sourceCode, callback) {
  if (parameter?.type === 'Identifier') {
    const variable = sourceCode.getDeclaredVariables(callback).find(v => v.identifiers.includes(parameter));
    return variable ? { variable, parts } : null;
  }
  if (parameter?.type === 'ObjectPattern') {
    const property = parameter.properties.find(p => (p.key?.name ?? p.key?.value) === parts[0]);
    return bindingForPath(property?.value, parts.slice(1), sourceCode, callback);
  }
  return null;
}

function assertedValue(node) {
  if (node.type !== 'CallExpression' || node.callee.type !== 'MemberExpression' ||
      !comparisons.has(node.callee.property.name) || node.arguments.length === 0) {
    return null;
  }
  let root = node.callee.object;
  if (root.type === 'Identifier' && root.name === 'assert' && node.arguments.length >= 2) {
    return node.arguments[0];
  }
  while (root.type === 'MemberExpression') {
    root = root.object;
  }
  return root.type === 'CallExpression' && root.callee.name === 'expect' ? root.arguments[0] : null;
}

function callbackChecks(callback, paths, method, sourceCode) {
  const targets = paths.map(parts => {
    const spread = method === 'spread';
    const index = spread ? Number(parts[0]) : 0;
    const relative = spread || method === 'each' ? parts.slice(1) : parts;
    return bindingForPath(callback.params[index], relative, sourceCode, callback);
  });
  if (targets.some(target => !target)) {
    return false;
  }
  const checked = new Set();
  const pending = [callback.body];
  while (pending.length) {
    const node = pending.pop();
    if (['ArrowFunctionExpression', 'FunctionExpression', 'FunctionDeclaration'].includes(node.type)) {
      continue;
    }
    const path = memberPath(assertedValue(node));
    targets.forEach((target, index) => {
      if (path && JSON.stringify(path.parts) === JSON.stringify(target.parts) &&
          target.variable.references.some(reference => reference.identifier === path.root)) {
        checked.add(index);
      }
    });
    for (const key of sourceCode.visitorKeys[node.type] ?? []) {
      const children = Array.isArray(node[key]) ? node[key] : [node[key]];
      pending.push(...children.filter(Boolean));
    }
  }
  return checked.size === targets.length;
}

function checksStatus(calls, paths, sourceCode) {
  for (const call of calls) {
    const method = call.callee.computed ? call.callee.property.value : call.callee.property.name;
    if (method === 'its' && typeof call.arguments[0]?.value === 'string') {
      const prefix = call.arguments[0].value.split('.');
      if (!paths.every(parts => prefix.every((part, index) => part === parts[index]))) {
        return false;
      }
      paths = paths.map(parts => parts.slice(prefix.length));
    }
    if (['should', 'and'].includes(method)) {
      const assertion = call.arguments[0]?.value;
      if (paths.length === 1 && paths[0].length === 0 &&
          comparisons.has(assertion?.replace(/^be\./, '')) && call.arguments.length >= 2) {
        return true;
      }
      if (paths.length === 1 && ['have.property', 'have.nested.property'].includes(assertion) &&
          call.arguments[1]?.value === paths[0].join('.') && call.arguments.length >= 3) {
        return true;
      }
    }
    if (['then', 'should', 'and', 'each', 'spread'].includes(method)) {
      if (['each', 'spread'].includes(method) && !paths.every(parts => /^\d+$/.test(parts[0]))) {
        continue;
      }
      const callback = call.arguments.find(arg => ['ArrowFunctionExpression', 'FunctionExpression'].includes(arg.type));
      if (callback && callbackChecks(callback, paths, method, sourceCode)) {
        return true;
      }
    }
  }
  return false;
}

// Dynamic alias names and return-only helper chains are outside this rule's analysis.
export default {
  meta: {
    type: 'problem',
    docs: {
      description: 'Require expected response status assertions for Cypress network waits'
    },
    schema: [],
    messages: {
      missingStatusAssertion: 'Assert the expected response status after waiting for a network alias.'
    }
  },
  create(context) {
    return {
      CallExpression(node) {
        const callee = node.callee;
        if (callee.type !== 'MemberExpression' ||
            callee.object.type !== 'Identifier' || callee.object.name !== 'cy') {
          return;
        }

        const method = callee.computed ? callee.property.value : callee.property.name;
        if (method === 'wait' && isNetworkAlias(node.arguments[0])) {
          const calls = [];
          let outer = node;
          while (outer.parent.type === 'MemberExpression' && outer.parent.object === outer &&
              outer.parent.parent.type === 'CallExpression') {
            outer = outer.parent.parent;
            calls.push(outer);
          }
          const argument = node.arguments[0];
          const paths = argument.type === 'ArrayExpression' ?
            argument.elements.map((element, index) => [String(index), 'response', 'statusCode']) :
            [['response', 'statusCode']];
          if (outer.parent.type === 'ExpressionStatement' && !checksStatus(calls, paths, context.sourceCode)) {
            context.report({ node, messageId: 'missingStatusAssertion' });
          }
        }
      }
    };
  }
};
