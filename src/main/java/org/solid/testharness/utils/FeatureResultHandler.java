package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.report.Report;
import com.intuit.karate.report.SuiteReports;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.PathMappings;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FeatureResultHandler implements SuiteReports {
    private static final Logger logger = LoggerFactory.getLogger(FeatureResultHandler.class);

    @Inject
    DataRepository dataRepository;
    @Inject
    PathMappings pathMappings;

    @Override
    public Report featureReport(final Suite suite, final FeatureResult fr) {
        final String featurePath = fr.getDisplayName();
        final IRI featureIri = pathMappings.unmapFeaturePath(featurePath);
        if (featureIri != null) {
            dataRepository.addFeatureResult(suite, fr, featureIri);
        } else {
            logger.warn("The feature {} could not be mapped back to an IRI", featurePath);
        }
        return SuiteReports.super.featureReport(suite, fr);
    }
}
