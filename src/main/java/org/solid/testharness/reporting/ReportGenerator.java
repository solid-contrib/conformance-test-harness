package org.solid.testharness.reporting;

import com.intuit.karate.Results;
import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.core.TagResults;
import com.intuit.karate.core.TimelineResults;
import com.intuit.karate.report.Report;
import com.intuit.karate.report.SuiteReports;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

@ApplicationScoped
public class ReportGenerator implements SuiteReports {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.ReportGenerator");

    @Inject
    DataRepository repository;

    @Override
    public Report featureReport(Suite suite, FeatureResult fr) {
        logger.debug("featureReport");
        repository.addFeatureResult(suite, fr);
        return Report.template("karate-feature.html")
                .reportDir(suite.reportDir)
                .reportFileName(fr.getFeature().getPackageQualifiedName() + ".html")
                .variable("results", fr.toKarateJson())
                .build();
    }

    @Override
    public Report tagsReport(Suite suite, TagResults tr) {
        logger.debug("tagsReport");
        return Report.template("karate-tags.html")
                .reportDir(suite.reportDir)
                .variable("results", tr.toKarateJson())
                .build();
    }

    @Override
    public Report timelineReport(Suite suite, TimelineResults tr) {
        logger.debug("timelineReport");
        return Report.template("karate-timeline.html")
                .reportDir(suite.reportDir)
                .variable("results", tr.toKarateJson())
                .build();
    }

    @Override
    public Report summaryReport(Suite suite, Results results) {
        logger.debug("summaryReport");
        return Report.template("karate-summary.html")
                .reportDir(suite.reportDir)
                .variable("results", results.toKarateJson())
                .build();
    }

}
