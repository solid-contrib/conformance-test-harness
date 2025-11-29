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
 * Polyfills for GraalJS environment.
 * These must be loaded before any other modules that depend on browser/Node.js globals.
 */

// AbortController polyfill (needed by readable-stream)
if (typeof globalThis.AbortController === 'undefined') {
  globalThis.AbortController = class AbortController {
    constructor() {
      this.signal = {
        aborted: false,
        reason: undefined,
        throwIfAborted: function() {
          if (this.aborted) throw this.reason;
        },
        addEventListener: function() {},
        removeEventListener: function() {},
        onabort: null
      };
    }
    abort(reason) {
      this.signal.aborted = true;
      this.signal.reason = reason;
    }
  };
}

// AbortSignal polyfill
if (typeof globalThis.AbortSignal === 'undefined') {
  globalThis.AbortSignal = class AbortSignal {
    constructor() {
      this.aborted = false;
      this.reason = undefined;
    }
    throwIfAborted() {
      if (this.aborted) throw this.reason;
    }
    addEventListener() {}
    removeEventListener() {}
    static abort(reason) {
      const signal = new AbortSignal();
      signal.aborted = true;
      signal.reason = reason;
      return signal;
    }
    static timeout(ms) {
      return new AbortSignal();
    }
  };
}

// TextEncoder polyfill
if (typeof globalThis.TextEncoder === 'undefined') {
  globalThis.TextEncoder = class TextEncoder {
    constructor(encoding = 'utf-8') {
      this.encoding = encoding;
    }
    encode(str) {
      const buf = [];
      for (let i = 0; i < str.length; i++) {
        let c = str.charCodeAt(i);
        if (c < 0x80) {
          buf.push(c);
        } else if (c < 0x800) {
          buf.push(0xc0 | (c >> 6), 0x80 | (c & 0x3f));
        } else if (c < 0xd800 || c >= 0xe000) {
          buf.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
        } else {
          // Surrogate pair
          i++;
          c = 0x10000 + (((c & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff));
          buf.push(
            0xf0 | (c >> 18),
            0x80 | ((c >> 12) & 0x3f),
            0x80 | ((c >> 6) & 0x3f),
            0x80 | (c & 0x3f)
          );
        }
      }
      return new Uint8Array(buf);
    }
  };
}

// TextDecoder polyfill
if (typeof globalThis.TextDecoder === 'undefined') {
  globalThis.TextDecoder = class TextDecoder {
    constructor(encoding = 'utf-8') {
      this.encoding = encoding;
    }
    decode(bytes) {
      if (!bytes) return '';
      const arr = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
      let str = '';
      let i = 0;
      while (i < arr.length) {
        let c = arr[i++];
        if (c < 0x80) {
          str += String.fromCharCode(c);
        } else if (c < 0xe0) {
          str += String.fromCharCode(((c & 0x1f) << 6) | (arr[i++] & 0x3f));
        } else if (c < 0xf0) {
          str += String.fromCharCode(
            ((c & 0x0f) << 12) | ((arr[i++] & 0x3f) << 6) | (arr[i++] & 0x3f)
          );
        } else {
          const cp =
            ((c & 0x07) << 18) |
            ((arr[i++] & 0x3f) << 12) |
            ((arr[i++] & 0x3f) << 6) |
            (arr[i++] & 0x3f);
          str += String.fromCodePoint(cp);
        }
      }
      return str;
    }
  };
}

// queueMicrotask polyfill
if (typeof globalThis.queueMicrotask === 'undefined') {
  globalThis.queueMicrotask = function(callback) {
    Promise.resolve().then(callback);
  };
}

// setTimeout/clearTimeout polyfills (synchronous - for simple use cases)
if (typeof globalThis.setTimeout === 'undefined') {
  globalThis.setTimeout = function(callback, delay) {
    // In synchronous context, just execute immediately
    // This works for the RDFa parser which doesn't rely on actual delays
    if (typeof callback === 'function') {
      callback();
    }
    return 0;
  };
}

if (typeof globalThis.clearTimeout === 'undefined') {
  globalThis.clearTimeout = function(id) {
    // No-op
  };
}

// setImmediate polyfill
if (typeof globalThis.setImmediate === 'undefined') {
  globalThis.setImmediate = function(callback) {
    if (typeof callback === 'function') {
      callback();
    }
    return 0;
  };
}

if (typeof globalThis.clearImmediate === 'undefined') {
  globalThis.clearImmediate = function(id) {
    // No-op
  };
}
