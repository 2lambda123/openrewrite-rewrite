/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.dataflow.analysis;

import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public class SinkFlow<Source extends Expression, Sink extends J> extends FlowGraph {
    @Nullable
    private final Cursor source;

    private final LocalFlowSpec<Source, Sink> spec;

    public SinkFlow(@Nullable Cursor source, LocalFlowSpec<Source, Sink> spec, Cursor cursor) {
        super(cursor);
        this.source = source;
        this.spec = spec;
    }

    public Cursor getSourceCursor() {
        return requireNonNull(source);
    }

    public Source getSource() {
        return requireNonNull(source).getValue();
    }

    public List<Sink> getSinks() {
        List<List<Cursor>> flows = getFlows();
        List<Sink> sinks = new ArrayList<>(flows.size());
        for (List<Cursor> flow : flows) {
            sinks.add(flow.get(flow.size() - 1).getValue());
        }
        return sinks;
    }

    public List<List<Cursor>> getFlows() {
        List<Cursor> sourceFlow = null;
        if (source != null &&
                spec.getSinkType().isAssignableFrom(getSource().getClass()) &&
                spec.isSink((Sink) getSource(), getSourceCursor())) {
            // If the source is a sink, then generate a single-step flow for it
            sourceFlow = Collections.singletonList(getSourceCursor());
        }
        if (getEdges().isEmpty()) {
            if (sourceFlow != null) {
                return Collections.singletonList(sourceFlow);
            }
            return emptyList();
        }
        List<List<Cursor>> flows = new ArrayList<>();
        if (sourceFlow != null) {
            flows.add(sourceFlow);
        }
        Stack<Cursor> path = new Stack<>();
        path.push(getCursor());
        recurseGetFlows(this, path, flows);
        return flows;
    }

    public boolean isEmpty() {
        return getSinks().isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    private void recurseGetFlows(FlowGraph flowGraph, Stack<Cursor> pathToHere,
                                 List<List<Cursor>> pathsToSinks) {
        Cursor cursor = flowGraph.getCursor();
        Iterator<Cursor> cursorPath = cursor.getPathAsCursors();
        while (cursorPath.hasNext()) {
            Cursor c = cursorPath.next();
            if (c.getValue() instanceof Expression && spec.isBarrierGuard(c.getValue())) {
                return;
            }

            boolean isSinkType = spec.getSinkType().isAssignableFrom(c.getValue().getClass());
            if (isSinkType && spec.isSink(c.getValue(), c)) {
                List<Cursor> flow = new ArrayList<>(pathToHere);
                flow.add(c);
                pathsToSinks.add(flow);
            }
        }

        for (FlowGraph edge : flowGraph.getEdges()) {
            Stack<Cursor> pathToEdge = new Stack<>();
            pathToEdge.addAll(pathToHere);
            pathToEdge.add(edge.getCursor());
            recurseGetFlows(edge, pathToEdge, pathsToSinks);
        }
    }
}
