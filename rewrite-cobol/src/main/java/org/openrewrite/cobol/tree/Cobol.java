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
package org.openrewrite.cobol.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.cobol.CobolVisitor;
import org.openrewrite.cobol.internal.CobolPrinter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Cobol extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof CobolVisitor ? (R) acceptCobol((CobolVisitor<P>) v, p) : v.defaultValue(this, p);
    }

    @Nullable
    default <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof CobolVisitor;
    }

    String getPrefix();

    <P extends Cobol> P withPrefix(String prefix);

    <P extends Cobol> P withMarkers(Markers markers);

    Markers getMarkers();

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CompilationUnit implements Cobol, SourceFile {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Path sourcePath;

        @With
        @Getter
        @Nullable FileAttributes fileAttributes;

        @With
        @Getter
        String prefix;

        @With
        @Getter
        Markers markers;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        @Getter
        boolean charsetBomMarked;

        @With
        @Getter
        @Nullable Checksum checksum;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        List<CobolRightPadded<ProgramUnit>> programUnits;

        public List<ProgramUnit> getProgramUnits() {
            return CobolRightPadded.getElements(programUnits);
        }

        public CompilationUnit withProgramUnits(List<ProgramUnit> body) {
            return getPadding().withProgramUnits(CobolRightPadded.withElements(this.programUnits, body));
        }

        @With
        @Getter
        String eof;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new CobolPrinter<>();
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final CompilationUnit t;

            public List<CobolRightPadded<ProgramUnit>> getProgramUnits() {
                return t.programUnits;
            }

            public CompilationUnit withProgramUnits(List<CobolRightPadded<ProgramUnit>> programUnits) {
                return t.programUnits == programUnits ? t : new CompilationUnit(t.id, t.sourcePath, t.fileAttributes, t.prefix, t.markers, t.charsetName, t.charsetBomMarked, t.checksum, programUnits, t.eof);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Display implements Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;
        @Getter
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Getter
        @With
        String prefix;
        @Getter
        @With
        Markers markers;
        /**
         * Either an {@link Identifier} or {@link Literal}.
         */
        @Getter
        @With
        List<CobolLeftPadded<String>> operands;

        @Nullable
        CobolLeftPadded<Identifier> upon;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDisplay(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Nullable
        public Cobol.Identifier getUpon() {
            return upon == null ? null : upon.getElement();
        }

        public Display withUpon(@Nullable Cobol.Identifier upon) {
            if (upon == null) {
                return this.upon == null ? this : new Display(padding, id, prefix, markers, operands, null);
            }
            return getPadding().withUpon(CobolLeftPadded.withElement(this.upon, upon));
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Display t;

            @Nullable
            public CobolLeftPadded<Cobol.Identifier> getUpon() {
                return t.upon;
            }

            public Display withUpon(@Nullable CobolLeftPadded<Cobol.Identifier> upon) {
                return t.upon == upon ? t : new Display(t.padding, t.id, t.prefix, t.markers, t.operands, upon);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Identifier implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        String prefix;
        Markers markers;
        String simpleName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Literal implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        String prefix;
        Markers markers;
        Object value;
        String valueSource;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class IdentificationDivision implements Cobol {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Getter
        @With
        String prefix;
        @Getter
        @With
        Markers markers;
        CobolRightPadded<String> identification;
        CobolRightPadded<String> division;
        @Getter
        @With
        String dot;

        public enum IdKeyword {
            Identification,
            Id
        }

        @Getter
        @With
        ProgramIdParagraph programIdParagraph;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIdentificationDivision(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        public String getIdentification() {
            return identification.getElement();
        }

        public IdentificationDivision withIdentification(String identification) {
            //noinspection ConstantConditions
            return getPadding().withIdentification(CobolRightPadded.withElement(this.identification, identification));
        }

        public String getDivision() {
            return division.getElement();
        }

        public IdentificationDivision withDivision(String division) {
            //noinspection ConstantConditions
            return getPadding().withDivision(CobolRightPadded.withElement(this.division, division));
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final IdentificationDivision t;

            public CobolRightPadded<String> getIdentification() {
                return t.identification;
            }

            public IdentificationDivision withIdentification(CobolRightPadded<String> identification) {
                return t.identification == identification ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, identification, t.division, t.dot, t.programIdParagraph);
            }

            public CobolRightPadded<String> getDivision() {
                return t.division;
            }

            public IdentificationDivision withDivision(CobolRightPadded<String> division) {
                return t.division == division ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, t.identification, division, t.dot, t.programIdParagraph);
            }

        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureDivision implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        String prefix;
        Markers markers;
        @Getter
        @With
        CobolRightPadded<String> procedure;
        @Getter
        @With
        CobolRightPadded<String> division;
        @Getter
        @With
        String dot;
        @Getter
        @With
        ProcedureDivisionBody body;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivision(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureDivisionBody implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        String prefix;
        Markers markers;
        Paragraphs paragraphs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivisionBody(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Paragraphs implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        String prefix;
        Markers markers;
        List<Sentence> sentences;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitParagraphs(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Sentence implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        String prefix;
        Markers markers;
        List<Statement> statements;
        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSentence(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProgramIdParagraph implements Cobol {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;
        @Getter
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Getter
        @With
        String prefix;
        @Getter
        @With
        Markers markers;

        CobolRightPadded<String> programId;

        CobolRightPadded<String> dot1;

        @Getter
        @With
        String programName;

        @Nullable
        CobolLeftPadded<String> dot2;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProgramIdParagraph(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        public String getProgramId() {
            return programId.getElement();
        }

        public ProgramIdParagraph withProgramId(String programId) {
            //noinspection ConstantConditions
            return getPadding().withProgramId(CobolRightPadded.withElement(this.programId, programId));
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ProgramIdParagraph t;

            public CobolRightPadded<String> getProgramId() {
                return t.programId;
            }

            public CobolRightPadded<String> getDot1() {
                return t.dot1;
            }

            @Nullable
            public CobolLeftPadded<String> getDot2() {
                return t.dot2;
            }


            public ProgramIdParagraph withProgramId(CobolRightPadded<String> programId) {
                return t.programId == programId ? t : new ProgramIdParagraph(t.padding, t.id, t.prefix, t.markers, programId, t.dot1, t.programName, t.dot2);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProgramUnit implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        String prefix;
        Markers markers;
        IdentificationDivision identificationDivision;

        @Nullable
        ProcedureDivision procedureDivision;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProgramUnit(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Stop implements Statement {
        @EqualsAndHashCode.Include
        UUID id;
        String prefix;
        Markers markers;
        String stop;
        String run;
        Cobol statement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStop(this, p);
        }
    }
}
