package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Contains collection of computer science graph algorithms.
 */
public class GraphAlgorithms {
    /**
     * Key for getting the distance metric of a graph vertex from some source vertex.
     */
    public static final String VERTEX_ATTRIBUTE_DIST = "dist";

    /**
     * Key for getting predecessor metric for a graph vertex.
     */
    public static final String VERTEX_ATTRIBUTE_PRED = "pred";

    /**
     * Implements dijkstra shortest path algorithm.
     * <p>
     * Implements algorithm as specified in Introduction to Algorithms, Section 24.3, by
     * Cormen, Leiserson, Rivest and Stein, 3rd edition.
     * 
     * @param graph list of adjacency-list representations of directed graphs.
     * @param weightFunction a function that computes the shortest nonnegative distance
     *        for an ordered pair of vertices. Can return null for infinity.
     * @param startVertex source vertex of algorithm.
     * @param stopVertex optional vertex to stop algorithm when shortest distance from
     * startVertex to stopVertex is determined.
     * @return map containing for each graph vertex the shortest path (or null for infinity),
     * and the predecessor vertex on the shortest path to the source vertex.
     */
    public static Map<Integer, Map<String, Object>> dijkstraShortestPathAlgorithm(
            List<Map<Integer, Set<Integer>>> graph,
            BiFunction<Integer, Integer, Double> weightFunction,
            int startVertex, Integer stopVertex) {

        // First implement INITIALIZE-SINGLE-SOURCE
        // for each vertex v belonging to G.V
        //   v.d = Infinity
        //   v.predecessorVertex = null
        // s.d = 0
        Map<Integer, Map<String, Object>> vertexAttributeMaps = new HashMap<>();
        for (Map<Integer, Set<Integer>> graphSection : graph) {
            for (Map.Entry<Integer, Set<Integer>> graphSectionEntry : graphSection.entrySet()) {
                if (!vertexAttributeMaps.containsKey(graphSectionEntry.getKey())) {
                    vertexAttributeMaps.put(graphSectionEntry.getKey(), createVertAttMap());
                }
                for (int adjacentVertex : graphSectionEntry.getValue()) {
                    if (!vertexAttributeMaps.containsKey(adjacentVertex)) {
                        vertexAttributeMaps.put(adjacentVertex, createVertAttMap());
                    }
                }
            }
        }
        if (vertexAttributeMaps.containsKey(startVertex)) {
            vertexAttributeMaps.get(startVertex).put(VERTEX_ATTRIBUTE_DIST, 0.0);
        }
        else {
            // not expected, but ensure prescence of map for start vertex
            // even if it will have no results.
            vertexAttributeMaps.put(startVertex, createVertAttMap());
        }
        if (stopVertex != null && !vertexAttributeMaps.containsKey(stopVertex)) {
            vertexAttributeMaps.put(stopVertex, createVertAttMap());
        }

        // Next, implement main body of algorithm.
        // S = {}
        // Q = G.V
        // while Q != {}
        //   u = EXTRACT-MIN(Q)
        //   S = S U {u}
        //   for each vertex v belonging to G.Adj[u]
        //       RELAX(u, v, w)

        Comparator<Integer> vertexComparator = (u, v) -> {
            // treat null distances as infinity
            Double uDistance = (Double) vertexAttributeMaps.get(u).get(VERTEX_ATTRIBUTE_DIST);
            Double vDistance = (Double) vertexAttributeMaps.get(v).get(VERTEX_ATTRIBUTE_DIST);
            if (uDistance == null) {
                return vDistance != null ? 1 : 0;
            }
            if (vDistance == null) {
                return -1;
            }
            return Double.compare(uDistance, vDistance);
        };
        PriorityQueue<Integer> queue = new PriorityQueue<>(vertexComparator);
        queue.addAll(vertexAttributeMaps.keySet());
        while (!queue.isEmpty()) {
            int u = queue.remove();

            // stop search if stop vertex attributes have been determined.
            if (stopVertex != null && u == stopVertex) {
                break;
            }

            Double uDistance = (Double) vertexAttributeMaps.get(u).get(VERTEX_ATTRIBUTE_DIST);
            if (uDistance == null) {
                // then no possible minimum can be discovered again.
                break;
            }

            // Implement RELAX for all adjacent vertices of u
            for (Map<Integer, Set<Integer>> graphSection : graph) {
                Set<Integer> adjacentVertices = graphSection.get(u);
                if (adjacentVertices == null) {
                    continue;
                }
                // if v.d > u.d + w(u, v)
                //   v.d = u.d + w(u, v)
                //   v.predecessorVertex = u
                for (int v : adjacentVertices) {
                    Double weight = weightFunction.apply(u, v);
                    if (weight == null)  {
                        continue;
                    }
                    if (weight < 0.0) {
                        throw new RuntimeException("Negative weights are not allowed. " +
                            String.format("Received w(%s, %s) = %s", u, v, weight));
                    }
                    Map<String, Object> vAtts = vertexAttributeMaps.get(v);
                    Double vDistance = (Double) vAtts.get(VERTEX_ATTRIBUTE_DIST);
                    if (vDistance == null || vDistance > uDistance + weight) {
                        // remove and re-add to restore queue min head property
                        if (!queue.remove(v)) {
                            // means a previously processed vertex has a shorter length
                            // than calculated.
                            throw new RuntimeException("Dijkstra shortest path algorithm error!");
                        }
                        vAtts.put(VERTEX_ATTRIBUTE_DIST, uDistance + weight);
                        vAtts.put(VERTEX_ATTRIBUTE_PRED, u);
                        queue.add(v);
                    }
                }
            }
        }

        return vertexAttributeMaps;
    }

    private static Map<String, Object> createVertAttMap() {
        // use nulls or absence of keys to indicate infinite distances
        // and null predecessorVertex.
        Map<String, Object> map = new HashMap<>();
        map.put(VERTEX_ATTRIBUTE_DIST, null);
        map.put(VERTEX_ATTRIBUTE_PRED, null);
        return map;
    }
}