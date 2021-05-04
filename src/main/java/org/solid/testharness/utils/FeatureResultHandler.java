package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.report.Report;
import com.intuit.karate.report.SuiteReports;
import org.eclipse.rdf4j.model.IRI;
import org.solid.testharness.config.PathMappings;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FeatureResultHandler implements SuiteReports {
    @Inject
    DataRepository dataRepository;
    @Inject
    PathMappings pathMappings;

    @Override
    public Report featureReport(Suite suite, FeatureResult fr) {
        IRI featureIri = pathMappings.unmapFeaturePath(fr.getDisplayName());
        dataRepository.addFeatureResult(suite, fr, featureIri);
        return SuiteReports.super.featureReport(suite, fr);
    }
}
