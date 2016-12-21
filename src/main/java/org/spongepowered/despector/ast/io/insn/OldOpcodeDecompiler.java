/*
 * The MIT License (MIT)
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package org.spongepowered.despector.ast.io.insn;

import static org.objectweb.asm.Opcodes.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.despector.ast.io.emitter.ConditionSimplifier;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.AbstractSwitch;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.CatchLocal;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.DummyInstruction;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateCompareJump;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateConditionalJump;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateFrame;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateGoto;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateJump;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateLabel;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateLookupSwitch;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateStackValue;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateStatement;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.IntermediateTableSwitch;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.TryEnd;
import org.spongepowered.despector.ast.io.insn.OldIntermediateOpcode.TryStart;
import org.spongepowered.despector.ast.io.insn.Locals.DummyLocalInstance;
import org.spongepowered.despector.ast.io.insn.Locals.Local;
import org.spongepowered.despector.ast.members.insn.Statement;
import org.spongepowered.despector.ast.members.insn.StatementBlock;
import org.spongepowered.despector.ast.members.insn.arg.CastArg;
import org.spongepowered.despector.ast.members.insn.arg.CompareArg;
import org.spongepowered.despector.ast.members.insn.arg.InstanceFunctionArg;
import org.spongepowered.despector.ast.members.insn.arg.InstanceOfArg;
import org.spongepowered.despector.ast.members.insn.arg.Instruction;
import org.spongepowered.despector.ast.members.insn.arg.NewArrayArg;
import org.spongepowered.despector.ast.members.insn.arg.NewRefArg;
import org.spongepowered.despector.ast.members.insn.arg.StaticFunctionArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.DoubleConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.FloatConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.IntConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.LongConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.NullConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.StringConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.TypeConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.field.ArrayLoadArg;
import org.spongepowered.despector.ast.members.insn.arg.field.FieldArg;
import org.spongepowered.despector.ast.members.insn.arg.field.InstanceFieldArg;
import org.spongepowered.despector.ast.members.insn.arg.field.LocalArg;
import org.spongepowered.despector.ast.members.insn.arg.field.StaticFieldArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.AddArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.DivideArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.MultiplyArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.NegArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.OperatorArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.RemainderArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.ShiftLeftArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.ShiftRightArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.SubtractArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.UnsignedShiftRightArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.bitwise.AndArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.bitwise.OrArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.bitwise.XorArg;
import org.spongepowered.despector.ast.members.insn.assign.ArrayAssign;
import org.spongepowered.despector.ast.members.insn.assign.Assignment;
import org.spongepowered.despector.ast.members.insn.assign.FieldAssign;
import org.spongepowered.despector.ast.members.insn.assign.InstanceFieldAssign;
import org.spongepowered.despector.ast.members.insn.assign.LocalAssign;
import org.spongepowered.despector.ast.members.insn.assign.StaticFieldAssign;
import org.spongepowered.despector.ast.members.insn.branch.CatchBlock;
import org.spongepowered.despector.ast.members.insn.branch.DoWhileLoop;
import org.spongepowered.despector.ast.members.insn.branch.ElseBlock;
import org.spongepowered.despector.ast.members.insn.branch.ForLoop;
import org.spongepowered.despector.ast.members.insn.branch.IfBlock;
import org.spongepowered.despector.ast.members.insn.branch.TableSwitch;
import org.spongepowered.despector.ast.members.insn.branch.Ternary;
import org.spongepowered.despector.ast.members.insn.branch.TryBlock;
import org.spongepowered.despector.ast.members.insn.branch.WhileLoop;
import org.spongepowered.despector.ast.members.insn.branch.condition.AndCondition;
import org.spongepowered.despector.ast.members.insn.branch.condition.BooleanCondition;
import org.spongepowered.despector.ast.members.insn.branch.condition.CompareCondition;
import org.spongepowered.despector.ast.members.insn.branch.condition.CompareCondition.CompareOp;
import org.spongepowered.despector.ast.members.insn.branch.condition.Condition;
import org.spongepowered.despector.ast.members.insn.branch.condition.OrCondition;
import org.spongepowered.despector.ast.members.insn.function.InstanceMethodCall;
import org.spongepowered.despector.ast.members.insn.function.NewInstance;
import org.spongepowered.despector.ast.members.insn.function.StaticMethodCall;
import org.spongepowered.despector.ast.members.insn.misc.IncrementStatement;
import org.spongepowered.despector.ast.members.insn.misc.ReturnValue;
import org.spongepowered.despector.ast.members.insn.misc.ReturnVoid;
import org.spongepowered.despector.ast.members.insn.misc.ThrowException;
import org.spongepowered.despector.util.AstUtil;
import org.spongepowered.despector.util.TypeHelper;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@SuppressWarnings("unchecked")
public class OldOpcodeDecompiler {

    private final DecompilerOptions options;

    private Deque<Instruction> stack;
    private List<Instruction> pop_list;
    private Locals locals;
    private List<AbstractInsnNode> instructions;
    private int instructions_index;
    private TryCatchBlockNode[] trycatch_blocks;
    private int[] trycatch_indices;

    private List<OldIntermediateOpcode> intermediates;
    private Map<Label, Integer> label_indices;

    public OldOpcodeDecompiler(DecompilerOptions options) {
        this.options = options;
    }

    public StatementBlock decompile(InsnList instructions, Locals locals, List<TryCatchBlockNode> trycatch) {
        this.stack = Queues.newArrayDeque();
        this.locals = locals;
        this.intermediates = Lists.newArrayList();
        this.pop_list = Lists.newArrayList();

        this.trycatch_blocks = trycatch.toArray(new TryCatchBlockNode[trycatch.size()]);
        this.trycatch_indices = new int[this.trycatch_blocks.length];
        int next_index = 0;
        outer: for (int i = 0; i < trycatch.size(); i++) {
            TryCatchBlockNode next = trycatch.get(i);
            for (int o = i - 1; o >= 0; o--) {
                TryCatchBlockNode last = trycatch.get(o);
                if (next.start == last.start && next.end == last.end) {
                    this.trycatch_indices[i] = this.trycatch_indices[o];
                    continue outer;
                }
            }
            this.trycatch_indices[i] = next_index++;
        }

        buildIntermediates(instructions, locals);

        this.label_indices = calcLabelIndices();

        locals.bakeInstances(this.label_indices);

        return buildBlock(StatementBlock.Type.METHOD, 0, this.intermediates.size());
    }

    private static boolean references(Statement insn, Local local) {
        if (insn == null) {
            return false;
        }
        if (insn instanceof LocalAssign) {
            return ((LocalAssign) insn).getLocal().getLocal() == local;
        } else if (insn instanceof IncrementStatement) {
            return ((IncrementStatement) insn).getLocal().getLocal() == local;
        }
        return false;
    }

    private static boolean references(Condition condition, Local local) {
        if (condition instanceof BooleanCondition) {
            return references(((BooleanCondition) condition).getConditionValue(), local);
        } else if (condition instanceof CompareCondition) {
            return references(((CompareCondition) condition).getLeft(), local) || references(((CompareCondition) condition).getRight(), local);
        }
        return false;
    }

    private static boolean references(Instruction arg, Local local) {
        if (arg instanceof LocalArg) {
            return ((LocalArg) arg).getLocal().getLocal() == local;
        }
        return false;
    }

    private StatementBlock buildBlock(StatementBlock.Type type, int start, int end) {
        StatementBlock block = new StatementBlock(type, this.locals);
        Ternary tmp_ternary = null;
        for (int index = start; index < end; index++) {
            OldIntermediateOpcode next = this.intermediates.get(index);
            if (next instanceof IntermediateStatement) {
                Statement stmt = ((IntermediateStatement) next).getStatement();
                stmt.accept(new LocalDefineVisitor(index));
                if (tmp_ternary != null) {
                    if (stmt instanceof Assignment) {
                        ((Assignment) stmt).setValue(tmp_ternary);
                        tmp_ternary = null;
                    } else if (stmt instanceof ReturnValue) {
                        ((ReturnValue) stmt).setValue(tmp_ternary);
                        tmp_ternary = null;
                    }
                }
                block.append(stmt);
            } else if (next instanceof IntermediateStackValue) {
                IntermediateStackValue next_sv = (IntermediateStackValue) next;
                block.append(next_sv);
                if(next_sv.getStackVal() instanceof DummyInstruction) {
                    return block;
                }
            } else if (next instanceof AbstractSwitch) {
                AbstractSwitch aswitch = (AbstractSwitch) next;
                Instruction var = aswitch.getSwitchVar();
                TableSwitch tswitch = new TableSwitch(var);
                boolean added_dflt = false;
                List<LabelNode> labels = aswitch.getLabels();
                if (!labels.contains(aswitch.getDefault())) {
                    labels.add(aswitch.getDefault());
                    added_dflt = true;
                }
                int last = -1;
                LabelNode break_label = null;
                LabelNode last_label = null;
                int switch_end = index;
                for (int i = index + 1; i < end; i++) {
                    OldIntermediateOpcode cnext = this.intermediates.get(i);
                    if (cnext instanceof IntermediateLabel) {
                        if (labels.contains(((IntermediateLabel) cnext).getLabel())) {
                            if (last == -1) {
                                last = i;
                                last_label = ((IntermediateLabel) cnext).getLabel();
                                continue;
                            }
                            OldIntermediateOpcode last_op = this.intermediates.get(i - 1);
                            boolean breaks = false;
                            if (last_op instanceof IntermediateGoto) {
                                LabelNode label = ((IntermediateGoto) last_op).getNode().label;
                                if (this.label_indices.get(label.getLabel()) > i) {
                                    breaks = true;
                                    break_label = label;
                                }
                            }
                            int case_end = i;
                            if (breaks) {
                                case_end--;
                            }
                            boolean is_def = aswitch.getDefault() == last_label;
                            List<Integer> indices = Lists.newArrayList();
                            for (LabelNode l : labels) {
                                if (l == last_label) {
                                    indices.add(aswitch.indexFor(l));
                                }
                            }
                            StatementBlock body_block = buildBlock(StatementBlock.Type.SWITCH, last, case_end);
                            tswitch.addCase(new TableSwitch.Case(body_block, breaks, is_def, indices));

                            last = i;
                            last_label = ((IntermediateLabel) cnext).getLabel();
                            int case_index = labels.lastIndexOf(((IntermediateLabel) cnext).getLabel());
                            if (case_index == labels.size() - 1) {
                                is_def = aswitch.getDefault() == last_label;
                                indices = Lists.newArrayList();
                                for (LabelNode l : labels) {
                                    if (l == last_label) {
                                        if (l == aswitch.getDefault() && added_dflt) {
                                            continue;
                                        }
                                        indices.add(aswitch.indexFor(l));
                                    }
                                }
                                if (break_label != null) {
                                    int last_end = this.label_indices.get(break_label.getLabel());
                                    last_op = this.intermediates.get(last_end - 1);
                                    breaks = false;
                                    if (last_op instanceof IntermediateGoto) {
                                        LabelNode label = ((IntermediateGoto) last_op).getNode().label;
                                        if (this.label_indices.get(label.getLabel()) > i) {
                                            breaks = true;
                                        }
                                    }
                                    if (breaks) {
                                        last_end--;
                                    }
                                    StatementBlock last_block = buildBlock(StatementBlock.Type.SWITCH, i, last_end);

                                    tswitch.addCase(new TableSwitch.Case(last_block, false, is_def, indices));
                                    switch_end = last_end;
                                    if (breaks) {
                                        switch_end++;
                                    }
                                } else {
                                    for (int o = i; o < end; o++) {
                                        OldIntermediateOpcode onext = this.intermediates.get(o);
                                        if (onext instanceof IntermediateFrame) {
                                            case_end = o;
                                            break;
                                        }
                                    }
                                    int last_end = case_end;
                                    last_op = this.intermediates.get(last_end - 1);
                                    breaks = false;
                                    if (last_op instanceof IntermediateGoto) {
                                        LabelNode label = ((IntermediateGoto) last_op).getNode().label;
                                        if (this.label_indices.get(label.getLabel()) > i) {
                                            breaks = true;
                                        }
                                    }
                                    if (breaks) {
                                        last_end--;
                                    }
                                    StatementBlock last_block = buildBlock(StatementBlock.Type.SWITCH, i, last_end);
                                    tswitch.addCase(new TableSwitch.Case(last_block, false, is_def, indices));
                                    switch_end = case_end;
                                }
                            }
                        }
                    }
                }
                tswitch.accept(new LocalDefineVisitor(index));
                block.append(tswitch);
                index = switch_end;
            } else if (next instanceof IntermediateJump) {
                if (next instanceof IntermediateGoto) {
                    if (index == end - 1 && type == StatementBlock.Type.IF) {
                        break;
                    }
                    int target = this.label_indices.get(((IntermediateJump) next).getNode().label.getLabel());
                    ConditionResult result = makeCondition(target);
                    StatementBlock body_block = buildBlock(StatementBlock.Type.WHILE, index + 1, target);
                    if (!block.getStatements().isEmpty()) {
                        Statement init = block.getStatements().get(block.getStatements().size() - 1);
                        Statement incr = null;
                        if (!body_block.getStatements().isEmpty()) {
                            incr = body_block.getStatements().get(body_block.getStatements().size() - 1);
                        }
                        if (init instanceof LocalAssign) {
                            Local local = ((LocalAssign) init).getLocal().getLocal();
                            if (references(result.condition, local) || references(incr, local)) {
                                block.getStatements().remove(init);
                                if (references(incr, local)) {
                                    body_block.getStatements().remove(incr);
                                    ForLoop forloop = new ForLoop(init, ConditionSimplifier.invert(result.condition), incr, body_block);
                                    forloop.accept(new LocalDefineVisitor(index));
                                    block.append(forloop);
                                    index = result.end;
                                    continue;
                                }
                                ForLoop forloop = new ForLoop(init, ConditionSimplifier.invert(result.condition), null, body_block);
                                forloop.accept(new LocalDefineVisitor(index));
                                block.append(forloop);
                                index = result.end;
                                continue;
                            }
                        }
                    }
                    WhileLoop whileloop = new WhileLoop(ConditionSimplifier.invert(result.condition), body_block);
                    whileloop.accept(new LocalDefineVisitor(index));
                    block.append(whileloop);
                    index = result.end;
                    continue;
                }
                ConditionResult result = makeCondition(index);
                if (result.block_end < index) {
                    StatementBlock body_block = buildBlock(StatementBlock.Type.WHILE, result.block_end, index - 1);
                    block.getStatements().removeAll(body_block.getStatements());
                    DoWhileLoop dowhile = new DoWhileLoop(ConditionSimplifier.invert(result.condition), body_block);
                    dowhile.accept(new LocalDefineVisitor(index));
                    block.append(dowhile);
                    index = result.end;
                } else {
                    OldIntermediateOpcode last = this.intermediates.get(result.block_end - 1);
                    if (last instanceof IntermediateGoto) {
                        int last_target = this.label_indices.get(((IntermediateGoto) last).getNode().label.getLabel());
                        if (last_target < result.end) {
                            StatementBlock body_block = buildBlock(StatementBlock.Type.WHILE, result.end, result.block_end - 1);
                            if (!block.getStatements().isEmpty()) {
                                Statement init = block.getStatements().get(block.getStatements().size() - 1);
                                Statement incr = null;
                                if (!body_block.getStatements().isEmpty()) {
                                    incr = body_block.getStatements().get(body_block.getStatements().size() - 1);
                                }
                                if (init instanceof LocalAssign) {
                                    Local local = ((LocalAssign) init).getLocal().getLocal();
                                    if (references(result.condition, local) || references(incr, local)) {
                                        block.getStatements().remove(init);
                                        if (references(incr, local)) {
                                            body_block.getStatements().remove(incr);
                                            ForLoop forloop = new ForLoop(init, result.condition, incr, body_block);
                                            forloop.accept(new LocalDefineVisitor(index));
                                            block.append(forloop);
                                            index = result.end;
                                            continue;
                                        }
                                        ForLoop forloop = new ForLoop(init, result.condition, null, body_block);
                                        forloop.accept(new LocalDefineVisitor(index));
                                        block.append(forloop);
                                        index = result.end;
                                        continue;
                                    }
                                }
                            }
                            WhileLoop whileloop = new WhileLoop(result.condition, body_block);
                            whileloop.accept(new LocalDefineVisitor(index));
                            block.append(whileloop);
                            index = result.end;
                            continue;
                        }
                    }
                    StatementBlock body_block = buildBlock(StatementBlock.Type.IF, result.end, result.block_end);
                    boolean is_ternary = false;
                    boolean is_intermediate_ternary = false;
                    Instruction true_val = null;
                    Instruction false_val = null;
                    if (body_block.getStatements().get(0) instanceof IntermediateStackValue) {
                        is_ternary = true;
                        true_val = ((IntermediateStackValue) body_block.getStatements().get(0)).getStackVal();
                    }
                    IfBlock if_block = new IfBlock(result.condition, body_block);
                    if (last instanceof IntermediateGoto) {
                        int target = this.label_indices.get(((IntermediateGoto) last).getNode().label.getLabel());
                        if (target > result.block_end) {
                            StatementBlock else_block = buildBlock(StatementBlock.Type.IF, result.block_end + 1, target);
                            if (is_ternary) {
                                if (!(else_block.getStatements().get(0) instanceof IntermediateStackValue)) {
                                    throw new IllegalStateException();
                                }
                                if(else_block.getStatements().size() == 2 && else_block.getStatements().get(1) instanceof IntermediateStackValue) {
                                    is_intermediate_ternary = true;
                                }
                                false_val = ((IntermediateStackValue) else_block.getStatements().get(0)).getStackVal();
                            }
                            ElseBlock elseblock = new ElseBlock(else_block);
                            if_block.setElseBlock(elseblock);
                            result.block_end = target;
                        }
                    }
                    if (is_ternary) {
                        if (false_val == null || true_val == null) {
                            throw new IllegalStateException();
                        }
                        Ternary ternary = new Ternary(result.condition, true_val, false_val);
                        ternary.accept(new LocalDefineVisitor(index));
                        if(is_intermediate_ternary) {
                            block.append(new IntermediateStackValue(ternary));
                            return block;
                        }
                        tmp_ternary = ternary;
                    } else {
                        if_block.accept(new LocalDefineVisitor(index));
                        block.append(if_block);
                    }
                    index = result.block_end;
                }
            } else if (next instanceof TryStart) {
                int try_start = index + 1;
                int try_end = try_start;
                int try_index = ((TryStart) next).getIndex();
                for (; try_end < end; try_end++) {
                    OldIntermediateOpcode trynext = this.intermediates.get(try_end);
                    if (trynext instanceof TryEnd && ((TryEnd) trynext).getIndex() == try_index) {
                        break;
                    }
                }
                OldIntermediateOpcode last = this.intermediates.get(try_end - 1);
                List<CatchBlock> catches = Lists.newArrayList();
                LabelNode after_catch_label = null;
                if (last instanceof IntermediateGoto) {
                    try_end--;
                    after_catch_label = ((IntermediateGoto) last).getNode().label;
                }
                int last_end = try_end + 2;
                for (int i = 0; i < this.trycatch_indices.length; i++) {
                    if (this.trycatch_indices[i] == try_index) {
                        int catch_begin = this.label_indices.get(this.trycatch_blocks[i].handler.getLabel());
                        int catch_end = -1;
                        Local exception_local = null;
                        for (int o = last_end; o < end; o++) {
                            OldIntermediateOpcode op = this.intermediates.get(o);
                            if (op instanceof CatchLocal) {
                                exception_local = ((CatchLocal) op).getLocal();
                            } else if (after_catch_label != null && op instanceof IntermediateGoto
                                    && ((IntermediateGoto) op).getNode().label == after_catch_label) {
                                catch_end = o;
                                break;
                            }
                        }
                        if (catch_end == -1) {
                            if (exception_local == null) {
                                catch_end = catch_begin;
                            } else {
                                catch_end = exception_local.getInstance(catch_begin + 3).getEnd();
                            }
                            last_end = catch_end;
                        } else {
                            last_end = catch_end + 1;
                        }
                        List<String> exceptions = Lists.newArrayList();
                        exceptions.add("L" + this.trycatch_blocks[i].type + ";");
                        for (int o = i + 1; o < this.trycatch_indices.length; o++) {
                            if (this.trycatch_indices[o] == try_index && this.trycatch_blocks[o].handler == this.trycatch_blocks[i].handler) {
                                exceptions.add("L" + this.trycatch_blocks[o].type + ";");
                                i = o;
                            }
                        }
                        if (catch_end != catch_begin) {
                            StatementBlock catch_block = buildBlock(StatementBlock.Type.CATCH, catch_begin, catch_end);
                            catches.add(new CatchBlock(exception_local.getInstance(catch_end - 1), exceptions, catch_block));
                        } else {
                            catches.add(new CatchBlock(this.locals.getNonConflictingName("e", catch_begin), exceptions,
                                    new StatementBlock(StatementBlock.Type.CATCH, this.locals)));
                        }
                    }
                }
                StatementBlock try_body = buildBlock(StatementBlock.Type.TRY, try_start, try_end);
                TryBlock try_block = new TryBlock(try_body);
                for (CatchBlock c : catches) {
                    try_block.getCatchBlocks().add(c);
                }
                try_block.accept(new LocalDefineVisitor(index));
                block.append(try_block);
                index = last_end;
            }
        }
        if (tmp_ternary != null) {
            block.append(new IntermediateStackValue(tmp_ternary));
        }
        return block;
    }

    private static enum CompareOps {
        AND,
        OR,
        MID,
        MID_OR
    }

    static class ConditionResult {

        public Condition condition;
        int end;
        int block_end;

    }

    private ConditionResult makeCondition(int index) {
        int condition_start = index;
        int condition_end = index;
        for (; condition_end < this.intermediates.size(); condition_end++) {
            OldIntermediateOpcode onext = this.intermediates.get(condition_end);
            if (onext instanceof IntermediateStatement || onext instanceof IntermediateGoto || onext instanceof IntermediateStackValue) {
                break;
            }
        }
        Set<LabelNode> seen_labels = Sets.newHashSet();
        List<OldIntermediateOpcode> group = Lists.newArrayList();
        int farthest = 0;
        outer: for (int i = condition_start; i < condition_end; i++) {
            OldIntermediateOpcode cnext = this.intermediates.get(i);
            if (cnext instanceof IntermediateLabel) {
                if (i == condition_end - 1) {
                    break;
                }
                for (int o = i + 1; o < this.intermediates.size(); o++) {
                    OldIntermediateOpcode onext = this.intermediates.get(o);
                    if (onext instanceof IntermediateGoto) {
                        if (((IntermediateLabel) cnext).getLabel() == ((IntermediateGoto) onext).getNode().label) {
                            for (int c = condition_start; c < i; c++) {
                                group.add(this.intermediates.get(c));
                            }
                            condition_end = i;
                            farthest = -1;
                            break outer;
                        }
                    }
                }
                boolean sharing = false;
                for (int o = i + 1; o < condition_end; o++) {
                    OldIntermediateOpcode onext = this.intermediates.get(o);
                    if (onext instanceof IntermediateJump) {
                        LabelNode label = ((IntermediateJump) onext).getNode().label;
                        if (seen_labels.contains(label)) {
                            sharing = true;
                            break;
                        }
                        if (this.label_indices.get(label.getLabel()) > farthest) {
                            sharing = true;
                            break;
                        }
                    } else if (onext instanceof IntermediateLabel) {
                        break;
                    }
                }
                if (!sharing) {
                    for (int c = condition_start; c < i; c++) {
                        group.add(this.intermediates.get(c));
                    }
                    condition_end = i;
                    farthest = -1;
                    break;
                }
            } else if (cnext instanceof IntermediateJump) {
                LabelNode label = ((IntermediateJump) cnext).getNode().label;
                seen_labels.add(label);
                int target = this.label_indices.get(label.getLabel());
                if (target > farthest) {
                    farthest = target;
                }
            }
        }

        if (farthest != -1) {
            for (int c = condition_start; c < condition_end; c++) {
                group.add(this.intermediates.get(c));
            }
        }
        for (int c = group.size() - 1; c >= 0; c--) {
            if (group.get(c) instanceof IntermediateJump) {
                break;
            }
            group.remove(group.get(c));
            condition_end--;
        }
        
        Set<LabelNode> outer = Sets.newHashSet();
        boolean broken = false;
        for(Iterator<OldIntermediateOpcode> it = group.iterator(); it.hasNext();) {
            OldIntermediateOpcode next = it.next();
            if(broken) {
                it.remove();
                continue;
            }
            if(next instanceof IntermediateJump) {
                int target = this.label_indices.get(((IntermediateJump) next).getNode().label.getLabel());
                if(target > condition_end + 1 || target < condition_start) {
                    outer.add(((IntermediateJump) next).getNode().label);
                    if(outer.size() > 1) {
                        broken = true;
                        it.remove();
                    }
                }
            }
        }
        condition_end = condition_start + group.size();

        Condition condition = makeCondition(group);
        LabelNode break_node = ((IntermediateJump) group.get(group.size() - 1)).getNode().label;

        int block_end = this.label_indices.get(break_node.getLabel());
        ConditionResult result = new ConditionResult();
        result.condition = condition;
        result.end = condition_end;
        result.block_end = block_end;
        return result;
    }

    private Condition makeCondition(List<OldIntermediateOpcode> group) {
        int start = this.intermediates.indexOf(group.get(0));
        OldIntermediateOpcode last = group.get(group.size() - 1);
        int end = this.intermediates.indexOf(last);
        LabelNode break_node = ((IntermediateJump) group.get(group.size() - 1)).getNode().label;

        Deque<Condition> stack = Queues.newArrayDeque();
        Deque<CompareOps> ops_stack = Queues.newArrayDeque();

        for (int i = 0; i < group.size(); i++) {
            OldIntermediateOpcode next = group.get(i);
            if (next instanceof IntermediateJump) {
                JumpInsnNode node = ((IntermediateJump) next).getNode();
                if (node.label == break_node) {
                    if (next instanceof IntermediateConditionalJump) {
                        if (node.getOpcode() == IFEQ) {
                            stack.push(new BooleanCondition(((IntermediateConditionalJump) next).getCondition(), false));
                        } else if (node.getOpcode() == IFNE) {
                            stack.push(new BooleanCondition(((IntermediateConditionalJump) next).getCondition(), true));
                        } else if (node.getOpcode() == IFNULL) {
                            stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new NullConstantArg(),
                                    CompareOp.NOT_EQUAL));
                        } else if (node.getOpcode() == IFNONNULL) {
                            stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new NullConstantArg(),
                                    CompareOp.EQUAL));
                        } else {
                            stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new IntConstantArg(0),
                                    CompareCondition.fromOpcode(node.getOpcode()).inverse()));
                        }
                    } else {
                        IntermediateCompareJump cmp = (IntermediateCompareJump) next;
                        stack.push(new CompareCondition(cmp.getLeft(), cmp.getRight(), CompareCondition.fromOpcode(node.getOpcode()).inverse()));
                    }
                    if (!ops_stack.isEmpty()) {
                        CompareOps op = ops_stack.peek();
                        if (op == CompareOps.AND) {
                            ops_stack.pop();
                            Condition left = stack.pop();
                            Condition right = stack.pop();
                            AndCondition cond = new AndCondition(right, left);
                            stack.push(cond);
                        }
                        if (i < group.size() - 1) {
                            if (group.get(i + 1) instanceof IntermediateLabel) {
                                CompareOps nop = ops_stack.peek();
                                if (nop == CompareOps.OR || nop == CompareOps.MID) {
                                    ops_stack.pop();
                                    Condition left = stack.pop();
                                    Condition right = ConditionSimplifier.invert(stack.pop());
                                    OrCondition cond = new OrCondition(right, left);
                                    stack.push(cond);
                                    ops_stack.push(CompareOps.AND);
                                }
                            } else {
                                ops_stack.push(CompareOps.AND);
                            }
                        }
                    } else if (i < group.size() - 1) {
                        ops_stack.push(CompareOps.AND);
                    }
                } else {
                    int jump_target = this.label_indices.get(node.label.getLabel());
                    if (jump_target <= end && jump_target >= start) {
                        if (next instanceof IntermediateConditionalJump) {
                            if (node.getOpcode() == IFEQ) {
                                stack.push(new BooleanCondition(((IntermediateConditionalJump) next).getCondition(), false));
                            } else if (node.getOpcode() == IFNE) {
                                stack.push(new BooleanCondition(((IntermediateConditionalJump) next).getCondition(), true));
                            } else if (node.getOpcode() == IFNULL) {
                                stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new NullConstantArg(),
                                        CompareOp.NOT_EQUAL));
                            } else if (node.getOpcode() == IFNONNULL) {
                                stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new NullConstantArg(),
                                        CompareOp.EQUAL));
                            } else {
                                stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new IntConstantArg(0),
                                        CompareCondition.fromOpcode(node.getOpcode()).inverse()));
                            }
                        } else {
                            IntermediateCompareJump cmp = (IntermediateCompareJump) next;
                            stack.push(new CompareCondition(cmp.getLeft(), cmp.getRight(), CompareCondition.fromOpcode(node.getOpcode()).inverse()));
                        }
                        if (ops_stack.peek() == CompareOps.MID) {
                            ops_stack.pop();
                            Condition left = stack.pop();
                            Condition right = ConditionSimplifier.invert(stack.pop());
                            OrCondition cond = new OrCondition(right, left);
                            stack.push(cond);
                        }
                        ops_stack.push(CompareOps.MID);
                    } else {
                        if (next instanceof IntermediateConditionalJump) {
                            if (node.getOpcode() == IFEQ) {
                                stack.push(new BooleanCondition(((IntermediateConditionalJump) next).getCondition(), true));
                            } else if (node.getOpcode() == IFNE) {
                                stack.push(new BooleanCondition(((IntermediateConditionalJump) next).getCondition(), false));
                            } else if (node.getOpcode() == IFNULL) {
                                stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new NullConstantArg(),
                                        CompareOp.EQUAL));
                            } else if (node.getOpcode() == IFNONNULL) {
                                stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new NullConstantArg(),
                                        CompareOp.NOT_EQUAL));
                            } else {
                                stack.push(new CompareCondition(((IntermediateConditionalJump) next).getCondition(), new IntConstantArg(0),
                                        CompareCondition.fromOpcode(node.getOpcode())));
                            }
                        } else {
                            IntermediateCompareJump cmp = (IntermediateCompareJump) next;
                            stack.push(new CompareCondition(cmp.getLeft(), cmp.getRight(), CompareCondition.fromOpcode(node.getOpcode())));
                        }
                        if (ops_stack.peek() == CompareOps.MID) {
                            if (i < group.size() - 1 && group.get(i + 1) instanceof IntermediateLabel) {
                                ops_stack.pop();
                                Condition left = stack.pop();
                                Condition right = stack.pop();
                                AndCondition cond = new AndCondition(right, left);
                                stack.push(cond);
                            } else {
                                ops_stack.pop();
                                ops_stack.push(CompareOps.MID_OR);
                            }
                        } else if (ops_stack.peek() == CompareOps.MID_OR) {
                            ops_stack.pop();
                            Condition left = stack.pop();
                            Condition right = stack.pop();
                            OrCondition cond = new OrCondition(right, left);
                            Condition right2 = stack.pop();
                            AndCondition cond2 = new AndCondition(right2, cond);
                            stack.push(cond2);
                        } else {
                            ops_stack.push(CompareOps.OR);
                        }
                    }
                }
            }
        }
        while (!ops_stack.isEmpty()) {
            Condition left = stack.pop();
            Condition right = stack.pop();
            CompareOps op = ops_stack.pop();
            if (op == CompareOps.AND) {
                AndCondition cond = new AndCondition(right, left);
                stack.push(cond);
            } else if (op == CompareOps.OR) {
                OrCondition cond = new OrCondition(right, left);
                stack.push(cond);
            }
        }
        if (stack.size() == 2) {
            Condition left = stack.pop();
            Condition right = stack.pop();
            OrCondition cond = new OrCondition(right, left);
            stack.push(cond);
        }

        return stack.pop();
    }

    private static int intermediate_stack = 0;
    private static int expecting_intermediate_stack = 0;

    private void handleIntermediate(AbstractInsnNode next) {
        if (next instanceof JumpInsnNode) {
            if (next.getOpcode() == GOTO) {
                if (!this.stack.isEmpty()) {
                    expecting_intermediate_stack++;
                }
                this.intermediates.add(new IntermediateGoto((JumpInsnNode) next));
            } else if (next.getOpcode() == IF_ACMPEQ || next.getOpcode() == IF_ACMPNE || next.getOpcode() == IF_ICMPEQ
                    || next.getOpcode() == IF_ICMPGE || next.getOpcode() == IF_ICMPGT || next.getOpcode() == IF_ICMPLE
                    || next.getOpcode() == IF_ICMPLT || next.getOpcode() == IF_ICMPNE) {
                Instruction right = this.stack.pop();
                Instruction left = this.stack.pop();
                this.intermediates.add(new IntermediateCompareJump((JumpInsnNode) next, left, right));
            } else {
                Instruction condition = this.stack.pop();
                this.intermediates.add(new IntermediateConditionalJump((JumpInsnNode) next, condition));
            }
        } else if (next instanceof TableSwitchInsnNode) {
            this.intermediates.add(new IntermediateTableSwitch((TableSwitchInsnNode) next, this.stack.pop()));
        } else if (next instanceof LookupSwitchInsnNode) {
            this.intermediates.add(new IntermediateLookupSwitch((LookupSwitchInsnNode) next, this.stack.pop()));
        } else if (next instanceof LineNumberNode) {
        } else if (next instanceof LabelNode) {
            this.intermediates.add(new IntermediateLabel((LabelNode) next));
            if (this.trycatch_blocks != null) {
                for (int i = 0; i < this.trycatch_blocks.length; i++) {
                    TryCatchBlockNode trycatch = this.trycatch_blocks[i];
                    if (trycatch.start == next) {
                        this.intermediates.add(new TryStart(this.trycatch_indices[i]));
                        break;
                    } else if (trycatch.end == next) {
                        AbstractInsnNode after = this.instructions.get(this.instructions_index);
                        if (after instanceof LineNumberNode) {
                            this.instructions_index++;
                            after = this.instructions.get(this.instructions_index);
                        }
                        if (after.getOpcode() == GOTO) {
                            this.instructions_index++;
                            this.intermediates.add(new IntermediateGoto((JumpInsnNode) after));
                        } else if (after.getOpcode() >= IRETURN && after.getOpcode() <= ARETURN) {
                            this.instructions_index++;
                            handleIntermediate(after);
                        }
                        this.intermediates.add(new TryEnd(this.trycatch_indices[i]));
                        break;
                    }
                }
            }
        } else if (next instanceof FrameNode) {
            FrameNode frame = (FrameNode) next;
            if ((frame.type == F_SAME1 && this.stack.isEmpty()) || (frame.type == F_FULL && frame.stack.size() == this.stack.size() + 1)) {
                OldIntermediateOpcode last = this.intermediates.get(this.intermediates.size() - 1);
                if (last instanceof IntermediateLabel) {
                    for (int i = 0; i < this.trycatch_blocks.length; i++) {
                        if (this.trycatch_blocks[i].handler == ((IntermediateLabel) last).getLabel()) {
                            AbstractInsnNode after = this.instructions.get(this.instructions_index);
                            if (after.getOpcode() == ASTORE) {
                                this.intermediates.add(new CatchLocal(this.locals.getLocal(((VarInsnNode) after).var)));
                                this.instructions_index++;
                            } else if (after.getOpcode() == POP) {
                                this.instructions_index++;
                            }
                            this.intermediates.add(new IntermediateFrame(frame));
                            return;
                        }
                    }
                }
                this.stack.push(this.pop_list.get(this.pop_list.size() - 1));
            } else if ((frame.type == F_SAME1 && this.stack.size() == 1) || (frame.type == F_FULL && frame.stack.size() == this.stack.size())) {
                if (intermediate_stack > 0 && !this.stack.isEmpty()) {
                    this.intermediates.add(this.intermediates.size() - 1, new IntermediateStackValue(this.stack.pop()));
                    this.stack.push(new DummyInstruction());
                    intermediate_stack--;
                }
            } else if ((frame.type == F_SAME && this.stack.size() == 1) || (frame.type == F_FULL && frame.stack.size() == this.stack.size() - 1)
                    || (frame.type == F_SAME1 && this.stack.size() == 2) || (frame.type == F_APPEND && this.stack.size() == 1)) {
                if (expecting_intermediate_stack > 0 && !this.stack.isEmpty()) {
                    this.intermediates.add(this.intermediates.size() - 2, new IntermediateStackValue(this.stack.pop()));
                    expecting_intermediate_stack--;
                    if(intermediate_stack > 0) {
                        this.intermediates.add(this.intermediates.size() - 2, new IntermediateStackValue(new DummyInstruction()));
                        intermediate_stack--;
                    }
                    intermediate_stack++;
                }
            }
            this.intermediates.add(new IntermediateFrame(frame));
        } else {
            if (intermediate_stack > 0 && !this.stack.isEmpty() && next.getOpcode() >= IRETURN && next.getOpcode() <= ARETURN) {
                this.intermediates.add(this.intermediates.size() - 1, new IntermediateStackValue(this.stack.pop()));
                this.stack.push(new DummyInstruction());
                intermediate_stack--;
            }
            OpHandler handle = handlers[next.getOpcode()];
            if (handle == null) {
                System.err.println("Unsupported opcode " + next.getOpcode());
                throw new IllegalStateException();
            }
            handle.handle(this, next);
        }
    }

    private void buildIntermediates(InsnList instructions, Locals locals) {
        this.instructions = Lists.newArrayList();
        Iterator<AbstractInsnNode> it = instructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode next = it.next();
            this.instructions.add(next);
        }
        intermediate_stack = 0;
        expecting_intermediate_stack = 0;
        for (this.instructions_index = 0; this.instructions_index < this.instructions.size();) {
            AbstractInsnNode next = this.instructions.get(this.instructions_index++);
//            System.out.println(AstUtil.insnToString(next));
            handleIntermediate(next);
        }
//        System.out.println("Intermediates:");
//        for (IntermediateOpcode op : this.intermediates) {
//            System.out.println(op.toString());
//        }
    }

    private Map<Label, Integer> calcLabelIndices() {
        Map<Label, Integer> indices = Maps.newHashMap();
        int i = 0;
        for (OldIntermediateOpcode next : this.intermediates) {
            if (next instanceof OldIntermediateOpcode.IntermediateLabel) {
                indices.put(((OldIntermediateOpcode.IntermediateLabel) next).getLabel().getLabel(), i);
            }
            i++;
        }
        return indices;
    }

    public void push(Instruction arg) {
        this.stack.push(arg);
    }

    public Instruction pop() {
        Instruction insn = this.stack.pop();
        this.pop_list.add(insn);
        return insn;
    }

    public Instruction peek() {
        return this.stack.peek();
    }

    public void append(Statement statement) {
        this.intermediates.add(new OldIntermediateOpcode.IntermediateStatement(statement));
    }

    public Locals.Local getLocal(int index) {
        return this.locals.getLocal(index);
    }

    public AbstractInsnNode next() {
        AbstractInsnNode next = this.instructions.get(this.instructions_index++);
//        System.out.println(AstUtil.insnToString(next));
        return next;
    }

    public void revert(int n) {
//        System.out.println("Back up " + n);
        this.instructions_index -= n;
    }

    /**
     * A handler for a single opcode.
     */
    @FunctionalInterface
    private static interface OpHandler {

        void handle(OldOpcodeDecompiler state, AbstractInsnNode next);
    }

    // Highest opcode is IFNONNULL at 199
    private static final int MAX_OPCODE = 200;
    private static final OpHandler[] handlers = new OpHandler[MAX_OPCODE];

    static {
        OpHandler noop = (state, next) -> {
        };
        handlers[NOP] = noop;
        // Constants
        handlers[ACONST_NULL] = (state, next) -> {
            state.push(new NullConstantArg());
        };
        handlers[ICONST_M1] = (state, next) -> {
            state.push(new IntConstantArg(-1));
        };
        handlers[ICONST_0] = (state, next) -> {
            state.push(new IntConstantArg(0));
        };
        handlers[ICONST_1] = (state, next) -> {
            state.push(new IntConstantArg(1));
        };
        handlers[ICONST_2] = (state, next) -> {
            state.push(new IntConstantArg(2));
        };
        handlers[ICONST_3] = (state, next) -> {
            state.push(new IntConstantArg(3));
        };
        handlers[ICONST_4] = (state, next) -> {
            state.push(new IntConstantArg(4));
        };
        handlers[ICONST_5] = (state, next) -> {
            state.push(new IntConstantArg(5));
        };
        handlers[LCONST_0] = (state, next) -> {
            state.push(new LongConstantArg(0));
        };
        handlers[LCONST_1] = (state, next) -> {
            state.push(new LongConstantArg(1));
        };
        handlers[FCONST_0] = (state, next) -> {
            state.push(new FloatConstantArg(0));
        };
        handlers[FCONST_1] = (state, next) -> {
            state.push(new FloatConstantArg(1));
        };
        handlers[FCONST_2] = (state, next) -> {
            state.push(new FloatConstantArg(2));
        };
        handlers[DCONST_0] = (state, next) -> {
            state.push(new DoubleConstantArg(0));
        };
        handlers[DCONST_1] = (state, next) -> {
            state.push(new DoubleConstantArg(1));
        };
        OpHandler int_constant = (state, next) -> {
            IntInsnNode val = (IntInsnNode) next;
            IntConstantArg arg = new IntConstantArg(val.operand);
            state.push(arg);
        };
        handlers[BIPUSH] = int_constant;
        handlers[SIPUSH] = int_constant;
        handlers[LDC] = (state, next) -> {
            LdcInsnNode ldc = (LdcInsnNode) next;
            if (ldc.cst instanceof String) {
                state.push(new StringConstantArg((String) ldc.cst));
            } else if (ldc.cst instanceof Integer) {
                state.push(new IntConstantArg((Integer) ldc.cst));
            } else if (ldc.cst instanceof Float) {
                state.push(new FloatConstantArg((Float) ldc.cst));
            } else if (ldc.cst instanceof Long) {
                // LDC_W appears to be merged with this opcode by asm so long
                // and double constants will also be here
                state.push(new LongConstantArg((Long) ldc.cst));
            } else if (ldc.cst instanceof Double) {
                state.push(new DoubleConstantArg((Double) ldc.cst));
            } else if (ldc.cst instanceof Type) {
                state.push(new TypeConstantArg((Type) ldc.cst));
            } else {
                throw new IllegalStateException("Unsupported ldc constant: " + ldc.cst);
            }
        };
        // Local variable load/store
        OpHandler local_load = (state, next) -> {
            VarInsnNode var = (VarInsnNode) next;
            Local local = state.getLocal(var.var);
            LocalArg arg = new LocalArg(new DummyLocalInstance(local));
            state.push(arg);
        };
        handlers[ILOAD] = local_load;
        handlers[LLOAD] = local_load;
        handlers[FLOAD] = local_load;
        handlers[DLOAD] = local_load;
        handlers[ALOAD] = local_load;
        OpHandler array_load = (state, next) -> {
            Instruction index = state.pop();
            Instruction var = state.pop();
            ArrayLoadArg load = new ArrayLoadArg(var, index);
            state.push(load);
        };
        handlers[IALOAD] = array_load;
        handlers[LALOAD] = array_load;
        handlers[FALOAD] = array_load;
        handlers[DALOAD] = array_load;
        handlers[AALOAD] = array_load;
        handlers[BALOAD] = array_load;
        handlers[CALOAD] = array_load;
        handlers[SALOAD] = array_load;
        OpHandler local_store = (state, next) -> {
            VarInsnNode var = (VarInsnNode) next;
            Instruction val = state.pop();
            Local local = state.getLocal(var.var);
            LocalAssign insn = new LocalAssign(new DummyLocalInstance(local), val);
            state.append(insn);
        };
        handlers[ISTORE] = local_store;
        handlers[LSTORE] = local_store;
        handlers[FSTORE] = local_store;
        handlers[DSTORE] = local_store;
        handlers[ASTORE] = local_store;
        OpHandler array_store = (state, next) -> {
            Instruction val = state.pop();
            Instruction index = state.pop();
            Instruction var = state.pop();
            ArrayAssign assign = new ArrayAssign(var, index, val);
            state.append(assign);
        };
        handlers[IASTORE] = array_store;
        handlers[LASTORE] = array_store;
        handlers[FASTORE] = array_store;
        handlers[DASTORE] = array_store;
        handlers[AASTORE] = array_store;
        handlers[BASTORE] = array_store;
        handlers[CASTORE] = array_store;
        handlers[SASTORE] = array_store;
        handlers[POP] = (state, next) -> {
            Instruction arg = state.pop();
            if (arg instanceof InstanceFunctionArg) {
                state.append(new InstanceMethodCall((InstanceFunctionArg) arg));
            } else if (arg instanceof StaticFunctionArg) {
                state.append(new StaticMethodCall((StaticFunctionArg) arg));
            }
        };
        handlers[POP2] = (state, next) -> {
            Instruction arg = state.pop();
            if (arg instanceof InstanceFunctionArg) {
                state.append(new InstanceMethodCall((InstanceFunctionArg) arg));
            } else if (arg instanceof StaticFunctionArg) {
                state.append(new StaticMethodCall((StaticFunctionArg) arg));
            } else {
                throw new IllegalStateException("Unknown arg being popped: " + arg);
            }
            arg = state.pop();
            if (arg instanceof InstanceFunctionArg) {
                state.append(new InstanceMethodCall((InstanceFunctionArg) arg));
            } else if (arg instanceof StaticFunctionArg) {
                state.append(new StaticMethodCall((StaticFunctionArg) arg));
            } else {
                throw new IllegalStateException("Unknown arg being popped: " + arg);
            }
        };
        // Stack manipulation
        handlers[DUP] = (state, next) -> {
            state.push(state.peek());
        };
        handlers[DUP_X1] = (state, next) -> {
            Instruction val = state.pop();
            Instruction val2 = state.pop();
            state.push(val);
            state.push(val2);
            state.push(val);
        };
        handlers[DUP_X2] = (state, next) -> {
            Instruction val = state.pop();
            Instruction val2 = state.pop();
            Instruction val3 = state.pop();
            state.push(val);
            state.push(val3);
            state.push(val2);
            state.push(val);
        };
        handlers[DUP2] = (state, next) -> {
            Instruction val = state.pop();
            Instruction val2 = state.peek();
            state.push(val);
            state.push(val2);
            state.push(val);
        };
        handlers[DUP2_X1] = (state, next) -> {
            Instruction val = state.pop();
            Instruction val2 = state.pop();
            Instruction val3 = state.pop();
            state.push(val2);
            state.push(val);
            state.push(val3);
            state.push(val2);
            state.push(val);
        };
        handlers[DUP2_X2] = (state, next) -> {
            Instruction val = state.pop();
            Instruction val2 = state.pop();
            Instruction val3 = state.pop();
            Instruction val4 = state.pop();
            state.push(val2);
            state.push(val);
            state.push(val4);
            state.push(val3);
            state.push(val2);
            state.push(val);
        };
        handlers[SWAP] = (state, next) -> {
            Instruction val = state.pop();
            Instruction val2 = state.pop();
            state.push(val);
            state.push(val2);
        };
        // Operators
        handlers[IADD] = new OperatorHandler(AddArg::new);
        handlers[LADD] = new OperatorHandler(AddArg::new);
        handlers[FADD] = new OperatorHandler(AddArg::new);
        handlers[DADD] = new OperatorHandler(AddArg::new);
        handlers[ISUB] = new OperatorHandler(SubtractArg::new);
        handlers[LSUB] = new OperatorHandler(SubtractArg::new);
        handlers[FSUB] = new OperatorHandler(SubtractArg::new);
        handlers[DSUB] = new OperatorHandler(SubtractArg::new);
        handlers[IMUL] = new OperatorHandler(MultiplyArg::new);
        handlers[LMUL] = new OperatorHandler(MultiplyArg::new);
        handlers[FMUL] = new OperatorHandler(MultiplyArg::new);
        handlers[DMUL] = new OperatorHandler(MultiplyArg::new);
        handlers[IDIV] = new OperatorHandler(DivideArg::new);
        handlers[LDIV] = new OperatorHandler(DivideArg::new);
        handlers[FDIV] = new OperatorHandler(DivideArg::new);
        handlers[DDIV] = new OperatorHandler(DivideArg::new);
        handlers[IREM] = new OperatorHandler(RemainderArg::new);
        handlers[LREM] = new OperatorHandler(RemainderArg::new);
        handlers[FREM] = new OperatorHandler(RemainderArg::new);
        handlers[DREM] = new OperatorHandler(RemainderArg::new);
        OpHandler neg = (state, next) -> {
            state.push(new NegArg(state.pop()));
        };
        handlers[INEG] = neg;
        handlers[LNEG] = neg;
        handlers[FNEG] = neg;
        handlers[DNEG] = neg;
        handlers[ISHL] = new OperatorHandler(ShiftLeftArg::new);
        handlers[LSHL] = new OperatorHandler(ShiftLeftArg::new);
        handlers[ISHR] = new OperatorHandler(ShiftRightArg::new);
        handlers[LSHR] = new OperatorHandler(ShiftRightArg::new);
        handlers[IUSHR] = new OperatorHandler(UnsignedShiftRightArg::new);
        handlers[LUSHR] = new OperatorHandler(UnsignedShiftRightArg::new);
        handlers[IAND] = new OperatorHandler(AndArg::new);
        handlers[LAND] = new OperatorHandler(AndArg::new);
        handlers[IOR] = new OperatorHandler(OrArg::new);
        handlers[LOR] = new OperatorHandler(OrArg::new);
        handlers[IXOR] = new OperatorHandler(XorArg::new);
        handlers[LXOR] = new OperatorHandler(XorArg::new);
        handlers[IINC] = (state, next) -> {
            IincInsnNode inc = (IincInsnNode) next;
            Local local = state.getLocal(inc.var);
            IncrementStatement insn = new IncrementStatement(new DummyLocalInstance(local), inc.incr);
            state.append(insn);
        };
        // Casting
        handlers[I2L] = new CastHandler("J");
        handlers[I2F] = new CastHandler("F");
        handlers[I2D] = new CastHandler("D");
        handlers[L2I] = new CastHandler("I");
        handlers[L2F] = new CastHandler("F");
        handlers[L2D] = new CastHandler("D");
        handlers[F2I] = new CastHandler("I");
        handlers[F2L] = new CastHandler("J");
        handlers[F2D] = new CastHandler("D");
        handlers[D2I] = new CastHandler("I");
        handlers[D2L] = new CastHandler("J");
        handlers[D2F] = new CastHandler("F");
        handlers[I2B] = new CastHandler("B");
        handlers[I2C] = new CastHandler("C");
        handlers[I2S] = new CastHandler("S");
        OpHandler compare = (state, next) -> {
            Instruction right = state.pop();
            Instruction left = state.pop();
            state.push(new CompareArg(left, right));
        };
        handlers[LCMP] = compare;
        handlers[FCMPL] = compare;
        handlers[FCMPG] = compare;
        handlers[DCMPL] = compare;
        handlers[DCMPG] = compare;
        // Jumping
        handlers[IFEQ] = null; // handled in the post process
        handlers[IFNE] = null; // --
        handlers[IFGE] = null; // --
        handlers[IFGT] = null; // --
        handlers[IFLT] = null; // --
        handlers[IFLE] = null; // --
        handlers[IF_ACMPEQ] = null; // --
        handlers[IF_ACMPNE] = null; // --
        handlers[IF_ICMPEQ] = null; // --
        handlers[IF_ICMPNE] = null; // --
        handlers[IF_ICMPGE] = null; // --
        handlers[IF_ICMPGT] = null; // --
        handlers[IF_ICMPLE] = null; // --
        handlers[IF_ICMPLT] = null; // --
        handlers[GOTO] = null; // --
        handlers[JSR] = null; // TODO JSR
        handlers[RET] = null; // TODO RET
        handlers[TABLESWITCH] = null; // deferred handling
        handlers[LOOKUPSWITCH] = null; // deferred handling
        // Returns
        OpHandler return_value = (state, next) -> {
            state.append(new ReturnValue(state.pop()));
        };
        handlers[IRETURN] = return_value;
        handlers[LRETURN] = return_value;
        handlers[FRETURN] = return_value;
        handlers[DRETURN] = return_value;
        handlers[ARETURN] = return_value;
        handlers[RETURN] = (state, next) -> {
            state.append(new ReturnVoid());
        };
        // Field handling
        handlers[GETSTATIC] = (state, next) -> {
            FieldInsnNode field = (FieldInsnNode) next;
            String owner = field.owner;
            if (!owner.startsWith("[")) {
                owner = "L" + owner + ";";
            }
            FieldArg arg = new StaticFieldArg(field.name, field.desc, owner);
            state.push(arg);
        };
        handlers[PUTSTATIC] = (state, next) -> {
            FieldInsnNode field = (FieldInsnNode) next;
            Instruction val = state.pop();
            String owner = field.owner;
            if (!owner.startsWith("[")) {
                owner = "L" + owner + ";";
            }
            FieldAssign assign = new StaticFieldAssign(field.name, field.desc, owner, val);
            state.append(assign);
        };
        handlers[GETFIELD] = (state, next) -> {
            FieldInsnNode field = (FieldInsnNode) next;
            String owner = field.owner;
            if (!owner.startsWith("[")) {
                owner = "L" + owner + ";";
            }
            FieldArg arg = new InstanceFieldArg(field.name, field.desc, owner, state.pop());
            state.push(arg);
        };
        handlers[PUTFIELD] = (state, next) -> {
            FieldInsnNode field = (FieldInsnNode) next;
            Instruction val = state.pop();
            Instruction owner = state.pop();
            String owner_t = field.owner;
            if (!owner_t.startsWith("[")) {
                owner_t = "L" + owner_t + ";";
            }
            FieldAssign assign = new InstanceFieldAssign(field.name, field.desc, owner_t, owner, val);
            state.append(assign);
        };
        // Method invocation
        OpHandler method_invoke = (state, next) -> {
            MethodInsnNode method = (MethodInsnNode) next;
            String ret = TypeHelper.getRet(method.desc);
            Instruction[] args = new Instruction[TypeHelper.paramCount(method.desc)];
            for (int i = args.length - 1; i >= 0; i--) {
                args[i] = state.pop();
            }
            Instruction callee = state.pop();
            String owner = method.owner;
            if (!owner.startsWith("[")) {
                owner = "L" + owner + ";";
            }
            if (ret.equals("V")) {
                state.append(new InstanceMethodCall(method.name, method.desc, owner, args, callee));
            } else {
                InstanceFunctionArg arg = new InstanceFunctionArg(method.name, method.desc, owner, args, callee);
                state.push(arg);
            }
        };
        handlers[INVOKEVIRTUAL] = method_invoke;
        handlers[INVOKESPECIAL] = method_invoke;
        handlers[INVOKESTATIC] = (state, next) -> {
            MethodInsnNode method = (MethodInsnNode) next;
            String ret = TypeHelper.getRet(method.desc);
            Instruction[] args = new Instruction[TypeHelper.paramCount(method.desc)];
            for (int i = args.length - 1; i >= 0; i--) {
                args[i] = state.pop();
            }
            String owner = method.owner;
            if (!owner.startsWith("[")) {
                owner = "L" + owner + ";";
            }
            if (ret.equals("V")) {
                state.append(new StaticMethodCall(method.name, method.desc, owner, args));
            } else {
                StaticFunctionArg arg = new StaticFunctionArg(method.name, method.desc, owner, args);
                state.push(arg);
            }
        };
        handlers[INVOKEINTERFACE] = method_invoke;
        handlers[INVOKEDYNAMIC] = null; // TODO INVOKEDYNAMIC
        // Type allocation
        handlers[NEW] = (state, next) -> {
            String type = ((TypeInsnNode) next).desc;
            next = state.next();
            boolean standalone = next.getOpcode() != DUP;
            if (standalone) {
                next = state.next();
            }
            while (true) {
                next = state.next();
                if (next.getOpcode() == INVOKESPECIAL) {
                    MethodInsnNode mth = (MethodInsnNode) next;
                    if ("<init>".equals(mth.name) && type.equals(mth.owner)) {
                        break;
                    }
                }
                state.handleIntermediate(next);
            }
            MethodInsnNode ctor = (MethodInsnNode) next;
            Instruction[] args = new Instruction[TypeHelper.paramCount(ctor.desc)];
            for (int i = 0; i < args.length; i++) {
                args[i] = state.pop();
            }
            if (standalone) {
                NewInstance insn = new NewInstance("L" + type + ";", ctor.desc, args);
                state.append(insn);
            } else {
                NewRefArg arg = new NewRefArg("L" + type + ";", ctor.desc, args);
                state.push(arg);
            }
        };
        OpHandler array_init = (state, next) -> {
            // There are two forms of array initializers:
            //
            // new Type[size]
            //
            // and
            //
            // new Type[]{val1, val2, val3,..., valN}
            //
            // This consumer will handle either of the two form.
            // The first part of the initializer is the same for either form:
            //
            // BIPUSH size
            // NEWARRAY type
            //
            // and then the second form will move to a pattern of
            //
            // DUP
            // BIPUSH index
            // [...] create value
            // IASTORE
            //
            // Which it will repeat until it finishes and moves into the logic
            // which
            // consumes the array
            Instruction size = state.pop();
            int tstore = -1;
            String array_type = null;
            if (next instanceof IntInsnNode) {
                IntInsnNode array = (IntInsnNode) next;
                tstore = AstUtil.opcodeToStore(array.operand);
                array_type = AstUtil.opcodeToType(array.operand);
            } else if (next instanceof TypeInsnNode) {
                TypeInsnNode array = (TypeInsnNode) next;
                tstore = AASTORE;
                array_type = array.desc;
            }
            final int store = tstore;
            next = state.next();
            Instruction[] init = null;
            // TODO
            // This makes an assumption that the array is always stored to
            // value,
            // which is obviously completely wrong.
            // We should optimistically loop for an initializer pattern and
            // otherwise just revert and continue on passing the array on the
            // stack
            // to the next opcode in the original base consumer.
            if (next.getOpcode() == DUP && size instanceof IntConstantArg) {
                init = new Instruction[((IntConstantArg) size).getConstant()];
                while (next.getOpcode() == DUP) {
                    next = state.next();
                    int index = -1;
                    // TODO move this conversion to a utility
                    if (next.getOpcode() == ICONST_0) {
                        index = 0;
                    } else if (next.getOpcode() == ICONST_1) {
                        index = 1;
                    } else if (next.getOpcode() == ICONST_2) {
                        index = 2;
                    } else if (next.getOpcode() == ICONST_3) {
                        index = 3;
                    } else if (next.getOpcode() == ICONST_4) {
                        index = 4;
                    } else if (next.getOpcode() == ICONST_5) {
                        index = 5;
                    } else if (next.getOpcode() == BIPUSH) {
                        index = ((IntInsnNode) next).operand;
                    } else if (next.getOpcode() == SIPUSH) {
                        index = ((IntInsnNode) next).operand;
                    } else {
                        throw new RuntimeException("Unknown array index: " + AstUtil.insnToString(next));
                    }
                    next = state.next();
                    while (next.getOpcode() != store) {
                        state.handleIntermediate(next);
                        next = state.next();
                    }
                    Instruction val = state.pop();
                    init[index] = val;
                    next = state.next();
                }
            } else {
                state.revert(1);
            }
            NewArrayArg arg = new NewArrayArg(array_type, size, init);
            state.push(arg);
        };
        handlers[NEWARRAY] = array_init;
        handlers[ANEWARRAY] = array_init;
        // Misc
        handlers[ARRAYLENGTH] = (state, next) -> {
            FieldArg arg = new InstanceFieldArg("length", "I", "hidden-array-field", state.pop());
            state.push(arg);
        };
        handlers[ATHROW] = (state, next) -> {
            state.append(new ThrowException(state.pop()));
        };
        handlers[CHECKCAST] = (state, next) -> {
            TypeInsnNode cast = (TypeInsnNode) next;
            String desc = cast.desc;
            // Non-array types are specified as internal names rather than
            // descriptions
            if (!desc.startsWith("[")) {
                desc = "L" + desc + ";";
            }
            state.push(new CastArg(desc, state.pop()));
        };
        handlers[INSTANCEOF] = (state, next) -> {
            TypeInsnNode insn = (TypeInsnNode) next;
            Instruction val = state.pop();
            String type = insn.desc;
            if (!type.startsWith("[")) {
                type = "L" + insn.desc + ";";
            }
            state.push(new InstanceOfArg(val, type));
        };
        handlers[MONITORENTER] = noop;
        handlers[MONITOREXIT] = noop;
        handlers[MULTIANEWARRAY] = null; // TODO MULTIANEWARRAY
        handlers[IFNULL] = null; // handled in the post process
        handlers[IFNONNULL] = null; // --
    }

    /**
     * A handler for binary operators.
     */
    static class OperatorHandler implements OpHandler {

        private final BiFunction<Instruction, Instruction, OperatorArg> ctor;

        public OperatorHandler(BiFunction<Instruction, Instruction, OperatorArg> ctor) {
            this.ctor = ctor;
        }

        @Override
        public void handle(OldOpcodeDecompiler state, AbstractInsnNode next) {
            Instruction right = state.pop();
            Instruction left = state.pop();
            OperatorArg arg = this.ctor.apply(left, right);
            state.push(arg);
        }
    }

    /**
     * A handler for CHECKCAST opcodes.
     */
    static class CastHandler implements OpHandler {

        private final String type;

        public CastHandler(String t) {
            this.type = t;
        }

        @Override
        public void handle(OldOpcodeDecompiler state, AbstractInsnNode next) {
            Instruction val = state.pop();
            CastArg arg = new CastArg(this.type, val);
            state.push(arg);
        }
    }

}
