/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.UnwrapParentheses;
import org.openrewrite.java.style.UnnecessaryParenthesesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Incubating(since = "7.0.0")
public class UnnecessaryParenthesesVisitor<P> extends JavaVisitor<P> {

    private final UnnecessaryParenthesesStyle style;

    private static final String UNNECESSARY_PARENTHESES_MARKER = "unnecessaryParenthesesUnwrapTarget";

    public UnnecessaryParenthesesVisitor(UnnecessaryParenthesesStyle style) {
        this.style = style;
        setCursoringOn();
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, P p) {
        J par = super.visitParentheses(parens, p);
        Cursor c = getCursor().pollNearestMessage(UNNECESSARY_PARENTHESES_MARKER);
        if (c != null && (c.getValue() instanceof J.Literal || c.getValue() instanceof J.Ident)) {
            par = new UnwrapParentheses<>((J.Parentheses<?>) par).visit(par, p, getCursor());
        }
        return par;
    }

    @Override
    public J visitIdentifier(J.Ident ident, P p) {
        J.Ident i = visitAndCast(ident, p, super::visitIdentifier);
        if (style.isIdent() && getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.Parentheses) {
            getCursor().putMessageOnFirstEnclosing(J.Parentheses.class, UNNECESSARY_PARENTHESES_MARKER, getCursor());
        }
        return i;
    }

    @Override
    public J visitLiteral(J.Literal literal, P p) {
        J.Literal l = visitAndCast(literal, p, super::visitLiteral);
        JavaType.Primitive type = l.getType();
        if ((style.isNumInt() && type == JavaType.Primitive.Int) ||
                (style.isNumDouble() && type == JavaType.Primitive.Double) ||
                (style.isNumLong() && type == JavaType.Primitive.Long) ||
                (style.isNumFloat() && type == JavaType.Primitive.Float) ||
                (style.isStringLiteral() && type == JavaType.Primitive.String) ||
                (style.isLiteralNull() && type == JavaType.Primitive.Null) ||
                (style.isLiteralFalse() && type == JavaType.Primitive.Boolean && l.getValue() == Boolean.valueOf(false)) ||
                (style.isLiteralTrue() && type == JavaType.Primitive.Boolean && l.getValue() == Boolean.valueOf(true))) {
            if (getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.Parentheses) {
                getCursor().putMessageOnFirstEnclosing(J.Parentheses.class, UNNECESSARY_PARENTHESES_MARKER, getCursor());
            }
        }
        return l;
    }

    @Override
    public J visitAssignOp(J.AssignOp assignOp, P p) {
        J.AssignOp a = visitAndCast(assignOp, p, super::visitAssignOp);
        J.AssignOp.Type op = a.getOperator();
        if (a.getAssignment() instanceof J.Parentheses && ((style.isBandAssign() && op == J.AssignOp.Type.BitAnd) ||
                (style.isBorAssign() && op == J.AssignOp.Type.BitOr) ||
                (style.isBsrAssign() && op == J.AssignOp.Type.UnsignedRightShift) ||
                (style.isBxorAssign() && op == J.AssignOp.Type.BitXor) ||
                (style.isSrAssign() && op == J.AssignOp.Type.RightShift) ||
                (style.isSlAssign() && op == J.AssignOp.Type.LeftShift) ||
                (style.isMinusAssign() && op == J.AssignOp.Type.Subtraction) ||
                (style.isDivAssign() && op == J.AssignOp.Type.Division) ||
                (style.isPlusAssign() && op == J.AssignOp.Type.Addition) ||
                (style.isStarAssign() && op == J.AssignOp.Type.Multiplication) ||
                (style.isModAssign() && op == J.AssignOp.Type.Modulo))) {
            a = (J.AssignOp) new UnwrapParentheses<>((J.Parentheses<?>) a.getAssignment()).visit(a, p, getCursor());
        }
        return a;
    }

    @Override
    public J visitAssign(J.Assign assign, P p) {
        J.Assign a = visitAndCast(assign, p, super::visitAssign);
        if (style.isAssign() && a.getAssignment() instanceof J.Parentheses) {
            a = (J.Assign) new UnwrapParentheses<>((J.Parentheses<?>) a.getAssignment()).visit(a, p, getCursor());
        }
        return a;
    }

    @Override
    public J visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar v = visitAndCast(variable, p, super::visitVariable);
        if (style.isAssign() && v.getInitializer() != null && v.getInitializer() instanceof J.Parentheses) {
            v = (J.VariableDecls.NamedVar) new UnwrapParentheses<>((J.Parentheses<?>) v.getInitializer()).visit(v, p, getCursor());
        }
        return v;
    }

    @Override
    public J visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = visitAndCast(lambda, p, super::visitLambda);
        if (l.getParameters().getParams().size() == 1 &&
                l.getParameters().isParenthesized() &&
                l.getParameters().getParams().get(0) instanceof J.VariableDecls &&
                ((J.VariableDecls) l.getParameters().getParams().get(0)).getTypeExpr() == null) {
            l = l.withParameters(l.getParameters().withParenthesized(false));
        }
        return l;
    }

}


