package org.solid.testharness.config;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.SOLID_TEST;
import org.solid.testharness.utils.DataModelBase;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.validation.constraints.Positive;
import java.util.Map;
import java.util.stream.Collectors;

public class TargetServer extends DataModelBase {
    private static final Logger logger = LoggerFactory.getLogger(TargetServer.class);

    private Map<String, Boolean> features;
    @Positive(message = "maxThreads must be >= 1")
    private Integer maxThreads;
    private String origin;
    private Boolean setupRootAcl;
    private Boolean disableDPoP;

    public TargetServer(final IRI subject) {
        super(subject, ConstructMode.DEEP);
        logger.debug("Retrieved {} statements for {}", super.size(), subject);
        features = getLiteralsAsStringSet(SOLID_TEST.features).stream()
                .collect(Collectors.toMap(Object::toString, f -> Boolean.TRUE));
        maxThreads = getLiteralAsInt(SOLID_TEST.maxThreads);
        if (maxThreads <= 0) {
            throw new TestHarnessInitializationException("maxThreads must be >= 1");
        }
        origin = getIriAsString(SOLID_TEST.origin);
        setupRootAcl = getLiteralAsBoolean(SOLID_TEST.setupRootAcl);
        // TODO: Add disableDPoP to vocab
        disableDPoP = false; //getLiteralAsBoolean(SOLID_TEST.disableDPoP);
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public String getOrigin() {
        return origin;
    }

    public boolean isSetupRootAcl() {
        return setupRootAcl;
    }

    public boolean isDisableDPoP() {
        return disableDPoP;
    }
}
