/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
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
function fn() {
    const bootstrap = Java.type('org.solid.testharness.config.Bootstrap').getInstance();
    if (!bootstrap) {
        karate.log('Scenario was not bootstrapped')
        karate.abort()
        return {}
    }

    const config = bootstrap.getConfig();
    const testHarness = bootstrap.getTestHarness();
    const testSubject = bootstrap.getTestSubject();
    const target = testSubject.targetServer;
    if (target == null) {
        karate.log('Check the environment properties - config is not defined');
    }

    const LogMasker = Java.type('org.solid.testharness.utils.MaskingLogModifier');
    karate.configure('logModifier', LogMasker.INSTANCE);
    karate.configure('connectTimeout', config.connectTimeout);
    karate.configure('readTimeout', config.readTimeout);
    karate.configure('ssl', true);

    const Utils = Java.type('org.solid.testharness.utils.HttpUtils');
    const SolidContainer = Java.type('org.solid.testharness.api.SolidContainer');
    const SolidResource = Java.type('org.solid.testharness.api.SolidResource');
    const RDFUtils = Java.type('org.solid.testharness.utils.RDFUtils');

    return {
        // global variables
        rootTestContainer: new SolidContainer(testSubject.getTestRunContainer()),
        clients: karate.toMap(testHarness.clients),
        webIds: config.webIds,

        // utility libraries
        RDFUtils,
        SolidResource,
        SolidContainer,

        // useful functions
        parseWacAllowHeader: (headers) => Utils.parseWacAllowHeader(headers),
        parseLinkHeaders: (headers) => Utils.parseLinkHeaders(headers),
        resolveUri: (base, target) => Utils.resolveUri(base, target),
        pause: (pause) => java.lang.Thread.sleep(pause),

        // @deprecated - use rootTestContainer.reserveContainer()
        createTestContainer: () => rootTestContainer.reserveContainer(),
        // @deprecated - use rootTestContainer.createContainer()
        createTestContainerImmediate: () => rootTestContainer.createContainer()
    };
}
