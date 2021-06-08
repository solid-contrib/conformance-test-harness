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
package org.solid.testharness.utils;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.BNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.solid.testharness.reporting.Step;
import org.solid.testharness.reporting.TestCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataModelBaseTest extends AbstractDataModelTests {
    DataModelBase dataModelBase;

    @BeforeAll
    void getModel() {
        dataModelBase = new DataModelBase(iri(NS, "test"));
    }

    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/datamodelbase-testing-feature.ttl";

    }

    @Test
    void nullSubjectConstruct() {
        assertThrows(NullPointerException.class, () -> new DataModelBase(null));
    }

    @Test
    void getSubjectIri() {
        assertTrue(iri(NS, "test").equals(dataModelBase.getSubjectIri()));
    }

    @Test
    void getSubject() {
        assertEquals(NS + "test", dataModelBase.getSubject());
    }

    @Test
    void sizeShallow() {
        assertEquals(13, dataModelBase.size());
    }

    @Test
    void sizeDeep() {
        final DataModelBase deepModel = new DataModelBase(iri(NS, "test"), DataModelBase.ConstructMode.DEEP);
        assertEquals(17, deepModel.size());
    }

    @Test
    void sizeList() {
        final DataModelBase listModel = new DataModelBase(iri(NS, "test"), DataModelBase.ConstructMode.LIST);
        assertEquals(17, listModel.size());
    }

    @Test
    void getTypesList() {
        assertEquals("earl:Software earl:TestSubject", dataModelBase.getTypesList());
    }

    @Test
    void getIriAsString() {
        assertEquals(NS + "iri", dataModelBase.getIriAsString(iri(NS, "hasIri")));
    }

    @Test
    void getMissingIriAsString() {
        assertNull(dataModelBase.getIriAsString(iri(NS, "hasMissingIri")));
    }

    @Test
    void getModelList() {
        final List<TestCase> models = dataModelBase.getModelList(iri(NS, "hasTest"), TestCase.class);
        assertNotNull(models);
        assertEquals(1, models.size());
        assertEquals("earl:TestCase", models.get(0).getTypesList());
    }

    @Test
    void getEmptyModelList() {
        final List<TestCase> models = dataModelBase.getModelList(iri(NS, "hasTest2"), TestCase.class);
        assertNull(models);
    }

    @Test
    void getBadModelList() {
        assertThrows(RuntimeException.class, () -> dataModelBase.getModelList(iri(NS, "hasTest"), TestClass.class));
    }

    @Test
    void getModelCollectionList() {
        final DataModelBase listModel = new DataModelBase(iri(NS, "test"), DataModelBase.ConstructMode.LIST);
        final List<Step> models = listModel.getModelCollectionList(iri(NS, "hasSteps"), Step.class);
        assertNotNull(models);
        assertEquals(2, models.size());
        assertEquals("earl:Step", models.get(0).getTypesList());
    }

    @Test
    void getEmptyModelCollectionList() {
        final DataModelBase listModel = new DataModelBase(iri(NS, "test"), DataModelBase.ConstructMode.LIST);
        final List<Step> models = listModel.getModelCollectionList(iri(NS, "hasSteps2"), Step.class);
        assertNull(models);
    }

    @Test
    void getBadModelCollectionList() {
        final DataModelBase listModel = new DataModelBase(iri(NS, "test"), DataModelBase.ConstructMode.LIST);
        assertThrows(RuntimeException.class,
                () -> listModel.getModelCollectionList(iri(NS, "hasSteps"), TestClass.class)
        );
    }

    @Test
    void getAsIri() {
        assertTrue(iri(NS, "iri").equals(dataModelBase.getAsIri(iri(NS, "hasIri"))));
    }

    @Test
    void getMissingAsIri() {
        assertNull(dataModelBase.getAsIri(iri(NS, "hasMissingIri")));
    }

    @Test
    void getStringAsIri() {
        assertNull(dataModelBase.getAsIri(iri(NS, "hasString")));
    }

    @Test
    void getLiteralAsString() {
        assertEquals("string", dataModelBase.getLiteralAsString(iri(NS, "hasString")));
    }

    @Test
    void getMissingLiteralAsString() {
        assertEquals(null, dataModelBase.getLiteralAsString(iri(NS, "hasMissingString")));
    }

    @Test
    void getAsBNode() {
        assertNotNull(dataModelBase.getAsBNode(iri(NS, "hasBNode")));
    }

    @Test
    void getMissingAsBNode() {
        assertNull(dataModelBase.getAsBNode(iri(NS, "hasMissingBNode")));
    }

    @Test
    void getIriAsBNode() {
        assertNull(dataModelBase.getAsBNode(iri(NS, "hasString")));
    }

    @Test
    void getLiteralAsStringFromBNode() {
        final DataModelBase deepModel = new DataModelBase(iri(NS, "test"), DataModelBase.ConstructMode.DEEP);
        final BNode node = deepModel.getAsBNode(iri(NS, "hasBNode"));
        assertNotNull(node);
        assertEquals("string", deepModel.getLiteralAsString(node, iri(NS, "hasString")));
    }

    @Test
    void getLiteralsAsStringSet() {
        final Set<String> set = dataModelBase.getLiteralsAsStringSet(iri(NS, "hasStrings"));
        assertNotNull(set);
        assertEquals(2, set.size());
        assertTrue(set.contains("string1") && set.contains("string2"));
    }

    @Test
    void getMissingLiteralsAsStringSet() {
        final Set<String> set = dataModelBase.getLiteralsAsStringSet(iri(NS, "hasMissingStrings"));
        assertNotNull(set);
        assertEquals(0, set.size());
    }

    @Test
    void getLiteralAsInt() {
        assertEquals(1, dataModelBase.getLiteralAsInt(iri(NS, "hasInt")));
    }

    @Test
    void getMissingLiteralAsInt() {
        assertEquals(0, dataModelBase.getLiteralAsInt(iri(NS, "hasMissingInt")));
    }

    @Test
    void getLiteralAsBoolean() {
        assertEquals(true, dataModelBase.getLiteralAsBoolean(iri(NS, "hasBool")));
    }

    @Test
    void getMissingLiteralAsBoolean() {
        assertEquals(false, dataModelBase.getLiteralAsBoolean(iri(NS, "hasMissingBool")));
    }

    @Test
    void getLiteralAsDate() {
        final LocalDate date = dataModelBase.getLiteralAsDate(iri(NS, "hasDate"));
        assertThat("Date matches", date.isEqual(LocalDate.parse("2021-04-08")));
    }

    @Test
    void getMissingLiteralAsDate() {
        assertNull(dataModelBase.getLiteralAsDate(iri(NS, "hasMissingDate")));
    }

    @Test
    void getLiteralAsDateTime() {
        final LocalDateTime dateTime = dataModelBase.getLiteralAsDateTime(iri(NS, "hasDateTime"));
        assertThat("DateTime matches", dateTime.isEqual(LocalDateTime.parse("2021-04-08T12:30:00.000")));
    }
    @Test
    void getMissingLiteralAsDateTime() {
        assertNull(dataModelBase.getLiteralAsDateTime(iri(NS, "hasMissingDateTime")));
    }

    class TestClass extends DataModelBase {
        public TestClass() {
            super(null);
        }
    }
}
