package com.vireo.analysis

import com.vireo.core.*

object ExprEvaluator {

    fun evaluate(file: VireoFile): VireoResult<VireoFile> {
        val evaluator = ExprEvaluatorEngine(file)
        return evaluator.evaluateFile()
    }
}

internal sealed class EvalValue {
    data class NumberVal(val value: Double) : EvalValue()
    data class StringVal(val value: String) : EvalValue()
    data class ColorVal(val hex: String) : EvalValue()
    data class BoolVal(val value: Boolean) : EvalValue()
    data class RelationalVal(val expression: String) : EvalValue()

    fun toPropertyValue(location: SourceLocation): PropertyValue {
        return when (this) {
            is RelationalVal -> PropertyValue.Expr(expression, location)
            else -> PropertyValue.Literal(toLiteralValue(), location)
        }
    }

    fun toLiteralValue(): Any {
        return when (this) {
            is NumberVal -> {
                if (value == value.toLong().toDouble()) {
                    val longVal = value.toLong()
                    if (longVal in Int.MIN_VALUE..Int.MAX_VALUE) longVal.toInt() else longVal
                } else {
                    value
                }
            }
            is StringVal -> value
            is ColorVal -> hex
            is BoolVal -> value
            is RelationalVal -> expression
        }
    }

    fun toDisplayString(): String = when (this) {
        is NumberVal -> {
            if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                value.toString()
            }
        }
        is StringVal -> value
        is ColorVal -> hex
        is BoolVal -> value.toString()
        is RelationalVal -> expression
    }
}

private class ExprEvaluatorEngine(
    private val file: VireoFile
) {
    private val errors = mutableListOf<VireoError>()
    private val varScope = mutableMapOf<String, EvalValue>()
    private val funScope = mutableMapOf<String, FunDeclaration>()

    fun evaluateFile(): VireoResult<VireoFile> {
        // 1. Register top-level functions
        for (f in file.functions) {
            funScope[f.name] = f
        }

        // 2. Evaluate top-level vars in order
        val evaluatedVars = mutableListOf<VarDeclaration>()
        for (v in file.vars) {
            val evalRes = evaluatePropertyValue(v.value, emptyMap(), v.location)
            if (evalRes != null) {
                varScope[v.name] = evalRes
                evaluatedVars.add(v.copy(value = evalRes.toPropertyValue(v.location)))
            }
        }

        // 3. Evaluate blocks
        val evaluatedBlocks = file.blocks.map { evaluateBlock(it) }

        return if (errors.isNotEmpty()) {
            VireoResult.Err(errors)
        } else {
            VireoResult.Ok(
                file.copy(
                    vars = evaluatedVars,
                    blocks = evaluatedBlocks
                )
            )
        }
    }

    private fun evaluateBlock(block: Block): Block {
        return block.copy(components = block.components.map { evaluateComponent(it) })
    }

    private fun evaluateComponent(comp: ComponentNode): ComponentNode {
        val evaluatedProps = comp.properties.map { prop ->
            val evalRes = evaluatePropertyValue(prop.value, emptyMap(), prop.location)
            if (evalRes != null) {
                prop.copy(value = evalRes.toPropertyValue(prop.location))
            } else {
                prop
            }
        }

        val evaluatedConstraints = comp.constraints.map { constraint ->
            if (constraint is Constraint.Relational) {
                val evalRes = evaluateExpressionString(constraint.expression, emptyMap(), constraint.location)
                if (evalRes != null) {
                    constraint.copy(expression = evalRes.toDisplayString())
                } else {
                    constraint
                }
            } else {
                constraint
            }
        }

        val evaluatedChildren = comp.children.map { evaluateComponent(it) }

        return comp.copy(
            properties = evaluatedProps,
            constraints = evaluatedConstraints,
            children = evaluatedChildren
        )
    }

    private fun evaluatePropertyValue(
        pv: PropertyValue,
        localScope: Map<String, EvalValue>,
        fallbackLoc: SourceLocation
    ): EvalValue? {
        return when (pv) {
            is PropertyValue.Literal -> {
                val raw = pv.value
                if (raw is String) {
                    if (raw.startsWith("#") && (raw.length == 4 || raw.length == 7 || raw.length == 9) && raw.substring(1).all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                        EvalValue.ColorVal(raw)
                    } else if (raw.contains("$")) {
                        interpolateString(raw, localScope, pv.location)
                    } else {
                        EvalValue.StringVal(raw)
                    }
                } else if (raw is Number) {
                    EvalValue.NumberVal(raw.toDouble())
                } else if (raw is Boolean) {
                    EvalValue.BoolVal(raw)
                } else {
                    EvalValue.StringVal(raw.toString())
                }
            }
            is PropertyValue.Ref -> null
            is PropertyValue.Expr -> {
                evaluateExpressionString(pv.source, localScope, pv.location)
            }
            is PropertyValue.ConditionalExpr -> {
                val condVal = evaluatePropertyValue(pv.condition, localScope, pv.location) ?: return null
                val isTrue = when (condVal) {
                    is EvalValue.BoolVal -> condVal.value
                    is EvalValue.StringVal -> condVal.value.isNotEmpty() && condVal.value != "false"
                    is EvalValue.NumberVal -> condVal.value != 0.0
                    else -> true
                }
                val branch = if (isTrue) pv.thenBranch else pv.elseBranch
                evaluatePropertyValue(branch, localScope, pv.location)
            }
        }
    }

    private fun interpolateString(
        raw: String,
        localScope: Map<String, EvalValue>,
        location: SourceLocation
    ): EvalValue? {
        val result = StringBuilder()
        var i = 0
        while (i < raw.length) {
            if (raw[i] == '\\' && i + 1 < raw.length && raw[i + 1] == '$') {
                result.append('$')
                i += 2
                continue
            }
            if (raw[i] == '$') {
                if (i + 1 < raw.length && raw[i + 1] == '{') {
                    val closing = raw.indexOf('}', i + 2)
                    if (closing != -1) {
                        val subExpr = raw.substring(i + 2, closing)
                        val evalSub = evaluateExpressionString(subExpr, localScope, location) ?: return null
                        result.append(evalSub.toDisplayString())
                        i = closing + 1
                        continue
                    }
                }
                var j = i + 1
                if (j < raw.length && (raw[j].isLetter() || raw[j] == '_')) {
                    j++
                    while (j < raw.length && (raw[j].isLetterOrDigit() || raw[j] == '_')) {
                        j++
                    }
                    val varName = raw.substring(i + 1, j)
                    val valInScope = localScope[varName] ?: varScope[varName]
                    if (valInScope == null) {
                        errors.add(VireoError("Undefined variable '\$$varName' in string interpolation", location))
                        return null
                    }
                    result.append(valInScope.toDisplayString())
                    i = j
                    continue
                }
            }
            result.append(raw[i])
            i++
        }
        return EvalValue.StringVal(result.toString())
    }

    private fun evaluateExpressionString(
        source: String,
        localScope: Map<String, EvalValue>,
        location: SourceLocation
    ): EvalValue? {
        val trimmed = source.trim()
        if (trimmed.isEmpty()) {
            return EvalValue.StringVal("")
        }

        if (trimmed.endsWith("%parent") || trimmed == "fill" || trimmed == "hug") {
            return EvalValue.RelationalVal(trimmed)
        }

        val tokens = ExpressionTokenizer(trimmed, location, errors).tokenize() ?: return null

        val parser = ExpressionParser(tokens, varScope, funScope, localScope, location, errors, ::evaluatePropertyValue)
        return parser.parse()
    }
}

private enum class TokenType {
    NUMBER, STRING, HEX_COLOR, VAR, IDENT,
    OP_PLUS, OP_MINUS, OP_MUL, OP_DIV, OP_MOD, OP_EQ, OP_NEQ,
    LPAREN, RPAREN, COMMA, KW_IF, KW_THEN, KW_ELSE
}

private data class ExprToken(
    val type: TokenType,
    val text: String,
    val location: SourceLocation
)

private class ExpressionTokenizer(
    private val source: String,
    private val location: SourceLocation,
    private val errors: MutableList<VireoError>
) {
    private var i = 0

    fun tokenize(): List<ExprToken>? {
        val tokens = mutableListOf<ExprToken>()
        while (i < source.length) {
            val ch = source[i]
            if (ch.isWhitespace()) {
                i++
                continue
            }
            if (ch == '#') {
                val start = i
                i++
                while (i < source.length && (source[i].isDigit() || source[i] in 'a'..'f' || source[i] in 'A'..'F')) {
                    i++
                }
                tokens.add(ExprToken(TokenType.HEX_COLOR, source.substring(start, i), location))
                continue
            }
            if (ch == '$') {
                i++
                val start = i
                while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) {
                    i++
                }
                val varName = source.substring(start, i)
                if (varName.isEmpty()) {
                    errors.add(VireoError("Expected variable name after '$'", location))
                    return null
                }
                tokens.add(ExprToken(TokenType.VAR, varName, location))
                continue
            }
            if (ch == '"') {
                i++
                val start = i
                while (i < source.length && source[i] != '"') {
                    if (source[i] == '\\' && i + 1 < source.length) i++
                    i++
                }
                if (i >= source.length) {
                    errors.add(VireoError("Unterminated string literal in expression", location))
                    return null
                }
                val strContent = source.substring(start, i)
                i++
                tokens.add(ExprToken(TokenType.STRING, strContent, location))
                continue
            }
            if (ch.isDigit()) {
                val start = i
                while (i < source.length && (source[i].isDigit() || source[i] == '.')) {
                    i++
                }
                if (i < source.length && source.substring(i).startsWith("%parent")) {
                    i += 7
                    val relText = source.substring(start, i)
                    tokens.add(ExprToken(TokenType.IDENT, relText, location))
                    continue
                }
                tokens.add(ExprToken(TokenType.NUMBER, source.substring(start, i), location))
                continue
            }
            if (ch.isLetter() || ch == '_') {
                val start = i
                while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_' || source[i] == '.')) {
                    i++
                }
                val word = source.substring(start, i)
                val tokenType = when (word) {
                    "if" -> TokenType.KW_IF
                    "then" -> TokenType.KW_THEN
                    "else" -> TokenType.KW_ELSE
                    else -> TokenType.IDENT
                }
                tokens.add(ExprToken(tokenType, word, location))
                continue
            }
            if (ch == '=' && i + 1 < source.length && source[i + 1] == '=') {
                tokens.add(ExprToken(TokenType.OP_EQ, "==", location))
                i += 2
                continue
            }
            if (ch == '!' && i + 1 < source.length && source[i + 1] == '=') {
                tokens.add(ExprToken(TokenType.OP_NEQ, "!=", location))
                i += 2
                continue
            }
            when (ch) {
                '+' -> { tokens.add(ExprToken(TokenType.OP_PLUS, "+", location)); i++ }
                '-' -> { tokens.add(ExprToken(TokenType.OP_MINUS, "-", location)); i++ }
                '*' -> { tokens.add(ExprToken(TokenType.OP_MUL, "*", location)); i++ }
                '/' -> { tokens.add(ExprToken(TokenType.OP_DIV, "/", location)); i++ }
                '%' -> { tokens.add(ExprToken(TokenType.OP_MOD, "%", location)); i++ }
                '(' -> { tokens.add(ExprToken(TokenType.LPAREN, "(", location)); i++ }
                ')' -> { tokens.add(ExprToken(TokenType.RPAREN, ")", location)); i++ }
                ',' -> { tokens.add(ExprToken(TokenType.COMMA, ",", location)); i++ }
                else -> {
                    errors.add(VireoError("Unexpected character '$ch' in expression '$source'", location))
                    return null
                }
            }
        }
        return tokens
    }
}

private class ExpressionParser(
    private val tokens: List<ExprToken>,
    private val varScope: Map<String, EvalValue>,
    private val funScope: Map<String, FunDeclaration>,
    private val localScope: Map<String, EvalValue>,
    private val location: SourceLocation,
    private val errors: MutableList<VireoError>,
    private val evalPropertyValue: (PropertyValue, Map<String, EvalValue>, SourceLocation) -> EvalValue?
) {
    private var curr = 0

    fun parse(): EvalValue? {
        if (tokens.isEmpty()) return EvalValue.StringVal("")
        val res = parseExpression() ?: return null
        return res
    }

    private fun parseExpression(): EvalValue? {
        if (match(TokenType.KW_IF)) {
            val cond = parseExpression() ?: return null
            if (!match(TokenType.KW_THEN)) {
                errors.add(VireoError("Expected 'then' after condition in conditional expression", location))
                return null
            }
            val thenBranch = parseExpression() ?: return null
            if (!match(TokenType.KW_ELSE)) {
                errors.add(VireoError("Expected 'else' in conditional expression", location))
                return null
            }
            val elseBranch = parseExpression() ?: return null

            val isTrue = when (cond) {
                is EvalValue.BoolVal -> cond.value
                is EvalValue.StringVal -> cond.value.isNotEmpty() && cond.value != "false"
                is EvalValue.NumberVal -> cond.value != 0.0
                else -> true
            }
            return if (isTrue) thenBranch else elseBranch
        }
        return parseEquality()
    }

    private fun parseEquality(): EvalValue? {
        var left = parseAdditive() ?: return null

        while (match(TokenType.OP_EQ, TokenType.OP_NEQ)) {
            val op = previous().type
            val right = parseAdditive() ?: return null

            val isEqual = compareValues(left, right)
            left = EvalValue.BoolVal(if (op == TokenType.OP_EQ) isEqual else !isEqual)
        }
        return left
    }

    private fun compareValues(left: EvalValue, right: EvalValue): Boolean {
        if (left is EvalValue.NumberVal && right is EvalValue.NumberVal) {
            return left.value == right.value
        }
        if (left is EvalValue.BoolVal && right is EvalValue.BoolVal) {
            return left.value == right.value
        }
        return left.toDisplayString() == right.toDisplayString()
    }

    private fun parseAdditive(): EvalValue? {
        var left = parseMultiplicative() ?: return null

        while (match(TokenType.OP_PLUS, TokenType.OP_MINUS)) {
            val op = previous().type
            val right = parseMultiplicative() ?: return null

            if (left is EvalValue.RelationalVal || right is EvalValue.RelationalVal) {
                val opStr = if (op == TokenType.OP_PLUS) "+" else "-"
                left = EvalValue.RelationalVal("${left.toDisplayString()} $opStr ${right.toDisplayString()}")
            } else if (left is EvalValue.NumberVal && right is EvalValue.NumberVal) {
                val res = if (op == TokenType.OP_PLUS) left.value + right.value else left.value - right.value
                left = EvalValue.NumberVal(res)
            } else if (left is EvalValue.StringVal || right is EvalValue.StringVal) {
                if (op == TokenType.OP_PLUS) {
                    left = EvalValue.StringVal(left.toDisplayString() + right.toDisplayString())
                } else {
                    errors.add(VireoError("Cannot subtract string values", location))
                    return null
                }
            } else {
                val opStr = if (op == TokenType.OP_PLUS) "+" else "-"
                left = EvalValue.RelationalVal("${left.toDisplayString()} $opStr ${right.toDisplayString()}")
            }
        }
        return left
    }

    private fun parseMultiplicative(): EvalValue? {
        var left = parsePrimary() ?: return null

        while (match(TokenType.OP_MUL, TokenType.OP_DIV, TokenType.OP_MOD)) {
            val op = previous().type
            val right = parsePrimary() ?: return null

            if (left is EvalValue.NumberVal && right is EvalValue.NumberVal) {
                val res = when (op) {
                    TokenType.OP_MUL -> left.value * right.value
                    TokenType.OP_DIV -> {
                        if (right.value == 0.0) {
                            errors.add(VireoError("Division by zero in expression", location))
                            return null
                        }
                        left.value / right.value
                    }
                    TokenType.OP_MOD -> left.value % right.value
                    else -> 0.0
                }
                left = EvalValue.NumberVal(res)
            } else {
                errors.add(VireoError("Arithmetic operators require numeric operands", location))
                return null
            }
        }
        return left
    }

    private fun parsePrimary(): EvalValue? {
        if (match(TokenType.OP_MINUS)) {
            val expr = parsePrimary() ?: return null
            return if (expr is EvalValue.NumberVal) {
                EvalValue.NumberVal(-expr.value)
            } else {
                errors.add(VireoError("Unary minus expects numeric value", location))
                null
            }
        }
        if (match(TokenType.NUMBER)) {
            val numDouble = previous().text.toDoubleOrNull() ?: 0.0
            return EvalValue.NumberVal(numDouble)
        }
        if (match(TokenType.STRING)) {
            val strVal = previous().text
            if (strVal.contains("$")) {
                return interpolateStringInParser(strVal)
            }
            return EvalValue.StringVal(strVal)
        }
        if (match(TokenType.HEX_COLOR)) {
            return EvalValue.ColorVal(previous().text)
        }
        if (match(TokenType.VAR)) {
            val varName = previous().text
            val valInScope = localScope[varName] ?: varScope[varName]
            if (valInScope == null) {
                errors.add(VireoError("Undefined variable '\$$varName'", location))
                return null
            }
            return valInScope
        }
        if (match(TokenType.IDENT)) {
            val identName = previous().text
            if (match(TokenType.LPAREN)) {
                val args = mutableListOf<EvalValue>()
                if (!check(TokenType.RPAREN)) {
                    do {
                        val arg = parseExpression() ?: return null
                        args.add(arg)
                    } while (match(TokenType.COMMA))
                }
                if (!match(TokenType.RPAREN)) {
                    errors.add(VireoError("Expected ')' after function call arguments for '$identName'", location))
                    return null
                }

                val funDecl = funScope[identName]
                if (funDecl == null) {
                    errors.add(VireoError("Undefined function '$identName'", location))
                    return null
                }

                if (funDecl.parameters.size != args.size) {
                    errors.add(
                        VireoError(
                            "Function '$identName' expects ${funDecl.parameters.size} arguments, but got ${args.size}",
                            location
                        )
                    )
                    return null
                }

                val funScopeMap = mutableMapOf<String, EvalValue>()
                funDecl.parameters.forEachIndexed { idx, param ->
                    funScopeMap[param.name] = args[idx]
                }

                return evalPropertyValue(funDecl.returnExpr, funScopeMap, funDecl.location)
            }

            val valInScope = localScope[identName] ?: varScope[identName]
            if (valInScope != null) {
                return valInScope
            }
            if (identName == "true") return EvalValue.BoolVal(true)
            if (identName == "false") return EvalValue.BoolVal(false)
            if (identName.startsWith("parent") || identName.endsWith("%parent")) {
                return EvalValue.RelationalVal(identName)
            }

            errors.add(VireoError("Undefined identifier or variable '$identName'", location))
            return null
        }
        if (match(TokenType.LPAREN)) {
            val expr = parseExpression() ?: return null
            if (!match(TokenType.RPAREN)) {
                errors.add(VireoError("Expected ')' after parenthesized expression", location))
                return null
            }
            return expr
        }

        val tok = if (isAtEnd()) "EOF" else peek().text
        errors.add(VireoError("Unexpected token '$tok' in expression", location))
        return null
    }

    private fun interpolateStringInParser(raw: String): EvalValue? {
        val result = StringBuilder()
        var i = 0
        while (i < raw.length) {
            if (raw[i] == '\\' && i + 1 < raw.length && raw[i + 1] == '$') {
                result.append('$')
                i += 2
                continue
            }
            if (raw[i] == '$') {
                if (i + 1 < raw.length && raw[i + 1] == '{') {
                    val closing = raw.indexOf('}', i + 2)
                    if (closing != -1) {
                        val subExpr = raw.substring(i + 2, closing)
                        val subTokens = ExpressionTokenizer(subExpr, location, errors).tokenize() ?: return null
                        val subParser = ExpressionParser(subTokens, varScope, funScope, localScope, location, errors, evalPropertyValue)
                        val evalSub = subParser.parse() ?: return null
                        result.append(evalSub.toDisplayString())
                        i = closing + 1
                        continue
                    }
                }
                var j = i + 1
                if (j < raw.length && (raw[j].isLetter() || raw[j] == '_')) {
                    j++
                    while (j < raw.length && (raw[j].isLetterOrDigit() || raw[j] == '_')) {
                        j++
                    }
                    val varName = raw.substring(i + 1, j)
                    val valInScope = localScope[varName] ?: varScope[varName]
                    if (valInScope == null) {
                        errors.add(VireoError("Undefined variable '\$$varName' in string interpolation", location))
                        return null
                    }
                    result.append(valInScope.toDisplayString())
                    i = j
                    continue
                }
            }
            result.append(raw[i])
            i++
        }
        return EvalValue.StringVal(result.toString())
    }

    private fun match(vararg types: TokenType): Boolean {
        for (t in types) {
            if (check(t)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(t: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == t
    }

    private fun advance(): ExprToken {
        if (!isAtEnd()) curr++
        return previous()
    }

    private fun isAtEnd(): Boolean = curr >= tokens.size
    private fun peek(): ExprToken = tokens[curr]
    private fun previous(): ExprToken = tokens[curr - 1]
}
