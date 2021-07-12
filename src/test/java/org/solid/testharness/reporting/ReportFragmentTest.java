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
package org.solid.testharness.reporting;

import com.intuit.karate.Results;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.Template;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.SOLID_TEST;
import org.solid.common.vocab.SPEC;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.RDFUtils;
import org.solid.testharness.utils.TestUtils;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportFragmentTest {
    private static final Logger logger = LoggerFactory.getLogger(ReportFragmentTest.class);

    @Inject
    DataRepository dataRepository;

    @Inject
    Engine engine;

    private Template testTemplate;

    void addLocator(final @Observes EngineBuilder engineBuilder) {
        engineBuilder.addLocator(new TestTemplateLocator());
    }

    @BeforeAll
    void setup() throws MalformedURLException {
        testTemplate = engine.getTemplate("src/test/resources/reporting/test.html");
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.clear();
        }
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/harness-sample.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/reporting/testsuite-results-sample.ttl"),
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString());
    }

    @Test
    void stepReport() throws IOException {
        final IRI testCaseIri = iri(
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString(),
                "#group1-feature1"
        );
        final TestCase testCase = new TestCase(testCaseIri);
        final Scenario scenario = testCase.getScenarios().get(0);
        final Step step = scenario.getSteps().get(2);
        step.getModel().remove(iri(step.getScenario()), null, null);
        logger.debug("Step Model:\n{}", TestUtils.toTurtle(step.getModel()));

        final String report = render("steps", List.of(step));
        logger.debug("Report:\n{}", report);

        final Model reportModel = RDFUtils.parseRdfa(report, "https://example.org");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        assertTrue(Models.isomorphic(reportModel, step.getModel()));
    }

    @Test
    void scenarioReport() throws IOException {
        final IRI testCaseIri = iri(
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString(),
                "#group1-feature1"
        );
        final TestCase testCase = new TestCase(testCaseIri);
        final Scenario scenario = testCase.getScenarios().get(0);
        for (Step step: scenario.getSteps()) {
            scenario.getModel().addAll(step.getModel());
        }
        logger.debug("Scenario Model:\n{}", TestUtils.toTurtle(scenario.getModel()));
        logger.debug("Scenario Model:\n{}", TestUtils.toTriples(scenario.getModel()));

        final String report = render("scenarios", List.of(scenario));
        logger.debug("Report:\n{}", report);

        final Model reportModel = RDFUtils.parseRdfa(report, "https://example.org");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));
        logger.debug("Report Model:\n{}", TestUtils.toTriples(reportModel));

        assertTrue(Models.isSubset(reportModel, scenario.getModel()));
    }

    @Test
    void testCaseReport() throws IOException {
        final IRI testCaseIri = iri(
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString(),
                "#group1-feature1"
        );
        final TestCase testCase = new TestCase(testCaseIri);
        testCase.getModel().addAll(testCase.getAssertion().getModel());
        testCase.getModel().remove(iri(testCase.getAssertion().getTestSubject()), null, null);
        testCase.getModel().remove(iri(testCase.getAssertion().getAssertedBy()), null, null);
        for (Scenario scenario: testCase.getScenarios()) {
            testCase.getModel().remove(null, null, iri(scenario.getSubject()));
        }
        logger.debug("TestCase Model:\n{}", TestUtils.toTurtle(testCase.getModel()));

        final String report = render("testCases", List.of(testCase));
        logger.debug("Report:\n{}", report);

        final Model reportModel = RDFUtils.parseRdfa(report, "https://example.org");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        assertTrue(Models.isomorphic(reportModel, testCase.getModel()));
    }

    @Test
    void requirementReportNoTestCases() throws IOException {
        final IRI requirementIri = iri("https://example.org/specification1#spec1");
        final SpecificationRequirement requirement = new SpecificationRequirement(requirementIri);
        requirement.getModel().remove(null, null, requirementIri);
        logger.debug("SpecificationRequirement Model:\n{}", TestUtils.toTurtle(requirement.getModel()));

        final String report = render("specificationRequirements", List.of(requirement));
        logger.debug("Report:\n{}", report);

        final Model reportModel = RDFUtils.parseRdfa(report, "https://example.org");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        assertTrue(Models.isomorphic(reportModel, requirement.getModel()));
    }

    @Test
    void requirementReportWithTestCases() throws IOException {
        final IRI requirementIri = iri("https://example.org/specification1#spec1");
        final SpecificationRequirement requirement = new SpecificationRequirement(requirementIri);
        requirement.getModel().remove(null, SPEC.requirement, requirementIri);
        for (TestCase testCase: requirement.getTestCases()) {
            requirement.getModel().addAll(testCase.getModel());
            if (testCase.getAssertion() != null) {
                requirement.getModel().addAll(testCase.getAssertion().getModel());
                requirement.getModel().remove(iri(testCase.getAssertion().getTestSubject()), null, null);
                requirement.getModel().remove(iri(testCase.getAssertion().getAssertedBy()), null, null);
            }
        }
        requirement.getModel().remove(null, DCTERMS.hasPart, null);
        logger.debug("SpecificationRequirement Model:\n{}", TestUtils.toTurtle(requirement.getModel()));

        final String report = render("specificationRequirements", List.of(requirement));
        logger.debug("Report:\n{}", report);

        final Model reportModel = RDFUtils.parseRdfa(report, "https://example.org");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        assertTrue(Models.isomorphic(reportModel, requirement.getModel()));
    }

    @Test
    void specificationHeader() throws IOException {
        final IRI specificationIri = iri("https://example.org/specification1");
        final Specification specification = new Specification(specificationIri);
        logger.debug("Specification Model:\n{}", TestUtils.toTurtle(specification.getModel()));

        final String report = render("specifications", List.of(specification));
        logger.debug("Report:\n{}", report);

        final Model reportModel = RDFUtils.parseRdfa(report, "https://example.org");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        assertTrue(Models.isomorphic(reportModel, specification.getModel()));
    }

    @Test
    void assertionHeader() throws IOException {
        final IRI assertorIri = iri("https://github.com/solid/conformance-test-harness/");
        final Assertor assertor = new Assertor(assertorIri);
        logger.debug("Assertor Model:\n{}", TestUtils.toTurtle(assertor.getModel()));

        final String report = render("assertor", assertor);
        logger.debug("Report:\n{}", report);

        final Model reportModel = RDFUtils.parseRdfa(report, "https://example.org");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        assertTrue(Models.isomorphic(reportModel, assertor.getModel()));
    }

    @Test
    void testSubject() throws IOException {
        final IRI testSubjectIri = iri("https://github.com/solid/conformance-test-harness/testserver");
        final TestSubject testSubject = new TestSubject(testSubjectIri);
        testSubject.getModel().remove(null, SOLID_TEST.features, null);
        testSubject.getModel().remove(null, SOLID_TEST.maxThreads, null);
        testSubject.getModel().remove(null, SOLID_TEST.origin, null);
        logger.debug("TestSubject Model:\n{}", TestUtils.toTurtle(testSubject.getModel()));

        final String report = render("testSubject", testSubject);
        logger.debug("Report:\n{}", report);

        final Model reportModel = RDFUtils.parseRdfa(report, "https://example.org");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        assertTrue(Models.isomorphic(reportModel, testSubject.getModel()));
    }

    @Test
    void testResultsSummary() {
        final Results results = mock(Results.class);
        when(results.getFeaturesPassed()).thenReturn(1);
        when(results.getFeaturesFailed()).thenReturn(2);
        when(results.getFeaturesTotal()).thenReturn(3);
        when(results.getScenariosPassed()).thenReturn(4);
        when(results.getScenariosFailed()).thenReturn(5);
        when(results.getScenariosTotal()).thenReturn(6);
        when(results.getEndTime()).thenReturn(new Date(0).getTime());
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        testSuiteResults.setStartTime(System.currentTimeMillis() - 1000);
        final String report = render("testSuiteResults", testSuiteResults);

        final String reportStripped = StringUtils.normalizeSpace(report);
        assertTrue(reportStripped.contains("datetime=\"Thu Jan 01 01:00:00 GMT 1970\""));
        assertThat(reportStripped, matchesPattern(".*<dd>[0-9]+ ms</dd>.*"));
        assertTrue(reportStripped.contains("<td>1</td> <td>2</td> <td>3</td>"));
        assertTrue(reportStripped.contains("<td>4</td> <td>5</td> <td>6</td>"));
    }

    private String render(final String key, final Object object) {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(), null);
        return testTemplate.data(
                "subject", resultData.getSubject(),
                "prefixes", resultData.getPrefixes(),
                key, object
        ).render();
    }
}
