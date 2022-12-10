/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.properties.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.markers.LineContinuation;
import org.openrewrite.properties.tree.Properties;

import java.util.function.UnaryOperator;

public class PropertiesPrinter<P> extends PropertiesVisitor<PrintOutputCapture<P>> {

    @Override
    public Properties visitFile(Properties.File file, PrintOutputCapture<P> p) {
        beforeSyntax(file, p);
        visit(file.getContent(), p);
        p.out.append(file.getEof());
        afterSyntax(file, p);
        return file;
    }

    @Override
    public Properties visitEntry(Properties.Entry entry, PrintOutputCapture<P> p) {
        beforeSyntax(entry, p);
        LineContinuation lineContinuation = entry.getMarkers().findFirst(LineContinuation.class).orElse(null);
        // Track the current position relative to the properties without markers and / or continuation lines.
        StringBuilder currentPos = new StringBuilder();
        if (lineContinuation != null) {
            if (lineContinuation.getContinuationMap().containsKey(0)) {
                p.out.append(lineContinuation.getContinuationMap().get(0));
            }

            char[] charArray = entry.getKey().toCharArray();
            for (char c : charArray) {
                p.out.append(c);
                currentPos.append(c);

                if (lineContinuation.getContinuationMap().containsKey(currentPos.length())) {
                    p.out.append(lineContinuation.getContinuationMap().get(currentPos.length()));
                }
            }
        } else {
            p.out.append(entry.getKey());
            currentPos.append(entry.getKey());
        }

        if (lineContinuation != null) {
            for (char c : entry.getBeforeEquals().toCharArray()) {
                p.out.append(c);
                currentPos.append(c);

                if (lineContinuation.getContinuationMap().containsKey(currentPos.length())) {
                    p.out.append(lineContinuation.getContinuationMap().get(currentPos.length()));
                }
            }
        } else {
            p.out.append(entry.getBeforeEquals());
            currentPos.append(entry.getBeforeEquals());
        }

        p.append("=");
        currentPos.append("=");

        LineContinuation valueContinuation = entry.getValue().getMarkers().findFirst(LineContinuation.class).orElse(null);
        if (valueContinuation != null) {
            currentPos.setLength(0);
            for (Marker marker : entry.getValue().getMarkers().getMarkers()) {
                p.out.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), PROPERTIES_MARKER_WRAPPER));
            }

            char[] charArray = entry.getValue().getPrefix().toCharArray();
            for (char c : charArray) {
                p.out.append(c);
                currentPos.append(c);

                if (valueContinuation.getContinuationMap().containsKey(currentPos.length())) {
                    p.out.append(valueContinuation.getContinuationMap().get(currentPos.length()));
                }
            }

            visitMarkers(entry.getValue().getMarkers(), p);
            for (Marker marker : entry.getValue().getMarkers().getMarkers()) {
                p.out.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), PROPERTIES_MARKER_WRAPPER));
            }
        } else {
            beforeSyntax(entry.getValue().getPrefix(), entry.getValue().getMarkers(), p);
            currentPos.append(entry.getValue().getPrefix());
        }

        if (lineContinuation != null) {
            char[] charArray = entry.getValue().getText().toCharArray();
            for (char c : charArray) {
                p.out.append(c);
                currentPos.append(c);

                if (valueContinuation.getContinuationMap().containsKey(currentPos.length())) {
                    p.out.append(valueContinuation.getContinuationMap().get(currentPos.length()));
                }
            }
        } else {
            p.out.append(entry.getValue().getText());
        }

        afterSyntax(entry.getValue().getMarkers(), p);
        afterSyntax(entry, p);
        return entry;
    }

    @Override
    public Properties visitComment(Properties.Comment comment, PrintOutputCapture<P> p) {
        beforeSyntax(comment, p);
        p.out.append('#').append(comment.getMessage());
        afterSyntax(comment, p);
        return comment;
    }

    private static final UnaryOperator<String> PROPERTIES_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private void beforeSyntax(Properties props, PrintOutputCapture<P> p) {
        beforeSyntax(props.getPrefix(), props.getMarkers(), p);
    }

    private void beforeSyntax(String prefix, Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), PROPERTIES_MARKER_WRAPPER));
        }
        p.out.append(prefix);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), PROPERTIES_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Properties props, PrintOutputCapture<P> p) {
        afterSyntax(props.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), PROPERTIES_MARKER_WRAPPER));
        }
    }
}
