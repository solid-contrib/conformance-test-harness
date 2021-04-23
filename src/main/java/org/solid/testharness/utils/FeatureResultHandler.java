package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.report.Report;
import com.intuit.karate.report.SuiteReports;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FeatureResultHandler implements SuiteReports {
    @Inject
    DataRepository repository;

    @Override
    public Report featureReport(Suite suite, FeatureResult fr) {
        repository.addFeatureResult(suite, fr);
        return SuiteReports.super.featureReport(suite, fr);
    }
}
