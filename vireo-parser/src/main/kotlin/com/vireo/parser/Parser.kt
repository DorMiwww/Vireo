package com.vireo.parser

import com.vireo.core.*
import com.vireo.lexer.*

object Parser {

    fun parse(tokens: List<Token>, filePath: String = "<anonymous>"): VireoResult<VireoFile> {
        val parser = FileParser(tokens, filePath)
        return parser.parseFile()
    }

    fun parse(source: String, filePath: String = "<anonymous>"): VireoResult<VireoFile> {
        return when (val lexResult = Lexer.tokenize(source, filePath)) {
            is VireoResult.Err -> VireoResult.Err(lexResult.errors)
            is VireoResult.Ok -> parse(lexResult.value, filePath)
        }
    }

    private class FileParser(
        private val tokens: List<Token>,
        private val filePath: String
    ) {
        private var current = 0
        private val errors = mutableListOf<VireoError>()

        fun parseFile(): VireoResult<VireoFile> {
            val fileLocation = if (tokens.isNotEmpty()) tokens.first().location else SourceLocation(filePath, 1, 1)
            val imports = mutableListOf<Import>()
            val vars = mutableListOf<VarDeclaration>()
            val functions = mutableListOf<FunDeclaration>()
            val blocks = mutableListOf<Block>()

            while (!isAtEnd()) {
                when {
                    check(TokenType.KEYWORD_IMPORT) -> {
                        val importNode = parseImport()
                        if (importNode != null) {
                            imports.add(importNode)
                        }
                    }
                    check(TokenType.KEYWORD_VAR) -> {
                        val varNode = parseVarDeclaration()
                        if (varNode != null) {
                            vars.add(varNode)
                        }
                    }
                    check(TokenType.KEYWORD_FUN) -> {
                        val funNode = parseFunDeclaration()
                        if (funNode != null) {
                            functions.add(funNode)
                        }
                    }
                    check(TokenType.KEYWORD_BLOCK) -> {
                        val blockNode = parseBlock()
                        if (blockNode != null) {
                            blocks.add(blockNode)
                        }
                    }
                    check(TokenType.EOF) -> break
                    else -> {
                        val tok = peek()
                        errors.add(VireoError("Unexpected token '${tok.value}' at top level", tok.location))
                        synchronizeTopLevel()
                    }
                }
            }

            return if (errors.isNotEmpty()) {
                VireoResult.Err(errors)
            } else {
                VireoResult.Ok(
                    VireoFile(
                        path = filePath,
                        imports = imports,
                        vars = vars,
                        functions = functions,
                        blocks = blocks,
                        location = fileLocation
                    )
                )
            }
        }

        private fun parseImport(): Import? {
            val importTok = advance() // consume 'import'
            val aliasTok = consume(TokenType.IDENTIFIER, "Expected import alias identifier after 'import'")
            consume(TokenType.KEYWORD_FROM, "Expected 'from' after import alias")
            val pathTok = consume(TokenType.STRING, "Expected string file path after 'from'")

            if (aliasTok == null || pathTok == null) {
                return null
            }

            return Import(
                alias = aliasTok.value,
                filePath = pathTok.value,
                location = importTok.location
            )
        }

        private fun parseVarDeclaration(): VarDeclaration? {
            val varTok = advance() // consume 'var'
            val nameTok = consume(TokenType.IDENTIFIER, "Expected variable name identifier after 'var'")
            val varName = nameTok?.value ?: ""

            if (consume(TokenType.EQUALS, "Expected '=' after variable name '$varName'") == null) {
                synchronizeTopLevel()
                return null
            }

            val targetLine = nameTok?.location?.line ?: varTok.location.line
            val lineTokens = mutableListOf<Token>()
            while (!isAtEnd() && (peek().location.line == targetLine || peek().type == TokenType.KEYWORD_IF || peek().type == TokenType.KEYWORD_THEN || peek().type == TokenType.KEYWORD_ELSE) && peek().type != TokenType.RBRACE && peek().type != TokenType.LBRACE && peek().type != TokenType.KEYWORD_BLOCK && peek().type != TokenType.KEYWORD_VAR && peek().type != TokenType.KEYWORD_FUN && peek().type != TokenType.EOF) {
                lineTokens.add(advance())
            }

            if (lineTokens.isEmpty()) {
                errors.add(VireoError("Expected variable initializer value after '=' for variable '$varName'", varTok.location))
                return null
            }

            val propVal = parsePropertyValueFromTokens(lineTokens)

            return VarDeclaration(
                name = varName,
                value = propVal,
                location = varTok.location
            )
        }

        private fun parseFunDeclaration(): FunDeclaration? {
            val funTok = advance() // consume 'fun'
            val nameTok = consume(TokenType.IDENTIFIER, "Expected function name identifier after 'fun'")
            val funName = nameTok?.value ?: ""

            if (consume(TokenType.LPAREN, "Expected '(' after function name '$funName'") == null) {
                synchronizeTopLevel()
                return null
            }

            val parameters = mutableListOf<Parameter>()
            while (!check(TokenType.RPAREN) && !isAtEnd()) {
                val paramNameTok = consume(TokenType.IDENTIFIER, "Expected parameter name in function '$funName'")
                if (paramNameTok == null) break
                consume(TokenType.COLON, "Expected ':' after parameter name '${paramNameTok.value}'")
                val paramTypeTok = consume(TokenType.IDENTIFIER, "Expected parameter type after ':'")
                if (paramTypeTok == null) break

                parameters.add(
                    Parameter(
                        name = paramNameTok.value,
                        type = paramTypeTok.value,
                        location = paramNameTok.location
                    )
                )

                if (check(TokenType.COMMA)) {
                    advance()
                } else {
                    break
                }
            }

            consume(TokenType.RPAREN, "Expected ')' after function parameters for '$funName'")
            consume(TokenType.COLON, "Expected ':' for return type of function '$funName'")
            val returnTypeTok = consume(TokenType.IDENTIFIER, "Expected return type identifier for function '$funName'")
            val returnType = returnTypeTok?.value ?: ""

            if (consume(TokenType.LBRACE, "Expected '{' to start body of function '$funName'") == null) {
                synchronizeTopLevel()
                return null
            }

            if (check(TokenType.KEYWORD_RETURN)) {
                advance()
            }

            val bodyTokens = mutableListOf<Token>()
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                bodyTokens.add(advance())
            }

            consume(TokenType.RBRACE, "Expected '}' to close body of function '$funName'")

            val returnExpr = if (bodyTokens.isNotEmpty()) {
                parsePropertyValueFromTokens(bodyTokens)
            } else {
                PropertyValue.Literal("", funTok.location)
            }

            return FunDeclaration(
                name = funName,
                parameters = parameters,
                returnType = returnType,
                returnExpr = returnExpr,
                location = funTok.location
            )
        }

        private fun parseBlock(): Block? {
            val blockTok = advance() // consume 'block'
            val nameTok = consume(TokenType.IDENTIFIER, "Expected block name identifier after 'block'")
            val blockName = nameTok?.value ?: ""

            if (consume(TokenType.LBRACE, "Expected '{' after block name '$blockName'") == null) {
                synchronizeTopLevel()
                return null
            }

            val components = mutableListOf<ComponentNode>()
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                if (check(TokenType.KEYWORD_COMPONENT)) {
                    val comp = parseComponent()
                    if (comp != null) {
                        components.add(comp)
                    }
                } else {
                    val tok = peek()
                    errors.add(VireoError("Unexpected token '${tok.value}' inside block '$blockName'", tok.location))
                    synchronizeInsideBlock()
                }
            }

            consume(TokenType.RBRACE, "Expected '}' to close block '$blockName'")

            return Block(
                name = blockName,
                components = components,
                location = blockTok.location
            )
        }

        private fun parseComponent(): ComponentNode? {
            val compTok = advance() // consume 'component'
            val nameTok = consume(TokenType.IDENTIFIER, "Expected component name identifier after 'component'")
            val compName = nameTok?.value ?: ""

            if (consume(TokenType.LBRACE, "Expected '{' after component name '$compName'") == null) {
                synchronizeInsideBlock()
                return null
            }

            val properties = mutableListOf<Property>()
            val constraints = mutableListOf<Constraint>()
            val children = mutableListOf<ComponentNode>()

            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                if (check(TokenType.KEYWORD_COMPONENT)) {
                    val childComp = parseComponent()
                    if (childComp != null) {
                        children.add(childComp)
                    }
                } else if (isPropertyStart()) {
                    parsePropertyOrConstraint(properties, constraints)
                } else {
                    val tok = peek()
                    errors.add(VireoError("Unexpected token '${tok.value}' inside component '$compName'", tok.location))
                    synchronizeInsideComponent()
                }
            }

            consume(TokenType.RBRACE, "Expected '}' to close component '$compName'")

            val layoutProp = properties.find { it.key == "layout" }
            if (layoutProp != null) {
                val layoutValStr = when (val v = layoutProp.value) {
                    is PropertyValue.Literal -> v.value.toString()
                    is PropertyValue.Expr -> v.source
                    else -> ""
                }
                val lower = layoutValStr.lowercase()
                val dir = when {
                    lower.contains("stack") || lower.contains("layer") || lower.contains("constraint") -> Direction.STACK
                    lower.contains("horizontal") -> Direction.HORIZONTAL
                    else -> Direction.VERTICAL
                }

                fun parseSizingStr(str: String): Sizing? = when (str.lowercase()) {
                    "fill" -> Sizing.FILL
                    "hug" -> Sizing.HUG
                    "fixed" -> Sizing.FIXED
                    else -> null
                }

                val mainAxisProp = properties.find { it.key == "mainAxis" }
                val mainAxisValStr = (mainAxisProp?.value as? PropertyValue.Literal)?.value?.toString() ?: ""
                val mainAxisSizing = parseSizingStr(mainAxisValStr)
                    ?: layoutValStr.split("\\s+".toRegex()).getOrNull(1)?.let { parseSizingStr(it) }
                    ?: Sizing.HUG

                val crossAxisProp = properties.find { it.key == "crossAxis" }
                val crossAxisValStr = (crossAxisProp?.value as? PropertyValue.Literal)?.value?.toString() ?: ""
                val crossAxisSizing = parseSizingStr(crossAxisValStr)
                    ?: layoutValStr.split("\\s+".toRegex()).getOrNull(2)?.let { parseSizingStr(it) }
                    ?: Sizing.HUG

                val gapProp = properties.find { it.key == "gap" }
                val gapVal = when (val v = gapProp?.value) {
                    is PropertyValue.Literal -> (v.value as? Number)?.toFloat() ?: v.value.toString().toFloatOrNull() ?: 0f
                    else -> layoutValStr.split("\\s+".toRegex()).getOrNull(3)?.toFloatOrNull() ?: 0f
                }

                constraints.removeAll { it is Constraint.AutoLayout }
                constraints.add(
                    Constraint.AutoLayout(
                        direction = dir,
                        mainAxis = mainAxisSizing,
                        crossAxis = crossAxisSizing,
                        gap = gapVal,
                        location = layoutProp.location
                    )
                )
            }

            return ComponentNode(
                name = compName,
                properties = properties,
                constraints = constraints,
                children = children,
                location = compTok.location
            )
        }

        private fun isPropertyStart(): Boolean {
            val first = peek().type
            val isKey = first == TokenType.IDENTIFIER || isKeywordAsIdentifier(first)
            return isKey && peekNext().type == TokenType.COLON
        }

        private fun parsePropertyValueFromTokens(tokens: List<Token>): PropertyValue {
            if (tokens.isEmpty()) {
                return PropertyValue.Literal("", SourceLocation(filePath, 1, 1))
            }

            val ifIndex = tokens.indexOfFirst { it.type == TokenType.KEYWORD_IF }
            if (ifIndex != -1) {
                val ifTok = tokens[ifIndex]
                val thenIndex = tokens.indexOfFirst { it.type == TokenType.KEYWORD_THEN }
                val elseIndex = tokens.indexOfFirst { it.type == TokenType.KEYWORD_ELSE }

                if (thenIndex > ifIndex && elseIndex > thenIndex) {
                    val condTokens = tokens.subList(ifIndex + 1, thenIndex)
                    val thenTokens = tokens.subList(thenIndex + 1, elseIndex)
                    val elseTokens = tokens.subList(elseIndex + 1, tokens.size)

                    val condition = parsePropertyValueFromTokens(condTokens)
                    val thenBranch = parsePropertyValueFromTokens(thenTokens)
                    val elseBranch = parsePropertyValueFromTokens(elseTokens)

                    return PropertyValue.ConditionalExpr(
                        condition = condition,
                        thenBranch = thenBranch,
                        elseBranch = elseBranch,
                        location = ifTok.location
                    )
                }
            }

            if (tokens.size == 1) {
                val tok = tokens.first()
                return when (tok.type) {
                    TokenType.NUMBER -> {
                        val numStr = tok.value
                        val numVal: Any = if (numStr.contains('.')) {
                            numStr.toDouble()
                        } else {
                            numStr.toIntOrNull() ?: numStr.toLongOrNull() ?: numStr.toDouble()
                        }
                        PropertyValue.Literal(numVal, tok.location)
                    }
                    TokenType.STRING, TokenType.HEX_COLOR -> PropertyValue.Literal(tok.value, tok.location)
                    else -> {
                        val litVal: Any = when (tok.value) {
                            "true" -> true
                            "false" -> false
                            else -> tok.value
                        }
                        PropertyValue.Literal(litVal, tok.location)
                    }
                }
            }

            val formattedExpr = formatExpressionTokens(tokens)

            val isExpr = tokens.any {
                it.type == TokenType.DOLLAR ||
                it.type == TokenType.DOUBLE_EQUALS ||
                it.type == TokenType.PLUS ||
                it.type == TokenType.MINUS ||
                it.type == TokenType.STAR ||
                it.type == TokenType.SLASH ||
                it.type == TokenType.PERCENT ||
                it.type == TokenType.IDENTIFIER
            }

            return if (isExpr) {
                PropertyValue.Expr(formattedExpr, tokens.first().location)
            } else {
                PropertyValue.Literal(formattedExpr, tokens.first().location)
            }
        }

        private fun parsePropertyOrConstraint(
            properties: MutableList<Property>,
            constraints: MutableList<Constraint>
        ) {
            val keyTok = advance()
            advance() // consume ':'

            if (isAtEnd() || check(TokenType.RBRACE)) {
                errors.add(VireoError("Expected property value after ':' for key '${keyTok.value}'", keyTok.location))
                return
            }

            val valTok = peek()
            if (keyTok.value == "ref" || valTok.type == TokenType.KEYWORD_REF) {
                if (valTok.type == TokenType.KEYWORD_REF) {
                    advance() // consume 'ref' keyword
                }
                parseReferenceValue(keyTok, properties)
                return
            }

            val targetLine = valTok.location.line
            val lineTokens = mutableListOf<Token>()
            while (!isAtEnd() && (peek().location.line == targetLine || peek().type == TokenType.KEYWORD_IF || peek().type == TokenType.KEYWORD_THEN || peek().type == TokenType.KEYWORD_ELSE) && peek().type != TokenType.RBRACE && peek().type != TokenType.LBRACE && !isPropertyStart()) {
                lineTokens.add(advance())
            }

            if (lineTokens.isEmpty()) {
                errors.add(VireoError("Expected property value after ':' for key '${keyTok.value}'", keyTok.location))
                return
            }

            val axis = parseAxis(keyTok.value)
            val parsedValue = parsePropertyValueFromTokens(lineTokens)

            if (parsedValue is PropertyValue.ConditionalExpr) {
                properties.add(Property(keyTok.value, parsedValue, keyTok.location))
                return
            }

            if (lineTokens.size == 1) {
                val tok = lineTokens.first()
                if (tok.type == TokenType.NUMBER && axis != null) {
                    val numVal = (parsedValue as? PropertyValue.Literal)?.value
                    if (numVal is Number) {
                        constraints.add(Constraint.Explicit(axis, numVal.toFloat(), keyTok.location))
                    }
                } else if (keyTok.value == "layout" && (tok.value.lowercase() == "horizontal" || tok.value.lowercase() == "vertical" || tok.value.lowercase() == "stack" || tok.value.lowercase() == "layer" || tok.value.lowercase() == "constraint")) {
                    val dir = when (tok.value.lowercase()) {
                        "stack", "layer", "constraint" -> Direction.STACK
                        "vertical" -> Direction.VERTICAL
                        else -> Direction.HORIZONTAL
                    }
                    constraints.add(Constraint.AutoLayout(direction = dir, mainAxis = Sizing.HUG, crossAxis = Sizing.HUG, gap = 0f, location = keyTok.location))
                }
                properties.add(Property(keyTok.value, parsedValue, keyTok.location))
            } else {
                val formattedExpr = formatExpressionTokens(lineTokens)
                val isRelational = axis != null && (formattedExpr.contains("parent") || lineTokens.any { it.type == TokenType.PERCENT })

                if (isRelational && axis != null) {
                    properties.add(Property(keyTok.value, PropertyValue.Expr(formattedExpr, lineTokens[0].location), keyTok.location))
                    constraints.add(Constraint.Relational(axis, formattedExpr, keyTok.location))
                } else {
                    properties.add(Property(keyTok.value, parsedValue, keyTok.location))
                }
            }
        }

        private fun parseReferenceValue(keyTok: Token, properties: MutableList<Property>) {
            val fileTok = consume(TokenType.IDENTIFIER, "Expected file alias identifier in reference")
            if (fileTok == null) {
                return
            }
            if (consume(TokenType.DOT, "Expected '.' after file alias in reference") == null) {
                return
            }
            val blockTok = consume(TokenType.IDENTIFIER, "Expected block name identifier in reference")
            if (blockTok == null) {
                return
            }
            if (consume(TokenType.DOT, "Expected '.' after block name in reference") == null) {
                return
            }
            val compTok = consume(TokenType.IDENTIFIER, "Expected component name identifier in reference")
            if (compTok == null) {
                return
            }

            val refLoc = fileTok.location
            val refObj = Reference(
                file = fileTok.value,
                block = blockTok.value,
                component = compTok.value,
                location = refLoc
            )
            val propVal = PropertyValue.Ref(refObj, refLoc)
            properties.add(Property(keyTok.value, propVal, keyTok.location))
        }

        private fun parseAxis(name: String): Axis? = when (name.lowercase()) {
            "width" -> Axis.WIDTH
            "height" -> Axis.HEIGHT
            "x" -> Axis.X
            "y" -> Axis.Y
            else -> null
        }

        private fun formatExpressionTokens(tokens: List<Token>): String {
            val sb = StringBuilder()
            for (i in tokens.indices) {
                val tok = tokens[i]
                if (i > 0) {
                    val prev = tokens[i - 1]
                    val noSpace = (tok.type == TokenType.DOT || prev.type == TokenType.DOT ||
                            tok.type == TokenType.PERCENT || prev.type == TokenType.PERCENT ||
                            prev.type == TokenType.DOLLAR)
                    if (!noSpace) {
                        sb.append(" ")
                    }
                }
                if (tok.type == TokenType.STRING) {
                    sb.append("\"").append(tok.value).append("\"")
                } else {
                    sb.append(tok.value)
                }
            }
            return sb.toString()
        }

        private fun isKeywordAsIdentifier(type: TokenType): Boolean {
            return when (type) {
                TokenType.KEYWORD_REF,
                TokenType.KEYWORD_VAR,
                TokenType.KEYWORD_FROM,
                TokenType.KEYWORD_RETURN,
                TokenType.KEYWORD_IF,
                TokenType.KEYWORD_THEN,
                TokenType.KEYWORD_ELSE -> true
                else -> false
            }
        }

        private fun peek(): Token = if (current < tokens.size) tokens[current] else Token(TokenType.EOF, "", SourceLocation(filePath, 1, 1))
        private fun peekNext(): Token = if (current + 1 < tokens.size) tokens[current + 1] else Token(TokenType.EOF, "", SourceLocation(filePath, 1, 1))
        private fun isAtEnd(): Boolean = current >= tokens.size || peek().type == TokenType.EOF

        private fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type

        private fun advance(): Token {
            val tok = peek()
            if (!isAtEnd()) current++
            return tok
        }

        private fun consume(type: TokenType, errorMessage: String): Token? {
            return if (check(type)) {
                advance()
            } else {
                val loc = peek().location
                errors.add(VireoError(errorMessage, loc))
                null
            }
        }

        private fun synchronizeTopLevel() {
            if (!isAtEnd()) advance()
            while (!isAtEnd()) {
                if (check(TokenType.KEYWORD_IMPORT) || check(TokenType.KEYWORD_VAR) || check(TokenType.KEYWORD_FUN) || check(TokenType.KEYWORD_BLOCK)) return
                advance()
            }
        }

        private fun synchronizeInsideBlock() {
            if (!isAtEnd()) advance()
            while (!isAtEnd()) {
                if (check(TokenType.KEYWORD_COMPONENT) || check(TokenType.RBRACE)) return
                advance()
            }
        }

        private fun synchronizeInsideComponent() {
            if (!isAtEnd()) advance()
            while (!isAtEnd()) {
                if (check(TokenType.KEYWORD_COMPONENT) || check(TokenType.RBRACE) || isPropertyStart()) return
                advance()
            }
        }
    }
}
