// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.expr.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.ecmascript6.parsing.ES6ExpressionParser
import com.intellij.lang.ecmascript6.parsing.ES6FunctionParser
import com.intellij.lang.ecmascript6.parsing.ES6Parser
import com.intellij.lang.ecmascript6.parsing.ES6StatementParser
import com.intellij.lang.javascript.*
import com.intellij.lang.javascript.parsing.JSPsiTypeParser
import com.intellij.lang.javascript.parsing.JavaScriptParser
import com.intellij.psi.tree.IElementType
import com.hxz.mpxjs.MpxBundle.message
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.*
import com.hxz.mpxjs.lang.expr.parser.MpxJSElementTypes.FILTER_ARGUMENTS_LIST
import com.hxz.mpxjs.lang.expr.parser.MpxJSElementTypes.FILTER_EXPRESSION
import com.hxz.mpxjs.lang.expr.parser.MpxJSElementTypes.FILTER_LEFT_SIDE_ARGUMENT
import com.hxz.mpxjs.lang.expr.parser.MpxJSElementTypes.FILTER_REFERENCE_EXPRESSION

class MpxJSParser(builder: PsiBuilder, private val isJavaScript: Boolean)
  : ES6Parser<MpxJSParser.MpxJSExpressionParser, MpxJSParser.MpxJSStatementParser, ES6FunctionParser<*>,
  JSPsiTypeParser<JavaScriptParser<*, *, *, *>>>(builder) {

  constructor(builder: PsiBuilder) : this(builder, true)

  companion object {
    fun parseEmbeddedExpression(builder: PsiBuilder, root: IElementType, attributeInfo: MpxAttributeInfo?) {
      val rootMarker = builder.mark()
      val statementMarker = builder.mark()
      val parseAction: (MpxJSStatementParser) -> Unit =
        when (attributeInfo?.kind) {
          MpxAttributeKind.DIRECTIVE ->
            when ((attributeInfo as MpxDirectiveInfo).directiveKind) {
              MpxDirectiveKind.FOR -> MpxJSStatementParser::parseVFor
              MpxDirectiveKind.BIND -> MpxJSStatementParser::parseVBind
              MpxDirectiveKind.ON -> MpxJSStatementParser::parseVOn
              MpxDirectiveKind.SLOT -> MpxJSStatementParser::parseSlotPropsExpression
              else -> MpxJSStatementParser::parseRegularExpression
            }
          MpxAttributeKind.SLOT_SCOPE -> MpxJSStatementParser::parseSlotPropsExpression
          MpxAttributeKind.SCOPE -> MpxJSStatementParser::parseSlotPropsExpression
          else -> MpxJSStatementParser::parseRegularExpression
        }
      MpxJSParser(builder, false).statementParser.let {
        parseAction(it)
        // we need to consume rest of the tokens, even if they are invalid
        it.parseRest()
      }
      statementMarker.done(MpxJSElementTypes.EMBEDDED_EXPR_STATEMENT)
      rootMarker.done(root)
    }

    fun parseInterpolation(builder: PsiBuilder, root: IElementType) {
      parseEmbeddedExpression(builder, root, null)
    }

    fun parseJS(builder: PsiBuilder, root: IElementType) {
      MpxJSParser(builder).parseJS(root)
    }
  }

  init {
    myStatementParser = MpxJSStatementParser(this)
    myExpressionParser = MpxJSExpressionParser(this)
  }

  inner class MpxJSStatementParser(parser: MpxJSParser) : ES6StatementParser<MpxJSParser>(parser) {

    fun parseRegularExpression() {
      if (!myExpressionParser.parseFilterOptional() && !builder.eof()) {
        val mark = builder.mark()
        builder.advanceLexer()
        mark.error(JavaScriptBundle.message("javascript.parser.message.expected.expression"))
        parseRest(true)
      }
    }

    fun parseVOn() {
      while (!builder.eof()) {
        if (builder.tokenType === JSTokenTypes.SEMICOLON) {
          builder.advanceLexer()
        }
        else if (!parseExpressionStatement()) {
          builder.error(JavaScriptBundle.message("javascript.parser.message.expected.expression"))
          if (!builder.eof()) {
            builder.advanceLexer()
          }
        }
      }
    }

    fun parseVBind() {
      if (!myExpressionParser.parseFilterOptional()) {
        val mark = builder.mark()
        if (!builder.eof()) {
          builder.advanceLexer()
        }
        mark.error(JavaScriptBundle.message("javascript.parser.message.expected.expression"))
        parseRest(true)
      }
    }

    fun parseStyle() {
      print("parseStyle")
    }

    fun parseVFor() {
      val vForExpr = builder.mark()
      if (builder.tokenType == JSTokenTypes.LPAR) {
        parseVForVariables()
      }
      else if (!parseVariableStatement(MpxJSStubElementTypes.V_FOR_VARIABLE)) {
        val marker = builder.mark()
        if (!builder.eof()
            && builder.tokenType !== JSTokenTypes.IN_KEYWORD
            && builder.tokenType !== JSTokenTypes.OF_KEYWORD) {
          builder.advanceLexer()
        }
        marker.error(JavaScriptBundle.message("javascript.parser.message.expected.identifier"))
      }
      if (builder.tokenType !== JSTokenTypes.IN_KEYWORD && builder.tokenType !== JSTokenTypes.OF_KEYWORD) {
        builder.error(message("mpx.parser.message.expected.in.or.of"))
      }
      else {
        builder.advanceLexer()
      }
      myExpressionParser.parseExpression()
      vForExpr.done(MpxJSElementTypes.V_FOR_EXPRESSION)
    }

    fun parseSlotPropsExpression() {
      val slotPropsParameterList = builder.mark()
      val functionParser = object : ES6FunctionParser<MpxJSParser>(this@MpxJSParser) {
        override fun getParameterType(): IElementType {
          return MpxJSStubElementTypes.SLOT_PROPS_PARAMETER
        }
      }
      var first = true
      while (!builder.eof()) {
        if (first) {
          first = false
        }
        else {
          if (builder.tokenType === JSTokenTypes.COMMA) {
            builder.advanceLexer()
          }
          else {
            builder.error(message("mpx.parser.message.expected.comma.or.end.of.expression"))
            break
          }
        }
        val parameter = builder.mark()
        if (builder.tokenType === JSTokenTypes.DOT_DOT_DOT) {
          builder.advanceLexer()
        }
        else if (builder.tokenType === JSTokenTypes.DOT) {
          // incomplete ...args
          builder.error(JavaScriptBundle.message("javascript.parser.message.expected.parameter.name"))
          while (builder.tokenType === JSTokenTypes.DOT) {
            builder.advanceLexer()
          }
        }
        functionParser.parseSingleParameter(parameter)
      }
      slotPropsParameterList.done(JSStubElementTypes.PARAMETER_LIST)
      slotPropsParameterList.precede().done(MpxJSElementTypes.SLOT_PROPS_EXPRESSION)
    }

    internal fun parseRest(initialReported: Boolean = false) {
      var reported = initialReported
      while (!builder.eof()) {
        if (builder.tokenType === JSTokenTypes.SEMICOLON) {
          val mark = builder.mark()
          builder.advanceLexer()
          mark.error(message("mpx.parser.message.statements.not.allowed"))
          reported = true
        }
        else {
          var justReported = false
          if (!reported) {
            builder.error(message("mpx.parser.message.expected.end.of.expression"))
            reported = true
            justReported = true
          }
          if (!myExpressionParser.parseExpressionOptional()) {
            if (reported && !justReported) {
              val mark = builder.mark()
              builder.advanceLexer()
              mark.error(JavaScriptBundle.message("javascript.parser.message.expected.expression"))
            }
            else {
              builder.advanceLexer()
            }
          }
          else {
            reported = false
          }
        }
      }
    }

    private fun parseVariableStatement(elementType: IElementType): Boolean {
      val statement = builder.mark()
      if (parseVariable(elementType)) {
        statement.done(JSStubElementTypes.VAR_STATEMENT)
        return true
      }
      else {
        statement.drop()
        return false
      }
    }

    private fun parseVariable(elementType: IElementType): Boolean {
      if (isIdentifierToken(builder.tokenType)) {
        buildTokenElement(elementType)
        return true
      }
      else if (myFunctionParser.willParseDestructuringAssignment()) {
        myExpressionParser.parseDestructuringElement(MpxJSStubElementTypes.V_FOR_VARIABLE, false, false)
        return true
      }
      return false
    }

    private val EXTRA_VAR_COUNT = 2
    private fun parseVForVariables() {
      val parenthesis = builder.mark()
      builder.advanceLexer() //LPAR
      val varStatement = builder.mark()
      if (parseVariable(MpxJSStubElementTypes.V_FOR_VARIABLE)) {
        var i = 0
        while (builder.tokenType == JSTokenTypes.COMMA && i < EXTRA_VAR_COUNT) {
          builder.advanceLexer()
          if (isIdentifierToken(builder.tokenType)) {
            buildTokenElement(MpxJSStubElementTypes.V_FOR_VARIABLE)
          }
          i++
        }
      }
      if (builder.tokenType != JSTokenTypes.RPAR) {
        builder.error(JavaScriptBundle.message("javascript.parser.message.expected.rparen"))
        while (!builder.eof()
               && builder.tokenType != JSTokenTypes.RPAR
               && builder.tokenType != JSTokenTypes.IN_KEYWORD
               && builder.tokenType != JSTokenTypes.OF_KEYWORD) {
          builder.advanceLexer()
        }
        if (builder.tokenType != JSTokenTypes.RPAR) {
          varStatement.done(JSStubElementTypes.VAR_STATEMENT)
          parenthesis.done(JSElementTypes.PARENTHESIZED_EXPRESSION)
          return
        }
      }
      varStatement.done(JSStubElementTypes.VAR_STATEMENT)
      builder.advanceLexer()
      parenthesis.done(JSElementTypes.PARENTHESIZED_EXPRESSION)
    }
  }

  class MpxJSExpressionParser(parser: MpxJSParser) : ES6ExpressionParser<MpxJSParser>(parser) {

    private var expressionNestingLevel: Int = 0

    override fun parseScriptExpression() {
      throw UnsupportedOperationException()
    }

    //regex, curly, square, paren

    fun parseFilterOptional(): Boolean {
      var pipe: PsiBuilder.Marker = builder.mark()
      var firstParam: PsiBuilder.Marker = builder.mark()
      expressionNestingLevel = 0
      if (!parseExpressionOptional()) {
        firstParam.drop()
        pipe.drop()
        return false
      }

      while (builder.tokenType === JSTokenTypes.OR) {
        firstParam.done(FILTER_LEFT_SIDE_ARGUMENT)
        builder.advanceLexer()
        if (isIdentifierToken(builder.tokenType)) {
          val pipeName = builder.mark()
          builder.advanceLexer()
          pipeName.done(FILTER_REFERENCE_EXPRESSION)
        }
        else {
          builder.error(message("mpx.parser.message.expected.identifier.or.string"))
        }
        if (builder.tokenType === JSTokenTypes.LPAR) {
          val params = builder.mark()
          expressionNestingLevel = 2
          parseArgumentListNoMarker()
          params.done(FILTER_ARGUMENTS_LIST)
          if (builder.tokenType !== JSTokenTypes.OR && !builder.eof()) {
            val err = builder.mark()
            builder.advanceLexer()
            err.error(message("mpx.parser.message.expected.pipe.or.end.of.expression"))
            while (builder.tokenType !== JSTokenTypes.OR && !builder.eof()) {
              builder.advanceLexer()
            }
          }
        }
        else if (builder.tokenType !== JSTokenTypes.OR && !builder.eof()) {
          val err = builder.mark()
          builder.advanceLexer()
          err.error(message("mpx.parser.message.expected.lparen.pipe.or.end.of.expression"))
          while (builder.tokenType !== JSTokenTypes.OR && !builder.eof()) {
            builder.advanceLexer()
          }
        }
        pipe.done(FILTER_EXPRESSION)
        firstParam = pipe.precede()
        pipe = firstParam.precede()
      }
      firstParam.drop()
      pipe.drop()
      return true
    }

    override fun parseAssignmentExpression(allowIn: Boolean): Boolean {
      expressionNestingLevel++
      try {
        return super.parseAssignmentExpression(allowIn)
      }
      finally {
        expressionNestingLevel--
      }
    }

    override fun getCurrentBinarySignPriority(allowIn: Boolean, advance: Boolean): Int {
      return if (builder.tokenType === JSTokenTypes.OR && expressionNestingLevel <= 1) {
        -1
      }
      else super.getCurrentBinarySignPriority(allowIn, advance)
    }
  }
}
