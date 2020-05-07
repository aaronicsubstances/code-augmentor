package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.aaronicsubstances.code.augmentor.core.TestArg;
import com.google.common.collect.Sets;

import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMap;
import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMapEntry;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GraphAlgorithmsTest {

    @Test(dataProvider = "createTestDijkstraShortestPathAlgorithmData")
    public void testDijkstraShortestPathAlgorithm(
            TestArg<List<Map<Integer, Set<Integer>>>> graphWrapper,
            BiFunction<Integer, Integer, Double> weightFunction,
            int startVertex, Integer stopVertex,
            TestArg<Map<Integer, Map<String, Object>>> expectedWrapper) {
        List<Map<Integer, Set<Integer>>> graph = graphWrapper.value;
        Map<Integer, Map<String, Object>> expected = expectedWrapper.value;
        Map<Integer, Map<String, Object>> actual = GraphAlgorithms.dijkstraShortestPathAlgorithm(graph, 
            weightFunction, startVertex, stopVertex);
        assertThat(actual, is(expected));
    }

    @DataProvider
    public Object[][] createTestDijkstraShortestPathAlgorithmData() {
        // example 1.
        Map<Integer, Set<Integer>> graph1 = new HashMap<>();
        TestArg<List<Map<Integer, Set<Integer>>>> graphWrapper1 = new TestArg<>(
            Arrays.asList(graph1));
        graph1.put(1, Sets.newHashSet(2, 4));
        graph1.put(2, Sets.newHashSet(1, 3, 5));
        graph1.put(3, Sets.newHashSet(2, 26));
        graph1.put(4, Sets.newHashSet(1, 5));
        graph1.put(5, Sets.newHashSet(2, 4, 26));
        graph1.put(26, Sets.newHashSet(3, 5));
        Map<Set<Integer>, Double> undirectedEdges = new HashMap<>();
        undirectedEdges.put(Sets.newHashSet(1, 2), 4.0); 
        undirectedEdges.put(Sets.newHashSet(1, 4), 2.0);
        undirectedEdges.put(Sets.newHashSet(2, 3), 3.0);
        undirectedEdges.put(Sets.newHashSet(2, 5), 3.0); 
        undirectedEdges.put(Sets.newHashSet(3, 26), 2.0);
        undirectedEdges.put(Sets.newHashSet(4, 5), 3.0);
        undirectedEdges.put(Sets.newHashSet(5, 26), 1.0);
        BiFunction<Integer, Integer, Double> weightFunction1 = (u, v) -> {
            return undirectedEdges.get(Sets.newHashSet(u, v));
        };
        Map<Integer, Map<String, Object>> expected1a = new HashMap<>();
        expected1a.put(1, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 0.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, null))));
        expected1a.put(2, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 4.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 1))));
        expected1a.put(3, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 7.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 2))));
        expected1a.put(4, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 2.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 1))));
        expected1a.put(5, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 5.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 4))));
        expected1a.put(26, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 6.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 5))));

        Map<Integer, Map<String, Object>> expected1b = new HashMap<>();
        expected1b.put(1, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 0.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, null))));
        expected1b.put(2, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 4.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 1))));
        expected1b.put(3, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 7.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 2))));
        expected1b.put(4, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 2.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 1))));
        expected1b.put(5, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 5.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 4))));
        expected1b.put(26, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, null),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, null))));

        Map<Integer, Map<String, Object>> expected1c = new HashMap<>();
        expected1c.put(2, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 0.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, null))));
        expected1c.put(5, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 3.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 2))));
        expected1c.put(1, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 4.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 2))));
        expected1c.put(4, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 6.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 5))));
        expected1c.put(3, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 3.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 2))));
        expected1c.put(26, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 4.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 5))));
            
        // example 2.
        Map<Integer, Set<Integer>> graph2 = new HashMap<>();
        TestArg<List<Map<Integer, Set<Integer>>>> graphWrapper2 = new TestArg<>(
            Arrays.asList(graph2));
        graph2.put(1, Sets.newHashSet(2, 3));
        graph2.put(2, Sets.newHashSet(1, 3, 4));
        graph2.put(3, Sets.newHashSet(1, 2, 4, 5));
        graph2.put(4, Sets.newHashSet(2, 3, 5, 26));
        graph2.put(5, Sets.newHashSet(3, 4, 26));
        graph2.put(26, Sets.newHashSet(4, 5));
        Map<Set<Integer>, Double> undirectedEdges2 = new HashMap<>();
        undirectedEdges2.put(Sets.newHashSet(1, 2), 4.0); 
        undirectedEdges2.put(Sets.newHashSet(1, 3), 2.0);
        undirectedEdges2.put(Sets.newHashSet(2, 3), 1.0);
        undirectedEdges2.put(Sets.newHashSet(2, 4), 5.0); 
        undirectedEdges2.put(Sets.newHashSet(3, 4), 8.0);
        undirectedEdges2.put(Sets.newHashSet(3, 5), 10.0);
        undirectedEdges2.put(Sets.newHashSet(4, 5), 2.0);
        undirectedEdges2.put(Sets.newHashSet(4, 26), 6.0);
        undirectedEdges2.put(Sets.newHashSet(5, 26), 3.0);
        BiFunction<Integer, Integer, Double> weightFunction2 = (u, v) -> {
            return undirectedEdges2.get(Sets.newHashSet(u, v));
        };
        Map<Integer, Map<String, Object>> expected2a = new HashMap<>();
        expected2a.put(1, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 0.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, null))));
        expected2a.put(2, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 3.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 3))));
        expected2a.put(3, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 2.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 1))));
        expected2a.put(4, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 8.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 2))));
        expected2a.put(5, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 10.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 4))));
        expected2a.put(26, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 13.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 5))));
            
        // example 3 - truly directed graph.
        Map<Integer, Set<Integer>> graph3 = new HashMap<>();
        TestArg<List<Map<Integer, Set<Integer>>>> graphWrapper3 = new TestArg<>(
            Arrays.asList(graph3));
        graph3.put(0, Sets.newHashSet(1, 3));
        graph3.put(1, Sets.newHashSet(2, 3));
        graph3.put(2, Sets.newHashSet(4));
        graph3.put(3, Sets.newHashSet(1, 2, 4));
        graph3.put(4, Sets.newHashSet(0, 2));
        BiFunction<Integer, Integer, Double> weightFunction3 = (u, v) -> {
            if (u == 0) {
                switch (v) {
                    case 1: return 10.0;
                    case 3: return 5.0;
                }
            }
            if (u == 1) {
                switch (v) {
                    case 2: return 1.0;
                    case 3: return 2.0;
                }
            }
            if (u == 2) {
                if (v == 4) return 4.0;
            }
            if (u == 3) {
                switch (v) {
                    case 1: return 3.0;
                    case 2: return 9.0;
                    case 4: return 2.0;
                }
            }
            if (u == 4) {
                switch (v) {
                    case 0: return 7.0;
                    case 2: return 6.0;
                }
            }
            return null;
        };
        Map<Integer, Map<String, Object>> expected3 = new HashMap<>();
        expected3.put(0, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 0.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, null))));
        expected3.put(1, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 8.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 3))));
        expected3.put(2, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 9.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 1))));
        expected3.put(3, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 5.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 0))));
        expected3.put(4, newMap(Arrays.asList(
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_DIST, 7.0),
            newMapEntry(GraphAlgorithms.VERTEX_ATTRIBUTE_PRED, 3))));

        return new Object[][]{
            { graphWrapper1, weightFunction1, 1, null, new TestArg<>(expected1a) },
            { graphWrapper1, weightFunction1, 1, 5, new TestArg<>(expected1b) },
            { graphWrapper1, weightFunction1, 2, null, new TestArg<>(expected1c) },
            { graphWrapper2, weightFunction2, 1, null, new TestArg<>(expected2a) },
            { graphWrapper3, weightFunction3, 0, null, new TestArg<>(expected3) }
        };
    }
}