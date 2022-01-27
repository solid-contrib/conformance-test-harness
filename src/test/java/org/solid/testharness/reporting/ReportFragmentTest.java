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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.*;
import org.solid.testharness.utils.DataRepository;
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
class ReportFragmentTest {
    private static final Logger logger = LoggerFactory.getLogger(ReportFragmentTest.class);
    private static final String BASE_URI = "https://example.org/";

    @Inject
    DataRepository dataRepository;

    @Inject
    Engine engine;

    private Template testTemplate;

    void addLocator(final @Observes EngineBuilder engineBuilder) {
        engineBuilder.addLocator(new TestTemplateLocator());
    }

    @BeforeEach
    void setup() throws MalformedURLException {
        testTemplate = engine.getTemplate("src/test/resources/reporting/test.html");
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.clear();
        }
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/harness-sample.ttl"), BASE_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl"), BASE_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/tests-sample.ttl"), BASE_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/reporting/testsuite-results-sample.ttl"),
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString());
    }

    @Test
    void stepReport() throws IOException {
        remove(iri("https://github.com/solid/specification-tests/uuid#node1f273av6vx13"), null, null);
        final Step step = new Step(iri("https://github.com/solid/specification-tests/uuid#node1f273av6vx18"));

        final String report = render("steps", List.of(step));
        logger.debug("Report:\n{}", report);

        logger.debug("Step Model:\n{}", TestUtils.toTurtle(step.getModel()));

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        // remove list from model to just compare the step
        reportModel.remove(null, DCTERMS.hasPart, null);
        reportModel.remove(null, RDF.first, null);
        reportModel.remove(null, RDF.rest, null);
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        TestUtils.showModelDifferences(reportModel, step.getModel(), logger);
        assertTrue(Models.isomorphic(reportModel, step.getModel()));
    }

    @Test
    void scenarioReport() throws IOException {
        final Scenario scenario = new Scenario(
                iri("https://github.com/solid/specification-tests/uuid#node1f273av6vx13"));
        for (Step step: scenario.getSteps()) {
            scenario.getModel().addAll(step.getModel());
        }

        final String report = render("scenarios", List.of(scenario));
        logger.debug("Report:\n{}", report);

        logger.debug("Scenario Model:\n{}", TestUtils.toTurtle(scenario.getModel()));
        logger.debug("Scenario Model:\n{}", TestUtils.toTriples(scenario.getModel()));

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        // remove section hasPart to compare
        reportModel.remove(iri(BASE_URI), DCTERMS.hasPart, null);

        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));
        logger.debug("Report Model:\n{}", TestUtils.toTriples(reportModel));

        TestUtils.showModelDifferences(reportModel, scenario.getModel(), logger);
        assertTrue(Models.isomorphic(scenario.getModel(), reportModel));
    }

    @Test
    void scenarioReportWithBackground() throws IOException {
        final Scenario scenario = new Scenario(
                iri("https://github.com/solid/specification-tests/uuid#node1f273av6vx42"));
        for (Step step: scenario.getBackgroundSteps()) {
            scenario.getModel().addAll(step.getModel());
        }
        for (Step step: scenario.getSteps()) {
            scenario.getModel().addAll(step.getModel());
        }

        final String report = render("scenarios", List.of(scenario));
        logger.debug("Report:\n{}", report);

        logger.debug("Scenario Model:\n{}", TestUtils.toTurtle(scenario.getModel()));
        logger.debug("Scenario Model:\n{}", TestUtils.toTriples(scenario.getModel()));

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        // remove section hasPart to compare
        reportModel.remove(iri(BASE_URI), DCTERMS.hasPart, null);

        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));
        logger.debug("Report Model:\n{}", TestUtils.toTriples(reportModel));

        TestUtils.showModelDifferences(reportModel, scenario.getModel(), logger);
        assertTrue(Models.isomorphic(scenario.getModel(), reportModel));
    }

    @Test
    void testCaseReport() throws IOException {
        final IRI testCaseIri = iri(
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString(),
                "#group1-feature1"
        );
        remove(iri("https://github.com/solid/conformance-test-harness/testserver"), null, null);
        remove(null, null, iri("https://github.com/solid/specification-tests/uuid#node1f273av6vx13"));
        remove(null, null, iri("https://github.com/solid/specification-tests/uuid#node1f273av6vx42"));
        final TestCase testCase = new TestCase(testCaseIri);
        testCase.getModel().addAll(testCase.getAssertion().getModel());

        final String report = render("testCases", List.of(testCase));
        logger.debug("Report:\n{}", report);

        logger.debug("TestCase Model:\n{}", TestUtils.toTurtle(testCase.getModel()));

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        TestUtils.showModelDifferences(reportModel, testCase.getModel(), logger);
        assertTrue(Models.isomorphic(reportModel, testCase.getModel()));
    }

    @Test
    void requirementReportNoTestCases() throws IOException {
        final IRI requirementIri = iri(BASE_URI, "specification1#spec1");
        remove(null, null, requirementIri);
        final SpecificationRequirement requirement = new SpecificationRequirement(requirementIri);

        final String report = render("specificationRequirements", List.of(requirement), "coverageMode", false);
        logger.debug("Report:\n{}", report);

        logger.debug("SpecificationRequirement Model:\n{}", TestUtils.toTurtle(requirement.getModel()));

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        // remove section hasPart to compare
        reportModel.remove(iri(BASE_URI), DCTERMS.hasPart, null);
        requirement.getModel().remove(requirementIri, null, null);
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        TestUtils.showModelDifferences(reportModel, requirement.getModel(), logger);
        assertTrue(Models.isomorphic(reportModel, requirement.getModel()));
    }

    @Test
    void requirementReportWithTestCases() throws IOException {
        final IRI requirementIri = iri(BASE_URI, "specification1#spec1");
        remove(null, SPEC.requirement, requirementIri);
        remove(iri("https://github.com/solid/conformance-test-harness/testserver"), null, null);
        final SpecificationRequirement requirement = new SpecificationRequirement(requirementIri);
        for (TestCase testCase: requirement.getTestCases()) {
            requirement.getModel().addAll(testCase.getModel());
            if (testCase.getAssertion() != null) {
                requirement.getModel().addAll(testCase.getAssertion().getModel());
            }
        }
        requirement.getModel().remove(null, DCTERMS.hasPart, null);
        requirement.getModel().remove(null, DCTERMS.description, null);

        final String report = render("specificationRequirements", List.of(requirement), "coverageMode", false);
        logger.debug("Report:\n{}", report);

        logger.debug("SpecificationRequirement Model:\n{}", TestUtils.toTurtle(requirement.getModel()));

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        // remove section hasPart to compare
        reportModel.remove(iri(BASE_URI), DCTERMS.hasPart, null);
        requirement.getModel().remove(requirementIri, null, null);
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        TestUtils.showModelDifferences(reportModel, requirement.getModel(), logger);
        assertTrue(Models.isomorphic(reportModel, requirement.getModel()));
    }

    @Test
    void specificationHeader() throws IOException {
        final IRI specificationIri = iri(BASE_URI, "specification1");
        final Specification specification = new Specification(specificationIri);
        logger.debug("Specification Model:\n{}", TestUtils.toTurtle(specification.getModel()));

        final String report = render("specifications", List.of(specification));
        logger.debug("Report:\n{}", report);

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        assertTrue(reportModel.isEmpty());
        assertTrue(report.contains("specification1"));
    }

    @Test
    void assertorHeader() throws IOException {
        final IRI assertorIri = iri(BASE_URI);
        final Assertor assertor = new Assertor(assertorIri);
        logger.debug("Assertor Model:\n{}", TestUtils.toTurtle(assertor.getModel()));

        final String report = render("assertor", assertor);
        logger.debug("Report:\n{}", report);

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        TestUtils.showModelDifferences(reportModel, assertor.getModel(), logger);
        assertTrue(Models.isomorphic(reportModel, assertor.getModel()));
    }

    @Test
    void specificationTestsHeader() throws IOException {
        final IRI specificationTestsIri = iri(BASE_URI + "#tests");
        final SpecificationTests specificationTests = new SpecificationTests(specificationTestsIri);
        logger.debug("SpecificationTests Model:\n{}", TestUtils.toTurtle(specificationTests.getModel()));

        final String report = render("specificationTests", specificationTests);
        logger.debug("Report:\n{}", report);

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI + "#tests");
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        TestUtils.showModelDifferences(reportModel, specificationTests.getModel(), logger);
        assertTrue(Models.isomorphic(reportModel, specificationTests.getModel()));
    }

    @Test
    void testSubject() throws IOException {
        final IRI testSubjectIri = iri(BASE_URI, "testserver");
        final TestSubject testSubject = new TestSubject(testSubjectIri);
        testSubject.getModel().remove(null, SOLID_TEST.features, null);
        testSubject.getModel().remove(null, SOLID_TEST.skip, null);
        logger.debug("TestSubject Model:\n{}", TestUtils.toTurtle(testSubject.getModel()));

        final String report = render("testSubject", testSubject);
        logger.debug("Report:\n{}", report);

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        TestUtils.showModelDifferences(reportModel, testSubject.getModel(), logger);
        assertTrue(Models.isomorphic(reportModel, testSubject.getModel()));
    }

    @Test
    void testResultsSummary() {
        final Results results = mock(Results.class);
        when(results.getScenariosPassed()).thenReturn(4);
        when(results.getScenariosFailed()).thenReturn(5);
        when(results.getScenariosTotal()).thenReturn(6);
        when(results.getEndTime()).thenReturn(new Date(0).getTime());
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        testSuiteResults.setStartTime(System.currentTimeMillis() - 1000);
        testSuiteResults.summarizeOutcomes(dataRepository);
        final String report = render("testSuiteResults", testSuiteResults);

        final String reportStripped = StringUtils.normalizeSpace(report);
        assertThat(reportStripped, matchesPattern(".*datetime=\"Thu Jan 01 \\d\\d:00:00 \\w\\w\\w 1970\".*"));
        assertThat(reportStripped, matchesPattern(".*<dd>[0-9]+ ms</dd>.*"));
        assertTrue(reportStripped.contains("<td>1</td> <td>0</td> <td>0</td> <td>0</td> <td>1</td>"));
        assertTrue(reportStripped.contains("<td>4</td> <td>5</td> <td>N/A</td> <td>N/A</td> <td>6</td>"));
    }

    @Test
    void requirementCoverageReportWithTestCases() throws IOException {
        final IRI requirementIri = iri(BASE_URI, "specification1#spec1");
        remove(null, SPEC.requirement, requirementIri);
        remove(iri("https://github.com/solid/conformance-test-harness/testserver"), null, null);
        final String manifest = TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl") + "#";
        remove(null, null, iri(manifest, "group1-feature1"));
        remove(null, null, iri(manifest, "group1-feature2"));
        remove(null, null, iri(manifest, "group1-feature3"));
        remove(iri(manifest, "group1-feature1"), DCTERMS.description, null);

        final SpecificationRequirement requirement = new SpecificationRequirement(requirementIri);
        for (TestCase testCase: requirement.getTestCases()) {
            requirement.getModel().addAll(testCase.getModel());
        }
        requirement.getModel().remove(null, DCTERMS.hasPart, null);
        logger.debug("SpecificationRequirement Model:\n{}", TestUtils.toTurtle(requirement.getModel()));

        final String report = render("specificationRequirements", List.of(requirement), "coverageMode", true);
        logger.debug("Report:\n{}", report);

        final Model reportModel = TestUtils.parseRdfa(report, BASE_URI);
        // remove section hasPart to compare
        reportModel.remove(iri(BASE_URI), DCTERMS.hasPart, null);
        requirement.getModel().remove(requirementIri, null, null);
        logger.debug("Report Model:\n{}", TestUtils.toTurtle(reportModel));

        TestUtils.showModelDifferences(reportModel, requirement.getModel(), logger);
        assertTrue(Models.isomorphic(reportModel, requirement.getModel()));
    }

    private void remove(final Resource subject, final IRI predicate, final Value object) {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.remove(subject, predicate, object);
        }
    }

    private String render(final String key, final Object object) {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(), null);
        return testTemplate.data(
                "subject", resultData.getSubject(),
                "prefixes", resultData.getPrefixes(),
                key, object
        ).render();
    }

    private String render(final String key1, final Object object1, final String key2, final Object object2) {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(), null);
        return testTemplate.data(
                "subject", resultData.getSubject(),
                "prefixes", resultData.getPrefixes(),
                key1, object1,
                key2, object2
        ).render();
    }
}
