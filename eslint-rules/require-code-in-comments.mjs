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

/**
 * Requires code references in comments to be wrapped in backticks, so that a reader can tell them
 * apart from prose and a rename shows up when searching for the old name.
 *
 * Reported are calls such as `should()` or `cy.get('#id(x)')` and bare `cy.` / `Cypress.` references
 * such as `cy.intercept`. Bare names without parentheses are deliberately not reported: a comment
 * gives no way to tell the word 'login' from the function of the same name, which would make the
 * rule unusable in prose.
 *
 * A reference ends with the line it stands on. A code sample written over several lines is left to
 * its author, who marks it up as a fenced block; telling such a sample apart from a paragraph that
 * happens to carry a parenthesis takes guesswork this rule deliberately does not do.
 */

/**
 * The name and the opening parenthesis of a call, the end of its arguments is scanned for
 * separately. The name is taken from its start, which a word boundary would miss: a '$' is no word
 * character, so `\b` never holds in front of `$scope` and the rest of the name would match alone.
 */
const CALL_START_PATTERN = /(?<![\w$])[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*\(/g;

/**
 * A bare reference, `cy.intercept`, which no call follows. The names behind the dots are taken
 * through a lookahead and repeated, which holds them together: matched as an ordinary group, the
 * engine would fall back to a shorter prefix where the lookahead holds and cut the name in two.
 *
 * A dot, a hyphen or a slash in front of it says the name is part of something longer - a longer
 * path of names, a compound word, a file name - which is left whole rather than cut open at the
 * 'cy' inside it.
 */
const CYPRESS_REFERENCE_PATTERN =
  /(?<![\w$./-])(?:cy|Cypress)(?=((?:\.[A-Za-z_$][\w$]*)+))\1(?![\w$])(?!\s*\()/g;

/** A link of a cascade: the name behind the dot and, when it is called, its opening parenthesis. */
const CASCADE_LINK_PATTERN = /^\.[A-Za-z_$][\w$]*\(?/;

/** Conventions such as 'TODO(name)' that look like a call but name a person, not a function. */
const MARKER_PATTERN = /^(?:TODO|FIXME|NOTE|XXX|HACK)\(/i;

/** The plural a word in prose is given, 'the file(s)', which looks like a call taking a name. */
const PLURAL_PATTERN = /^[A-Za-z_$][\w$]*\((?:s|es|ies)\)$/;

/**
 * The JSDoc tags whose braces hold prose rather than a type. Naming those rather than the tags
 * that do carry one leaves an unknown tag on the harmless side: its type keeps its markup and at
 * worst a reference inside it goes unreported, rather than being rewritten and broken.
 */
const PROSE_TAGS = [
  'example', 'see', 'description', 'summary', 'file', 'fileoverview', 'overview',
  'license', 'copyright', 'author', 'todo', 'tutorial'
];

/**
 * The brace opening what a tag types, '@param {string}', or an inline tag, '{@link foo}'. A tag is
 * taken wherever it stands, a second one on the line included: an '@' in a sentence then holds a
 * brace of prose silent, which costs a report, while missing a tag would rewrite a type and break
 * it.
 */
const TYPE_START_PATTERN = new RegExp(
  `(?<![\\w$])@(?!(?:${PROSE_TAGS.join('|')})\\b)[A-Za-z]+[ \\t]*\\{|\\{@[A-Za-z]+`,
  'g'
);

/**
 * A tag whose body is a sample rather than prose. It has to open its line: an '@example' named in
 * a sentence describes the tag, it does not start a block.
 */
const BLOCK_TAG_PATTERN = /^([ \t]*\*?[ \t]*)@example\b/gm;

/**
 * A tag with the indentation it carries. What ends a sample is a tag standing no deeper than the
 * '@example' that opened it; a decorator inside the sample is indented past that and is part of
 * it, whatever the comment indents its lines by.
 */
const TAG_START_PATTERN = /^([ \t]*\*?[ \t]*)@[A-Za-z]/;

/** An address, from its scheme up to the quote or space that ends it, its parentheses included. */
const URL_PATTERN = /\b[a-z][\w+.-]*:\/\/[^\s'"`]+/gi;

/** The prefix of a line inside a comment, the ' * ' of a block comment or plain indentation. */
const LINE_PREFIX_PATTERN = /^[ \t]*\*?[ \t]*/;

/**
 * A regular expression literal: a body of ordinary characters, escapes and character classes
 * followed by the flags. Matching the classes keeps a '/' inside one from closing the literal.
 *
 * It is looked for where a literal can stand, inside an argument list. A '/' outside of one opens
 * no literal: it divides, or it is part of a path, and reading it as one would hold whatever
 * stands behind it silent.
 */
const REGEXP_LITERAL_PATTERN = /^\/(?![*/])(?:[^\\/[\n]|\\[^\n]|\[(?:[^\\\]\n]|\\[^\n])*\])+\/[a-z]*/;

/** Characters a '/' may follow to open a regular expression, after a value it divides instead. */
const REGEXP_START_PATTERN = /[([{,;:=!&|?+*%~^<>-]/;

/** Comments the tooling reads itself, for example 'eslint-disable-next-line' or 'istanbul ignore'. */
const DIRECTIVE_PATTERN = /^\s*(?:eslint|jshint|jslint|istanbul|prettier)\b/;

/**
 * Directives only a block comment carries. As the first word of a line comment they open a
 * sentence far more often than they declare a name, and would hold the whole comment silent.
 */
const BLOCK_DIRECTIVE_PATTERN = /^\s*(?:globals?|exported)\b/;

function getLineEnd(text, index) {
  const newline = text.indexOf('\n', index);
  return newline === -1 ? text.length : newline;
}

/** The line without the prefix its comment carries, so that what it opens with can be looked at. */
function stripPrefix(line) {
  return line.replace(LINE_PREFIX_PATTERN, '');
}

/**
 * Index ranges of the comment that already are marked up as code. A run of backticks opens a span
 * that a run of the same length closes, three or more of them a fenced block spanning lines.
 *
 * An unclosed marker marks the rest of its line, a fence the rest of the comment. Marking rather
 * than ignoring it keeps the fix idempotent: a stray backtick in prose would otherwise pair up with
 * the backticks of a previous fix, which moves the following reference out of the span again and
 * makes every further run add another pair.
 */
function getCodeRanges(text) {
  const ranges = [];
  let index = 0;

  while (index < text.length) {
    if (text[index] === '\\') {
      index += 2;
    } else if (text[index] !== '`') {
      index += 1;
    } else {
      const marker = /^`+/.exec(text.slice(index))[0];
      const limit = marker.length >= 3 ? text.length : getLineEnd(text, index);
      const closing = text.indexOf(marker, index + marker.length);
      const end = closing !== -1 && closing < limit ? closing + marker.length : limit;

      ranges.push([index, end]);
      index = end;
    }
  }

  return ranges;
}

/**
 * Index behind the brace closing the one at openIndex, or -1 when it is left open. A type carries
 * on over the lines it is broken across, blank ones included, up to the tag that follows it. A
 * brace left open behind a tag therefore holds the paragraphs up to that tag silent.
 */
function findBraceEnd(text, openIndex) {
  let depth = 0;

  for (let index = openIndex; index < text.length; index += 1) {
    if (text[index] === '\n') {
      // a type is broken across lines and grouped by blank ones, the next tag is where it ends
      if (stripPrefix(text.slice(index + 1, getLineEnd(text, index + 1))).startsWith('@')) {
        return -1;
      }
    } else if (text[index] === '{') {
      depth += 1;
    } else if (text[index] === '}') {
      depth -= 1;

      if (depth === 0) {
        return index + 1;
      }
    }
  }

  return -1;
}

/**
 * Index where the body of a tag ends: the next tag standing no deeper than it, or the end of the
 * comment.
 */
function findTagEnd(text, index, indent) {
  for (let line = text.indexOf('\n', index); line !== -1; line = text.indexOf('\n', line + 1)) {
    const tag = TAG_START_PATTERN.exec(text.slice(line + 1, getLineEnd(text, line + 1)));

    if (tag && tag[1].length <= indent) {
      return line;
    }
  }

  return text.length;
}

/**
 * Index ranges of the samples the block tags carry. What follows '@example' is rendered as a block
 * of code by the tooling itself, where a pair of backticks would stand as the two characters it is
 * made of rather than mark anything up.
 */
function getBlockRanges(text, isBlock) {
  if (!isBlock) {
    return [];
  }

  return Array.from(text.matchAll(BLOCK_TAG_PATTERN)).map((match) => {
    const start = match.index + match[0].length;

    return [start, findTagEnd(text, start, match[1].length)];
  });
}

/**
 * Index ranges of what the JSDoc tags of the comment type, '{string}' and its like, and of the
 * inline tags. The braces are counted rather than matched, so that a record type nested in another
 * is covered as well.
 */
function getTypeRanges(text) {
  const ranges = [];

  Array.from(text.matchAll(TYPE_START_PATTERN)).forEach((match) => {
    // the brace opens an inline tag at the start of the match and a type at its end
    const start = match[0].startsWith('{') ? match.index : match.index + match[0].length - 1;
    const end = findBraceEnd(text, start);

    if (end !== -1) {
      ranges.push([start, end]);
    }
  });

  return ranges;
}

function getUrlRanges(text) {
  return Array.from(text.matchAll(URL_PATTERN)).map((match) => [match.index, match.index + match[0].length]);
}

/**
 * Index ranges the rule keeps out of: what is marked up as code already, the type a JSDoc tag
 * carries, the sample an '@example' introduces and an address. All but the first are read by the
 * tooling rather than by a person, and a pair of backticks would break them for both.
 */
function getIgnoredRanges(text, isBlock) {
  return [...getCodeRanges(text), ...getTypeRanges(text), ...getBlockRanges(text, isBlock), ...getUrlRanges(text)];
}

/**
 * Whether one of those ranges covers the reference or runs into it, in which case the reference is
 * left alone: its markup would land inside markup that is already there, inside an address or
 * inside a type, or its backticks would stand against those of the span next to it, where they
 * would read as a delimiter of their own rather than as a pair.
 *
 * A range the reference holds whole is no such case: `cy.wrap(`x`)` carries its template literal
 * with it, `cy.visit('http://x')` its address, and `getDelimiter` sets the markup around the
 * reference wide enough to hold what is inside it.
 */
function isIgnored(reference, ranges) {
  return ranges.some(([start, end]) => reference.start <= end && start <= reference.end &&
    !(reference.start <= start && end <= reference.end));
}

/**
 * Index of the last character of the regular expression literal opening at index, or index itself
 * when none opens there or it is not closed on the line. The scan starts inside an argument list,
 * so a '/' opens a literal where an operator or a bracket stands in front of it and divides where
 * a value does.
 */
function skipRegExp(text, index) {
  const preceding = text.slice(text.lastIndexOf('\n', index - 1) + 1, index).trimEnd();
  const opens = REGEXP_START_PATTERN.test(preceding.slice(-1));
  const literal = opens && REGEXP_LITERAL_PATTERN.exec(text.slice(index, getLineEnd(text, index)));

  return literal ? index + literal[0].length - 1 : index;
}

/**
 * Index behind the parenthesis that closes the call opening at openIndex, or -1 when the call is
 * not closed on that line.
 *
 * The arguments are scanned rather than matched, so that a parenthesis inside a quoted selector or
 * message does not end the call early and a nested call is covered by the span of the outer one.
 * A call left open at the end of the line is reported by nobody: without its arguments there is no
 * span to wrap, and a sample written over several lines belongs into a fence its author writes.
 */
function findCallEnd(text, openIndex) {
  const limit = getLineEnd(text, openIndex);
  let depth = 0;
  let quote = null;

  for (let index = openIndex; index < limit; index += 1) {
    const character = text[index];

    if (quote) {
      if (character === '\\') {
        index += 1;
      } else if (character === quote) {
        quote = null;
      }
    } else if (character === "'" || character === '"' || character === '`') {
      quote = character;
    } else if (character === '/') {
      // a parenthesis inside a regular expression neither opens nor closes the argument list
      index = skipRegExp(text, index);
    } else if (character === '(') {
      depth += 1;
    } else if (character === ')') {
      depth -= 1;

      if (depth === 0) {
        return index + 1;
      }
    }
  }

  return -1;
}

/**
 * Index behind the last link of the cascade continuing at index, or index itself when nothing
 * chains there.
 *
 * A chained call belongs into a single span: wrapping every link on its own would put backticks
 * around the parts and leave the dots between them as prose. A link left open ends the cascade,
 * the links up to it still form a span that can be wrapped.
 *
 * A link without arguments carries the cascade on to the next call, so that the '.first' of
 * `cy.get('a').first.click()` stays inside the span. On its own it ends the cascade rather than
 * joining it: a full stop in prose looks exactly like it.
 *
 * The links are walked rather than recursed through: a cascade is as long as its author writes it,
 * and a very long one would otherwise fill the stack and take the whole file down with it.
 */
function findCascadeEnd(text, index) {
  let end = index;
  let reached = index;

  for (let link = CASCADE_LINK_PATTERN.exec(text.slice(reached)); link;
    link = CASCADE_LINK_PATTERN.exec(text.slice(reached))) {
    const linkEnd = reached + link[0].length;

    if (!link[0].endsWith('(')) {
      // a name without arguments counts once the call it carries the cascade on to is there
      reached = linkEnd;
    } else {
      const callEnd = findCallEnd(text, linkEnd - 1);

      if (callEnd === -1) {
        return end;
      }

      end = callEnd;
      reached = callEnd;
    }
  }

  return end;
}

/** A run of backticks longer than every run inside the text, so that it delimits the text. */
function getDelimiter(text) {
  const runs = Array.from(text.matchAll(/`+/g)).map((match) => match[0].length);

  return '`'.repeat(Math.max(0, ...runs) + 1);
}

function isPartOf(reference, other) {
  return other.start <= reference.start && reference.end <= other.end &&
    other.end - other.start > reference.end - reference.start;
}

/** The references of a comment, without duplicates and in the order they appear. */
function findReferences(text) {
  const unique = new Map();

  Array.from(text.matchAll(CALL_START_PATTERN)).forEach((match) => {
    if (MARKER_PATTERN.test(match[0])) {
      return;
    }

    // a link whose cascade is broken in front of it keeps the dot inside the span
    const start = text[match.index - 1] === '.' ? match.index - 1 : match.index;
    const callEnd = findCallEnd(text, match.index + match[0].length - 1);

    if (callEnd !== -1) {
      const end = findCascadeEnd(text, callEnd);
      const span = text.slice(start, end);

      if (!PLURAL_PATTERN.test(span)) {
        unique.set(`${start}:${end}`, { text: span, start, end });
      }
    }
  });

  Array.from(text.matchAll(CYPRESS_REFERENCE_PATTERN)).forEach((match) => {
    const start = match.index;
    const end = start + match[0].length;

    unique.set(`${start}:${end}`, { text: match[0], start, end });
  });

  const references = Array.from(unique.values());

  // a nested call, a further link of a cascade and an argument are inside the span around them
  return references
    .filter((reference) => !references.some((other) => isPartOf(reference, other)))
    .sort((first, second) => first.start - second.start);
}

export default {
  meta: {
    type: 'suggestion',
    docs: {
      description: 'Require code references in comments to be wrapped in backticks'
    },
    fixable: 'code',
    schema: [],
    messages: {
      requireBackticks: 'Code reference "{{reference}}" in a comment must be wrapped in backticks.'
    }
  },

  create(context) {
    const sourceCode = context.sourceCode;

    return {
      Program() {
        sourceCode.getAllComments().forEach((comment) => {
          const isDirective = DIRECTIVE_PATTERN.test(comment.value) ||
            (comment.type === 'Block' && BLOCK_DIRECTIVE_PATTERN.test(comment.value));

          if (comment.type === 'Shebang' || comment.type === 'Hashbang' || isDirective) {
            return;
          }

          const ranges = getIgnoredRanges(comment.value, comment.type === 'Block');

          findReferences(comment.value).forEach((reference) => {
            if (isIgnored(reference, ranges)) {
              return;
            }

            // the range of a comment starts at its '//' or '/*', its value two characters later
            const start = comment.range[0] + 2 + reference.start;
            const end = comment.range[0] + 2 + reference.end;
            const marker = getDelimiter(reference.text);

            context.report({
              loc: {
                start: sourceCode.getLocFromIndex(start),
                end: sourceCode.getLocFromIndex(end)
              },
              messageId: 'requireBackticks',
              data: { reference: reference.text },
              fix: (fixer) => fixer.replaceTextRange([start, end], `${marker}${reference.text}${marker}`)
            });
          });
        });
      }
    };
  }
};
