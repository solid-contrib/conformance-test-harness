/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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
 * RDFa Parser wrapper for GraalJS integration.
 *
 * Wraps rdfa-streaming-parser to provide a simple interface callable from Java.
 * Converts RDF/JS terms to plain objects for Java interop.
 */

const { RdfaParser } = require('rdfa-streaming-parser');

/**
 * Convert an RDF/JS Term to a plain object for Java interop.
 *
 * @param {Object} term - RDF/JS Term (NamedNode, BlankNode, Literal, DefaultGraph)
 * @returns {Object} Plain object with termType, value, language, datatype
 */
function termToObject(term) {
  if (term === null || term === undefined) {
    return null;
  }
  return {
    termType: term.termType,
    value: term.value,
    language: term.language || null,
    datatype: term.datatype ? term.datatype.value : null
  };
}

/**
 * Parse HTML/XHTML containing RDFa and call the handler for each quad.
 *
 * @param {string} html - The HTML/XHTML content to parse
 * @param {string} baseIRI - The base IRI for resolving relative IRIs
 * @param {string} contentType - The content type ('text/html' or 'application/xhtml+xml')
 * @param {Object} quadHandler - Java callback object with onQuad(s, p, o, g) method
 * @returns {Object} Result object with success boolean and optional error message
 */
function parseRdfa(html, baseIRI, contentType, quadHandler) {
  try {
    const parser = new RdfaParser({
      baseIRI: baseIRI,
      contentType: contentType,
      // RDFa 1.1 profile
      profile: 'html'
    });

    // Set up quad handler - called for each parsed quad
    parser.on('data', (quad) => {
      try {
        quadHandler.onQuad(
          termToObject(quad.subject),
          termToObject(quad.predicate),
          termToObject(quad.object),
          termToObject(quad.graph)
        );
      } catch (e) {
        // Ignore errors in quad handling - continue parsing
      }
    });

    // Track errors (ignoring langString validation errors which occur when
    // HTML content lacks explicit lang attributes - these are validation
    // warnings not parsing failures, and semargl didn't enforce this)
    let parseError = null;
    parser.on('error', (err) => {
      const msg = err.message || String(err);
      // Ignore known non-fatal errors
      if (msg.includes('rdf:langString requires a language tag') ||
          msg.includes('termType')) {
        return;
      }
      parseError = err;
    });

    // Parse synchronously (htmlparser2 processes synchronously despite streaming API)
    parser.write(html);
    parser.end();

    if (parseError) {
      return {
        success: false,
        error: parseError.message || String(parseError)
      };
    }

    return { success: true };
  } catch (err) {
    return {
      success: false,
      error: err.message || String(err)
    };
  }
}

/**
 * Simple test function to verify the bundle loads correctly.
 *
 * @returns {string} Version info
 */
function getVersion() {
  return 'rdfa-parser-bundle 1.0.0 (rdfa-streaming-parser)';
}

// Export for GraalJS
module.exports = {
  parseRdfa,
  getVersion
};

// Also expose as default export for webpack library config
module.exports.default = {
  parseRdfa,
  getVersion
};
