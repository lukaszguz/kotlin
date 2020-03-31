/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.Printer
import java.util.*
import kotlin.NoSuchElementException

class FirControlFlowGraphRenderVisitor(
    builder: StringBuilder,
) : FirVisitorVoid() {
    companion object {
        private const val EDGE = " -> "
        private const val RED = "red"
        private const val BLUE = "blue"

        private val EDGE_STYLE = EnumMap(
            mapOf(
                EdgeKind.Simple to "",
                EdgeKind.Dead to "[style=dotted]",
                EdgeKind.Cfg to "[color=green]",
                EdgeKind.Dfg to "[color=red]",
            )
        )
    }

    private val printer = Printer(builder)

    private var indexOffset = 0
    private var nodeCounter = 0
    private var clusterCounter = 0
    private val indices = mutableMapOf<CFGNode<*>, Int>()

    private val topLevelGraphs = mutableSetOf<ControlFlowGraph>()
    private val allGraphs = mutableSetOf<ControlFlowGraph>()

    override fun visitFile(file: FirFile) {
        printer
            .println("digraph ${file.name.replace(".", "_")} {")
            .pushIndent()
            .println("graph [nodesep=3]")
            .println("node [shape=box penwidth=2]")
            .println("edge [penwidth=2]")
            .println()
        visitElement(file)

        for (topLevelGraph in topLevelGraphs) {
            printer.renderNodes(topLevelGraph)
            printer.renderEdges(topLevelGraph)
            printer.println()
        }

        printer
            .popIndent()
            .println("}")
    }

    private fun ControlFlowGraph.collectNodes() {
        for (node in nodes) {
            if (node in indices) {
                val x = 1
            }
            indices[node] = nodeCounter++
        }
    }

    private fun Printer.renderNodes(graph: ControlFlowGraph) {
        var color = RED
        val sortedNodes = graph.sortedNodes()
        for (node in sortedNodes) {
            if (node is EnterNodeMarker) {
                enterCluster(color)
                color = BLUE
            }
            val attributes = mutableListOf<String>()
            attributes += "label=\"${node.render().replace("\"", "")}\""

            fun fillColor(color: String) {
                attributes += "style=\"filled\""
                attributes += "fillcolor=$color"
            }

            if (node == node.owner.enterNode || node == node.owner.exitNode) {
                fillColor("red")
            }
            if (node.isDead) {
                fillColor("gray")
            } else if (node is UnionFunctionCallArgumentsNode) {
                fillColor("yellow")
            }
            try {
                println(indices.getValue(node), attributes.joinToString(separator = " ", prefix = " [", postfix = "];"))
            } catch (e: NoSuchElementException) {
                throw e
            }
            if (node is ExitNodeMarker) {
                exitCluster()
            }
        }
    }

    private fun Printer.renderEdges(graph: ControlFlowGraph) {
        for (node in graph.nodes) {
            if (node.followingNodes.isEmpty()) continue

            fun renderEdges(kind: EdgeKind) {
                val edges = node.followingNodes.filter { node.outgoingEdges.getValue(it) == kind }
                if (edges.isEmpty()) return
                print(
                    indices.getValue(node),
                    EDGE,
                    edges.joinToString(prefix = "{", postfix = "}", separator = " ") { indices.getValue(it).toString() }
                )
                EDGE_STYLE.getValue(kind).takeIf { it.isNotBlank() }?.let { printWithNoIndent(" $it") }
                printlnWithNoIndent(";")
            }

            for (kind in EdgeKind.values()) {
                renderEdges(kind)
            }
        }
        for (subGraph in graph.subGraphs) {
            renderEdges(subGraph)
        }
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
        val controlFlowGraph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
        if (controlFlowGraph in allGraphs) {
            val x = 1
        }
        controlFlowGraph.collectNodes()
        if (controlFlowGraph.owner == null) {
            topLevelGraphs += controlFlowGraph
        }
        allGraphs += controlFlowGraph
    }

    private fun Printer.enterCluster(color: String) {
        println("subgraph cluster_${clusterCounter++} {")
        pushIndent()
        println("color=$color")
    }

    private fun Printer.exitCluster() {
        popIndent()
        println("}")
    }
}

private object CfgRenderMode : FirRenderer.RenderMode(renderLambdaBodies = false, renderCallArguments = false)

private fun CFGNode<*>.render(): String =
    buildString {
        append(
            when (this@render) {
                is FunctionEnterNode -> "Enter function \"${fir.name()}\""
                is FunctionExitNode -> "Exit function \"${fir.name()}\""

                is BlockEnterNode -> "Enter block"
                is BlockExitNode -> "Exit block"

                is WhenEnterNode -> "Enter when"
                is WhenBranchConditionEnterNode -> "Enter when branch condition ${if (fir.condition is FirElseIfTrueCondition) "\"else\"" else ""}"
                is WhenBranchConditionExitNode -> "Exit when branch condition"
                is WhenBranchResultEnterNode -> "Enter when branch result"
                is WhenBranchResultExitNode -> "Exit when branch result"
                is WhenSyntheticElseBranchNode -> "Synthetic else branch"
                is WhenExitNode -> "Exit when"

                is LoopEnterNode -> "Enter ${fir.type()} loop"
                is LoopBlockEnterNode -> "Enter loop block"
                is LoopBlockExitNode -> "Exit loop block"
                is LoopConditionEnterNode -> "Enter loop condition"
                is LoopConditionExitNode -> "Exit loop condition"
                is LoopExitNode -> "Exit ${fir.type()}loop"

                is QualifiedAccessNode -> "Access variable ${fir.calleeReference.render(CfgRenderMode)}"
                is ResolvedQualifierNode -> "Access qualifier ${fir.classId}"
                is OperatorCallNode -> "Operator ${fir.operation.operator}"
                is ComparisonExpressionNode -> "Comparison ${fir.operation.operator}"
                is TypeOperatorCallNode -> "Type operator: \"${fir.render(CfgRenderMode)}\""
                is JumpNode -> "Jump: ${fir.render()}"
                is StubNode -> "Stub"
                is CheckNotNullCallNode -> "Check not null: ${fir.render(CfgRenderMode)}"

                is ConstExpressionNode -> "Const: ${fir.render()}"
                is VariableDeclarationNode ->
                    "Variable declaration: ${buildString {
                        FirRenderer(
                            this,
                            CfgRenderMode
                        ).visitCallableDeclaration(fir)
                    }}"

                is VariableAssignmentNode -> "Assignmenet: ${fir.lValue.render(CfgRenderMode)}"
                is FunctionCallNode -> "Function call: ${fir.render(CfgRenderMode)}"
                is DelegatedConstructorCallNode -> "Delegated constructor call: ${fir.render(CfgRenderMode)}"
                is ThrowExceptionNode -> "Throw: ${fir.render(CfgRenderMode)}"

                is TryExpressionEnterNode -> "Try expression enter"
                is TryMainBlockEnterNode -> "Try main block enter"
                is TryMainBlockExitNode -> "Try main block exit"
                is CatchClauseEnterNode -> "Catch enter"
                is CatchClauseExitNode -> "Catch exit"
                is FinallyBlockEnterNode -> "Enter finally"
                is FinallyBlockExitNode -> "Exit finally"
                is FinallyProxyEnterNode -> TODO()
                is FinallyProxyExitNode -> TODO()
                is TryExpressionExitNode -> "Try expression exit"

                is BinaryAndEnterNode -> "Enter &&"
                is BinaryAndExitLeftOperandNode -> "Exit left part of &&"
                is BinaryAndEnterRightOperandNode -> "Enter right part of &&"
                is BinaryAndExitNode -> "Exit &&"
                is BinaryOrEnterNode -> "Enter ||"
                is BinaryOrExitLeftOperandNode -> "Exit left part of ||"
                is BinaryOrEnterRightOperandNode -> "Enter right part of ||"
                is BinaryOrExitNode -> "Exit ||"

                is PropertyInitializerEnterNode -> "Enter property"
                is PropertyInitializerExitNode -> "Exit property"
                is InitBlockEnterNode -> "Enter init block"
                is InitBlockExitNode -> "Exit init block"
                is AnnotationEnterNode -> "Enter annotation"
                is AnnotationExitNode -> "Exit annotation"

                is EnterContractNode -> "Enter contract"
                is ExitContractNode -> "Exit contract"

                is EnterSafeCallNode -> "Enter safe call"
                is ExitSafeCallNode -> "Exit safe call"

                is PostponedLambdaEnterNode -> "Postponed enter to lambda"
                is PostponedLambdaExitNode -> "Postponed exit from lambda"

                is UnionFunctionCallArgumentsNode -> "Call arguments union"

                is ClassEnterNode -> "Enter class ${owner.name}"
                is ClassExitNode -> "Exit class ${owner.name}"
                is LocalClassExitNode -> "Exit local class ${owner.name}"
                is AnonymousObjectExitNode -> "Exit anonymous object"

                is AbstractBinaryExitNode -> throw IllegalStateException()
            },
        )
    }

private fun FirFunction<*>.name(): String = when (this) {
    is FirSimpleFunction -> name.asString()
    is FirAnonymousFunction -> "anonymousFunction"
    is FirConstructor -> "<init>"
    is FirPropertyAccessor -> if (isGetter) "getter" else "setter"
    is FirErrorFunction -> "errorFunction"
    else -> TODO(toString())
}

private fun FirLoop.type(): String = when (this) {
    is FirWhileLoop -> "while"
    is FirDoWhileLoop -> "do-while"
    else -> throw IllegalArgumentException()
}

private fun ControlFlowGraph.sortedNodes(): List<CFGNode<*>> {
    val nodesToSort = nodes.filterTo(mutableListOf()) { it != enterNode }

    forEachSubGraph {
        nodesToSort += it.nodes
    }

    val topologicalOrder = DFS.topologicalOrder(nodesToSort) {
        val result = if (it !is WhenBranchConditionExitNode || it.followingNodes.size < 2) {
            it.followingNodes
        } else {
            it.followingNodes.sortedBy { node -> if (node is BlockEnterNode) 1 else 0 }
        }
        result
    }
    return listOf(enterNode) + topologicalOrder
}

private fun ControlFlowGraph.forEachSubGraph(block: (ControlFlowGraph) -> Unit) {
    for (subGraph in subGraphs) {
        block(subGraph)
        subGraph.forEachSubGraph(block)
    }
}