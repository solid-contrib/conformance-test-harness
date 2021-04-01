package org.solid.testharness.reporting;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

@ApplicationScoped
public class ResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessor");

    @Inject
    DataRepository repository;

    public void buildTurtleReport(Writer writer) {
        repository.export(writer);
    }

    public void printReportToConsole() {
        StringWriter sw = new StringWriter();
        repository.export(sw);
        logger.info("REPORT\n{}", sw.toString());
    }

    public void buildHtmlReport(Writer writer) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("templates/result-report.mustache");
        try {
            mustache.execute(writer, new ResultData());
            writer.flush();
        } catch (IOException e) {
            logger.error("Failed to generate test suite result HTML report", e);
        }
    }
}
/*
                <!--
                <p>
                    {{subject}} <a href="{{predicate}}">{{predicateLabel}}</a>
                    {{#objectIsIRI}}
                        <a href="{{object}}">{{objectLabel}}</a>
                    {{/objectIsIRI}}
                    {{^objectIsIRI}}
                        {{objectLabel}}
                    {{/objectIsIRI}}
                </p>
                -->
<!--
{{#js}}
    <script src="{{.}}"></script>
{{/js}}
-->
 */
