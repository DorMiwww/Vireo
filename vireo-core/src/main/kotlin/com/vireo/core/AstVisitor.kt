package com.vireo.core

interface AstVisitor<T> {
    fun visitFile(node: VireoFile): T
    fun visitBlock(node: Block): T
    fun visitComponent(node: ComponentNode): T
    fun visitProperty(node: Property): T
    fun visitConstraint(node: Constraint): T
    fun visitVarDeclaration(node: VarDeclaration): T
    fun visitFunDeclaration(node: FunDeclaration): T
}
