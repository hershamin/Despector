/*
 * The MIT License (MIT)
 *
 * Copyright (c) Despector <https://despector.voxelgenesis.com>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.test.ast;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.spongepowered.despector.ast.AstVisitor;
import org.spongepowered.despector.ast.generic.ClassTypeSignature;
import org.spongepowered.despector.ast.generic.TypeSignature;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.insn.condition.AndCondition;
import org.spongepowered.despector.ast.insn.condition.BooleanCondition;
import org.spongepowered.despector.ast.insn.condition.Condition;
import org.spongepowered.despector.ast.insn.condition.OrCondition;
import org.spongepowered.despector.util.ConditionUtil;
import org.spongepowered.despector.util.serialization.MessagePacker;

import java.io.IOException;

public class ConditionSimplificationTest {

    private static final BooleanCondition a = new BooleanCondition(new MockInsn('a'), false);
    private static final BooleanCondition anot = new BooleanCondition(a.getConditionValue(), true);
    private static final BooleanCondition b = new BooleanCondition(new MockInsn('b'), false);
    private static final BooleanCondition bnot = new BooleanCondition(b.getConditionValue(), true);
    private static final BooleanCondition c = new BooleanCondition(new MockInsn('c'), false);
    private static final BooleanCondition d = new BooleanCondition(new MockInsn('d'), false);
    private static final BooleanCondition e = new BooleanCondition(new MockInsn('e'), false);

    private static OrCondition or(Condition... c) {
        return new OrCondition(c);
    }

    private static AndCondition and(Condition... c) {
        return new AndCondition(c);
    }

    @Test
    public void testTrivial() {
        Condition complex = a;
        Condition simple = a;
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial2() {
        Condition complex = or(a, a);
        Condition simple = a;
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial3() {
        Condition complex = or(a, and(a, b));
        Condition simple = a;
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial4() {
        Condition complex = or(a, and(anot, b));
        Condition simple = or(a, b);
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial5() {
        Condition complex = or(a, and(anot, b), and(b, c));
        Condition simple = or(a, b);
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial6() {
        Condition complex = or(and(a, b), and(a, bnot, c));
        Condition simple = and(a, or(b, c));
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial7() {
        Condition complex = or(and(a, b), and(a, bnot));
        Condition simple = a;
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial8() {
        Condition complex = or(and(a, b), and(a, c), and(d, b), and(d, c));
        Condition simple = and(or(a, d), or(b, c));
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial9() {
        Condition complex = or(and(a, b), and(a, c), and(d, a), and(d, c));
        Condition simple = or(and(a, or(b, c, d)), and(d, c));
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial10() {
        Condition complex = or(and(a, b), and(a, c), and(d, c), and(d, e));
        Condition simple = or(and(a, or(b, c)), and(d, or(c, e)));
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial11() {
        Condition complex = or(and(a, d), and(e, d), and(b, c));
        Condition simple = or(and(or(a, e), d), and(b, c));
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    @Test
    public void testTrivial12() {
        Condition complex = or(a, and(b, b));
        Condition simple = or(a, b);
        Condition simplified = ConditionUtil.simplifyCondition(complex);
        assertEquals(simple, simplified);
    }

    private static class MockInsn implements Instruction {

        private char c;

        public MockInsn(char c) {
            this.c = c;
        }

        @Override
        public TypeSignature inferType() {
            return ClassTypeSignature.INT;
        }

        @Override
        public void accept(AstVisitor visitor) {
        }

        @Override
        public String toString() {
            return this.c + "";
        }

        @Override
        public void writeTo(MessagePacker pack) throws IOException {
        }

    }
}
