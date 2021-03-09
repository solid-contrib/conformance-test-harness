plugins {
    java
//    jacoco
}

//jacoco {
//    toolVersion = "0.8.6"
//}

group = "org.solid.testharness"
version = "0.0.1-SNAPSHOT"

val junitJupiterVersion = "5.7.0"
val karateVersion = "0.9.9.RC4"
val cucumberReporting = "5.4.0"
val rdf4jVersion = "3.6.0"
val jakartaVersion = "3.0.0"
val jose4jVersion = "0.7.6"
val commonsTextVersion = "1.9"
val commonsLangVersion = "3.11"
val mockitoVersion = "3.+"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

repositories {
    // Use JCenter for resolving dependencies. OR mavenCentral()
//    jcenter()
    mavenCentral()
}

sourceSets {
    test {
        java {
            exclude("TestSuiteRunner.java")
        }
        resources {
            srcDir("src/main/resources")
        }
        resources {
            srcDir("examples")
        }
    }

    create("testsuite") {
        java {
            srcDir("src/main/java")
        }
        java {
            srcDir("src/test/java")
        }
        resources {
            srcDir("src/main/resources")
        }
        resources {
            srcDir("examples")
        }
    }
}

val testsuiteImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

configurations["testsuiteRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    implementation("com.intuit.karate:karate-core:$karateVersion")
    implementation("net.masterthought:cucumber-reporting:$cucumberReporting")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:$jakartaVersion")
    implementation("org.glassfish.jersey.core:jersey-client:$jakartaVersion")
    implementation("org.bitbucket.b_c:jose4j:$jose4jVersion")
    implementation("org.apache.commons:commons-text:$commonsTextVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")

    implementation("org.eclipse.rdf4j:rdf4j-repository-sail:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-sail-memory:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-rdfjson:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-n3:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-nquads:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-ntriples:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-rdfjson:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-rdfxml:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-trig:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-trix:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-turtle:$rdf4jVersion")
    implementation("org.eclipse.rdf4j:rdf4j-rio-jsonld:$rdf4jVersion")
    implementation(fileTree("src/main/libs") { include("*.jar") })
}

tasks.test {
    // work out the absolute path to the config file or use the one in the project by default
    if (System.getProperty("config") != null) {
        systemProperty("config", file(System.getProperty("config")).absolutePath)
    } else {
        systemProperty("config", project.file("config/config.json").absolutePath)
    }
    if (System.getProperty("features") != null) {
        systemProperty("features", file(System.getProperty("features")).absolutePath)
    } else {
        systemProperty("features", project.file("examples").absolutePath)
    }
    if (System.getProperty("credentials") != null) {
        systemProperty("credentials", file(System.getProperty("credentials")).absolutePath)
    }

    systemProperty("agent", System.getProperty("agent"))
    systemProperty("karate.options", System.getProperty("karate.options"))
    systemProperty("karate.env", System.getProperty("karate.env"))

    useJUnitPlatform {
        // only run the unit tests
        excludeTags("solid")
    }
    outputs.upToDateWhen { false }
}

val testsuite = task<Test>("testsuite") {
    description = "Runs test suite."
    group = "verification"
    testClassesDirs = sourceSets["testsuite"].output.classesDirs
    classpath = sourceSets["testsuite"].runtimeClasspath
    dependsOn("testClasses")

    if (System.getProperty("config") != null) {
        systemProperty("config", file(System.getProperty("config")).absolutePath)
    } else {
        systemProperty("config", project.file("config/config.json").absolutePath)
    }
    if (System.getProperty("features") != null) {
        systemProperty("features", file(System.getProperty("features")).absolutePath)
    } else {
        systemProperty("features", project.file("examples").absolutePath)
    }
    if (System.getProperty("credentials") != null) {
        systemProperty("credentials", file(System.getProperty("credentials")).absolutePath)
    }

    systemProperty("agent", System.getProperty("agent"))
    systemProperty("karate.options", System.getProperty("karate.options"))
    systemProperty("karate.env", System.getProperty("karate.env"))
    useJUnitPlatform{
        // only run the test suite
        includeTags("solid")
    }
    outputs.upToDateWhen { false }
}
