plugins {
    java
}

group = "org.solid.testharness"
version = "0.0.1-SNAPSHOT"

val junitJupiterVersion = "5.7.0"
val karateVersion = "0.9.9.RC4"
val cucumberReporting = "5.4.0"
val rdf4jVersion = "3.5.0"
val jakartaVersion = "3.0.0"
val jose4jVersion = "0.7.6"
val commonsTextVersion = "1.9"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

repositories {
    // Use JCenter for resolving dependencies. OR mavenCentral()
    // jcenter()
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    implementation("com.intuit.karate:karate-core:$karateVersion")
    implementation("net.masterthought:cucumber-reporting:$cucumberReporting")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:$jakartaVersion")
    implementation("org.glassfish.jersey.core:jersey-client:$jakartaVersion")
    implementation("org.bitbucket.b_c:jose4j:$jose4jVersion")
    implementation("org.apache.commons:commons-text:$commonsTextVersion")

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
}

tasks.test {
    useJUnitPlatform()
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
    systemProperty("karate.options", System.getProperty("karate.options"))
    systemProperty("karate.env", System.getProperty("karate.env"))
    if (System.getProperty("credentials") != null) {
        systemProperty("credentials", file(System.getProperty("credentials")).absolutePath)
    }
    outputs.upToDateWhen { false }
}

sourceSets {
    test {
        resources {
            srcDir("src/main/resources")
        }
        resources {
            srcDir("examples")
        }
        resources {
            srcDir("src/test/java")
            exclude("**/*.java")
        }
        resources {
            srcDir("src/test/resources")
//            exclude("**/*.*")
        }
    }
}


//tasks.register<Jar>("uberJar") {
//    archiveClassifier.set("uber")
//
//    from(sourceSets.main.get().output)
//
//    dependsOn(configurations.runtimeClasspath)
//    from({
//        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
//    })
//}

