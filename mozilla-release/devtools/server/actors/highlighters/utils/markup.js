/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

"use strict";

const { Cc, Ci, Cu } = require("chrome");
const { getCurrentZoom,
  getRootBindingParent } = require("devtools/shared/layout/utils");
const { on, emit } = require("sdk/event/core");

const lazyContainer = {};

loader.lazyRequireGetter(lazyContainer, "CssLogic",
  "devtools/shared/inspector/css-logic", true);
exports.getComputedStyle = (node) =>
  lazyContainer.CssLogic.getComputedStyle(node);

exports.getBindingElementAndPseudo = (node) =>
  lazyContainer.CssLogic.getBindingElementAndPseudo(node);

loader.lazyGetter(lazyContainer, "DOMUtils", () =>
  Cc["@mozilla.org/inspector/dom-utils;1"].getService(Ci.inIDOMUtils));
exports.hasPseudoClassLock = (...args) =>
  lazyContainer.DOMUtils.hasPseudoClassLock(...args);

exports.addPseudoClassLock = (...args) =>
  lazyContainer.DOMUtils.addPseudoClassLock(...args);

exports.removePseudoClassLock = (...args) =>
  lazyContainer.DOMUtils.removePseudoClassLock(...args);

exports.getCSSStyleRules = (...args) =>
  lazyContainer.DOMUtils.getCSSStyleRules(...args);

const SVG_NS = "http://www.w3.org/2000/svg";
const XUL_NS = "http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul";
const STYLESHEET_URI = "resource://devtools/server/actors/" +
                       "highlighters.css";

const _tokens = Symbol("classList/tokens");

/**
 * Shims the element's `classList` for anonymous content elements; used
 * internally by `CanvasFrameAnonymousContentHelper.getElement()` method.
 */
function ClassList(className) {
  let trimmed = (className || "").trim();
  this[_tokens] = trimmed ? trimmed.split(/\s+/) : [];
}

ClassList.prototype = {
  item(index) {
    return this[_tokens][index];
  },
  contains(token) {
    return this[_tokens].includes(token);
  },
  add(token) {
    if (!this.contains(token)) {
      this[_tokens].push(token);
    }
    emit(this, "update");
  },
  remove(token) {
    let index = this[_tokens].indexOf(token);

    if (index > -1) {
      this[_tokens].splice(index, 1);
    }
    emit(this, "update");
  },
  toggle(token) {
    if (this.contains(token)) {
      this.remove(token);
    } else {
      this.add(token);
    }
  },
  get length() {
    return this[_tokens].length;
  },
  [Symbol.iterator]: function* () {
    for (let i = 0; i < this.tokens.length; i++) {
      yield this[_tokens][i];
    }
  },
  toString() {
    return this[_tokens].join(" ");
  }
};

/**
 * Is this content window a XUL window?
 * @param {Window} window
 * @return {Boolean}
 */
function isXUL(window) {
  return window.document.documentElement.namespaceURI === XUL_NS;
}
exports.isXUL = isXUL;

/**
 * Inject a helper stylesheet in the window.
 */
var installedHelperSheets = new WeakMap();

function installHelperSheet(win, source, type = "agent") {
  if (installedHelperSheets.has(win.document)) {
    return;
  }
  let {Style} = require("sdk/stylesheet/style");
  let {attach} = require("sdk/content/mod");
  let style = Style({source, type});
  attach(style, win);
  installedHelperSheets.set(win.document, style);
}
exports.installHelperSheet = installHelperSheet;

function isNodeValid(node) {
  // Is it null or dead?
  if (!node || Cu.isDeadWrapper(node)) {
    return false;
  }

  // Is it an element node
  if (node.nodeType !== node.ELEMENT_NODE) {
    return false;
  }

  // Is the document inaccessible?
  let doc = node.ownerDocument;
  if (!doc || !doc.defaultView) {
    return false;
  }

  // Is the node connected to the document? Using getBindingParent adds
  // support for anonymous elements generated by a node in the document.
  let bindingParent = getRootBindingParent(node);
  if (!doc.documentElement.contains(bindingParent)) {
    return false;
  }

  return true;
}
exports.isNodeValid = isNodeValid;

/**
 * Helper function that creates SVG DOM nodes.
 * @param {Window} This window's document will be used to create the element
 * @param {Object} Options for the node include:
 * - nodeType: the type of node, defaults to "box".
 * - attributes: a {name:value} object to be used as attributes for the node.
 * - prefix: a string that will be used to prefix the values of the id and class
 *   attributes.
 * - parent: if provided, the newly created element will be appended to this
 *   node.
 */
function createSVGNode(win, options) {
  if (!options.nodeType) {
    options.nodeType = "box";
  }
  options.namespace = SVG_NS;
  return createNode(win, options);
}
exports.createSVGNode = createSVGNode;

/**
 * Helper function that creates DOM nodes.
 * @param {Window} This window's document will be used to create the element
 * @param {Object} Options for the node include:
 * - nodeType: the type of node, defaults to "div".
 * - namespace: if passed, doc.createElementNS will be used instead of
 *   doc.creatElement.
 * - attributes: a {name:value} object to be used as attributes for the node.
 * - prefix: a string that will be used to prefix the values of the id and class
 *   attributes.
 * - parent: if provided, the newly created element will be appended to this
 *   node.
 */
function createNode(win, options) {
  let type = options.nodeType || "div";

  let node;
  if (options.namespace) {
    node = win.document.createElementNS(options.namespace, type);
  } else {
    node = win.document.createElement(type);
  }

  for (let name in options.attributes || {}) {
    let value = options.attributes[name];
    if (options.prefix && (name === "class" || name === "id")) {
      value = options.prefix + value;
    }
    node.setAttribute(name, value);
  }

  if (options.parent) {
    options.parent.appendChild(node);
  }

  return node;
}
exports.createNode = createNode;

/**
 * Every highlighters should insert their markup content into the document's
 * canvasFrame anonymous content container (see dom/webidl/Document.webidl).
 *
 * Since this container gets cleared when the document navigates, highlighters
 * should use this helper to have their markup content automatically re-inserted
 * in the new document.
 *
 * Since the markup content is inserted in the canvasFrame using
 * insertAnonymousContent, this means that it can be modified using the API
 * described in AnonymousContent.webidl.
 * To retrieve the AnonymousContent instance, use the content getter.
 *
 * @param {HighlighterEnv} highlighterEnv
 *        The environemnt which windows will be used to insert the node.
 * @param {Function} nodeBuilder
 *        A function that, when executed, returns a DOM node to be inserted into
 *        the canvasFrame.
 */
function CanvasFrameAnonymousContentHelper(highlighterEnv, nodeBuilder) {
  this.highlighterEnv = highlighterEnv;
  this.nodeBuilder = nodeBuilder;
  this.anonymousContentDocument = this.highlighterEnv.document;
  // XXX the next line is a wallpaper for bug 1123362.
  this.anonymousContentGlobal = Cu.getGlobalForObject(
                                this.anonymousContentDocument);

  this._insert();

  this._onNavigate = this._onNavigate.bind(this);
  this.highlighterEnv.on("navigate", this._onNavigate);

  this.listeners = new Map();
}

CanvasFrameAnonymousContentHelper.prototype = {
  destroy: function () {
    try {
      let doc = this.anonymousContentDocument;
      doc.removeAnonymousContent(this._content);
    } catch (e) {
      // If the current window isn't the one the content was inserted into, this
      // will fail, but that's fine.
    }
    this.highlighterEnv.off("navigate", this._onNavigate);
    this.highlighterEnv = this.nodeBuilder = this._content = null;
    this.anonymousContentDocument = null;
    this.anonymousContentGlobal = null;

    this._removeAllListeners();
  },

  _insert: function () {
    // Insert the content node only if the page isn't in a XUL window, and if
    // the document still exists.
    if (!this.highlighterEnv.document.documentElement ||
        isXUL(this.highlighterEnv.window)) {
      return;
    }
    let doc = this.highlighterEnv.document;

    // For now highlighters.css is injected in content as a ua sheet because
    // <style scoped> doesn't work inside anonymous content (see bug 1086532).
    // If it did, highlighters.css would be injected as an anonymous content
    // node using CanvasFrameAnonymousContentHelper instead.
    installHelperSheet(this.highlighterEnv.window,
      "@import url('" + STYLESHEET_URI + "');");
    let node = this.nodeBuilder();

    // It was stated that hidden documents don't accept
    // `insertAnonymousContent` calls yet. That doesn't seems the case anymore,
    // at least on desktop. Therefore, removing the code that was dealing with
    // that scenario, fixes when we're adding anonymous content in a tab that
    // is not the active one (see bug 1260043 and bug 1260044)
    this._content = doc.insertAnonymousContent(node);
  },

  _onNavigate: function (e, {isTopLevel}) {
    if (isTopLevel) {
      this._removeAllListeners();
      this._insert();
      this.anonymousContentDocument = this.highlighterEnv.document;
    }
  },

  getTextContentForElement: function (id) {
    if (!this.content) {
      return null;
    }
    return this.content.getTextContentForElement(id);
  },

  setTextContentForElement: function (id, text) {
    if (this.content) {
      this.content.setTextContentForElement(id, text);
    }
  },

  setAttributeForElement: function (id, name, value) {
    if (this.content) {
      this.content.setAttributeForElement(id, name, value);
    }
  },

  getAttributeForElement: function (id, name) {
    if (!this.content) {
      return null;
    }
    return this.content.getAttributeForElement(id, name);
  },

  removeAttributeForElement: function (id, name) {
    if (this.content) {
      this.content.removeAttributeForElement(id, name);
    }
  },

  hasAttributeForElement: function (id, name) {
    return typeof this.getAttributeForElement(id, name) === "string";
  },

  /**
   * Add an event listener to one of the elements inserted in the canvasFrame
   * native anonymous container.
   * Like other methods in this helper, this requires the ID of the element to
   * be passed in.
   *
   * Note that if the content page navigates, the event listeners won't be
   * added again.
   *
   * Also note that unlike traditional DOM events, the events handled by
   * listeners added here will propagate through the document only through
   * bubbling phase, so the useCapture parameter isn't supported.
   * It is possible however to call e.stopPropagation() to stop the bubbling.
   *
   * IMPORTANT: the chrome-only canvasFrame insertion API takes great care of
   * not leaking references to inserted elements to chrome JS code. That's
   * because otherwise, chrome JS code could freely modify native anon elements
   * inside the canvasFrame and probably change things that are assumed not to
   * change by the C++ code managing this frame.
   * See https://wiki.mozilla.org/DevTools/Highlighter#The_AnonymousContent_API
   * Unfortunately, the inserted nodes are still available via
   * event.originalTarget, and that's what the event handler here uses to check
   * that the event actually occured on the right element, but that also means
   * consumers of this code would be able to access the inserted elements.
   * Therefore, the originalTarget property will be nullified before the event
   * is passed to your handler.
   *
   * IMPL DETAIL: A single event listener is added per event types only, at
   * browser level and if the event originalTarget is found to have the provided
   * ID, the callback is executed (and then IDs of parent nodes of the
   * originalTarget are checked too).
   *
   * @param {String} id
   * @param {String} type
   * @param {Function} handler
   */
  addEventListenerForElement: function (id, type, handler) {
    if (typeof id !== "string") {
      throw new Error("Expected a string ID in addEventListenerForElement but" +
        " got: " + id);
    }

    // If no one is listening for this type of event yet, add one listener.
    if (!this.listeners.has(type)) {
      let target = this.highlighterEnv.pageListenerTarget;
      target.addEventListener(type, this, true);
      // Each type entry in the map is a map of ids:handlers.
      this.listeners.set(type, new Map());
    }

    let listeners = this.listeners.get(type);
    listeners.set(id, handler);
  },

  /**
   * Remove an event listener from one of the elements inserted in the
   * canvasFrame native anonymous container.
   * @param {String} id
   * @param {String} type
   */
  removeEventListenerForElement: function (id, type) {
    let listeners = this.listeners.get(type);
    if (!listeners) {
      return;
    }
    listeners.delete(id);

    // If no one is listening for event type anymore, remove the listener.
    if (!this.listeners.has(type)) {
      let target = this.highlighterEnv.pageListenerTarget;
      target.removeEventListener(type, this, true);
    }
  },

  handleEvent: function (event) {
    let listeners = this.listeners.get(event.type);
    if (!listeners) {
      return;
    }

    // Hide the originalTarget property to avoid exposing references to native
    // anonymous elements. See addEventListenerForElement's comment.
    let isPropagationStopped = false;
    let eventProxy = new Proxy(event, {
      get: (obj, name) => {
        if (name === "originalTarget") {
          return null;
        } else if (name === "stopPropagation") {
          return () => {
            isPropagationStopped = true;
          };
        }
        return obj[name];
      }
    });

    // Start at originalTarget, bubble through ancestors and call handlers when
    // needed.
    let node = event.originalTarget;
    while (node) {
      let handler = listeners.get(node.id);
      if (handler) {
        handler(eventProxy, node.id);
        if (isPropagationStopped) {
          break;
        }
      }
      node = node.parentNode;
    }
  },

  _removeAllListeners: function () {
    if (this.highlighterEnv) {
      let target = this.highlighterEnv.pageListenerTarget;
      for (let [type] of this.listeners) {
        target.removeEventListener(type, this, true);
      }
    }
    this.listeners.clear();
  },

  getElement: function (id) {
    let classList = new ClassList(this.getAttributeForElement(id, "class"));

    on(classList, "update", () => {
      this.setAttributeForElement(id, "class", classList.toString());
    });

    return {
      getTextContent: () => this.getTextContentForElement(id),
      setTextContent: text => this.setTextContentForElement(id, text),
      setAttribute: (name, val) => this.setAttributeForElement(id, name, val),
      getAttribute: name => this.getAttributeForElement(id, name),
      removeAttribute: name => this.removeAttributeForElement(id, name),
      hasAttribute: name => this.hasAttributeForElement(id, name),
      addEventListener: (type, handler) => {
        return this.addEventListenerForElement(id, type, handler);
      },
      removeEventListener: (type, handler) => {
        return this.removeEventListenerForElement(id, type, handler);
      },
      classList
    };
  },

  get content() {
    if (!this._content || Cu.isDeadWrapper(this._content)) {
      return null;
    }
    return this._content;
  },

  /**
   * The canvasFrame anonymous content container gets zoomed in/out with the
   * page. If this is unwanted, i.e. if you want the inserted element to remain
   * unzoomed, then this method can be used.
   *
   * Consumers of the CanvasFrameAnonymousContentHelper should call this method,
   * it isn't executed automatically. Typically, AutoRefreshHighlighter can call
   * it when _update is executed.
   *
   * The matching element will be scaled down or up by 1/zoomLevel (using css
   * transform) to cancel the current zoom. The element's width and height
   * styles will also be set according to the scale. Finally, the element's
   * position will be set as absolute.
   *
   * Note that if the matching element already has an inline style attribute, it
   * *won't* be preserved.
   *
   * @param {DOMNode} node This node is used to determine which container window
   * should be used to read the current zoom value.
   * @param {String} id The ID of the root element inserted with this API.
   */
  scaleRootElement: function (node, id) {
    let zoom = getCurrentZoom(node);
    let value = "position:absolute;width:100%;height:100%;";

    if (zoom !== 1) {
      value = "position:absolute;";
      value += "transform-origin:top left;transform:scale(" + (1 / zoom) + ");";
      value += "width:" + (100 * zoom) + "%;height:" + (100 * zoom) + "%;";
    }

    this.setAttributeForElement(id, "style", value);
  }
};
exports.CanvasFrameAnonymousContentHelper = CanvasFrameAnonymousContentHelper;