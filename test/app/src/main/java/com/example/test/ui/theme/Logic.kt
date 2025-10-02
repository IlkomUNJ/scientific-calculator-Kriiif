package com.example.test.ui.theme

import java.util.Stack
import kotlin.math.*

class CalculatorLogic {
    private var currentExpression: String = "0"

    fun onDigit(digit: String) {
        if (currentExpression == "0" && digit != ".") {
            currentExpression = digit
        } else if (digit == "." && currentExpression.contains(".")) {
            return
        } else {
            currentExpression += digit
        }
    }

    fun onConstant(constant: String) {
        // Replace current expression with the constant value
        currentExpression = when(constant) {
            "e" -> E.toString()
            else -> currentExpression
        }
    }

    fun onOperator(operator: String) {
        // Prevent adding an operator right after another operator, unless it's a parenthesis
        if (currentExpression.isNotEmpty() && isOperator(currentExpression.last().toString())) {
            // Allow an operator to replace the last one, unless the new one is a parenthesis
            if (operator !in listOf("(", ")")) {
                currentExpression = currentExpression.dropLast(1) + operator
            } else {
                currentExpression += operator
            }
        } else {
            currentExpression += operator
        }
    }

    fun onScientificFunction(function: String) {
        if (function in listOf("x!", "1/x", "%")) {
            onEquals() // First, evaluate the current expression
            try {
                val numberToOperate = currentExpression.toDouble()
                val internalOperator = when (function) {
                    "x!" -> "!"
                    "1/x" -> "1/x"
                    "%" -> "%"
                    else -> function
                }
                val result = performScientificCalculation(numberToOperate, internalOperator)
                currentExpression = formatNumber(result)
            } catch (e: Exception) {
                currentExpression = "Error"
            }
        } else {
            // All other scientific functions prepend to the expression
            // e.g., pressing 'sin' on '5' results in 'sin(5'
            if (currentExpression == "0") {
                currentExpression = "$function("
            } else {
                currentExpression += "$function("
            }
        }
    }

    fun onEquals() {
        try {
            val result = evaluateExpression(currentExpression)
            currentExpression = formatNumber(result)
        } catch (e: Exception) {
            currentExpression = "Error"
        }
    }

    fun onClear() {
        currentExpression = "0"
    }

    fun onDelete() {
        if (currentExpression.length > 1) {
            currentExpression = currentExpression.dropLast(1)
        } else {
            currentExpression = "0"
        }
    }

    fun getDisplay(): String {
        return currentExpression
    }

    private fun evaluateExpression(expression: String): Double {
        val tokens = tokenize(expression)
        val rpnTokens = shuntingYard(tokens)
        return evaluateRPN(rpnTokens)
    }

    private fun tokenize(expression: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expression.length) {
            val char = expression[i]
            when {
                char.isDigit() || char == '.' -> {
                    var numberBuffer = ""
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.')) {
                        numberBuffer += expression[i]
                        i++
                    }
                    tokens.add(numberBuffer)
                }
                // Check for multi-character functions (e.g., sin, cos, log)
                char.isLetter() -> {
                    var functionBuffer = ""
                    while (i < expression.length && expression[i].isLetter()) {
                        functionBuffer += expression[i]
                        i++
                    }
                    tokens.add(functionBuffer)
                }
                // Handle unary minus: if it's at the start or follows an operator/paren
                char == '-' && (tokens.isEmpty() || tokens.last() in listOf("(", "+", "-", "x", "/", "^")) -> {
                    var numberBuffer = "-"
                    i++
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.')) {
                        numberBuffer += expression[i]
                        i++
                    }
                    tokens.add(numberBuffer)
                }
                isOperator(char.toString()) || char == '(' || char == ')' -> {
                    tokens.add(char.toString())
                    i++
                }
                else -> i++
            }
        }
        return tokens.filter { it.isNotBlank() }
    }

    private fun shuntingYard(tokens: List<String>): List<String> {
        val outputQueue = mutableListOf<String>()
        val operatorStack = Stack<String>()

        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> {
                    outputQueue.add(token)
                }
                isFunction(token) -> {
                    operatorStack.push(token)
                }
                isOperator(token) -> {
                    while (operatorStack.isNotEmpty() &&
                        (isFunction(operatorStack.peek()) || (isOperator(operatorStack.peek()) && getPrecedence(operatorStack.peek()) >= getPrecedence(token)))
                    ) {
                        outputQueue.add(operatorStack.pop())
                    }
                    operatorStack.push(token)
                }
                token == "(" -> {
                    operatorStack.push(token)
                }
                token == ")" -> {
                    while (operatorStack.isNotEmpty() && operatorStack.peek() != "(") {
                        outputQueue.add(operatorStack.pop())
                    }
                    if (operatorStack.isNotEmpty() && operatorStack.peek() == "(") {
                        operatorStack.pop() // Discard the "("
                    } else {
                        throw IllegalArgumentException("Mismatched parentheses")
                    }
                    if (operatorStack.isNotEmpty() && isFunction(operatorStack.peek())) {
                        outputQueue.add(operatorStack.pop())
                    }
                }
            }
        }
        while (operatorStack.isNotEmpty()) {
            if (operatorStack.peek() == "(") throw IllegalArgumentException("Mismatched parentheses")
            outputQueue.add(operatorStack.pop())
        }
        return outputQueue
    }

    private fun evaluateRPN(tokens: List<String>): Double {
        val stack = Stack<Double>()
        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> {
                    stack.push(token.toDouble())
                }
                isFunction(token) -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Invalid RPN expression: missing operand for $token")
                    val operand = stack.pop()
                    val result = performScientificCalculation(operand, token)
                    stack.push(result)
                }
                isOperator(token) -> {
                    if (stack.size < 2) throw IllegalArgumentException("Invalid RPN expression: missing operand for $token")
                    val operand2 = stack.pop()
                    val operand1 = stack.pop()
                    val result = performCalculation(operand1, operand2, token)
                    stack.push(result)
                }
            }
        }
        if (stack.size != 1) throw IllegalArgumentException("Invalid RPN expression")
        return stack.pop()
    }

    private fun getPrecedence(op: String): Int {
        return when (op) {
            "+", "-" -> 1
            "x", "/" -> 2
            "^" -> 3
            // Assign a high precedence to scientific functions
            "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt" -> 4
            else -> 0
        }
    }

    private fun Double.degToRadians(): Double=this*PI/180
    private fun Double.radiansToDeg(): Double=this*180/PI

    private fun isOperator(token: String): Boolean {
        return token in listOf("+", "-", "x", "/", "^")
    }

    private fun isFunction(token: String): Boolean {
        return token in listOf("sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt")
    }

    private fun performCalculation(operand1: Double, operand2: Double, operator: String): Double {
        return when (operator) {
            "+" -> operand1 + operand2
            "-" -> operand1 - operand2
            "x" -> operand1 * operand2
            "/" -> if (operand2 != 0.0) operand1 / operand2 else Double.NaN
            "^" -> operand1.pow(operand2)
            else -> Double.NaN
        }
    }

    private fun performScientificCalculation(operand: Double, operator: String): Double {
        return when (operator) {
            // Trigonometric
            "sin" -> sin(operand.degToRadians())
            "cos" -> cos(operand.degToRadians())
            "tan" -> tan(operand.degToRadians())
            // Corrected: acos, asin, atan do not need to convert to degrees twice
            "asin" -> asin(operand).radiansToDeg()
            "acos" -> acos(operand).radiansToDeg()
            "atan" -> atan(operand).radiansToDeg()
            // Logarithmic
            "log" -> log10(operand) // Base 10
            "ln" -> ln(operand) // Natural log (base e)
            // Other
            "sqrt" -> sqrt(operand)
            "!" -> factorial(operand) // Factorial
            "1/x" -> 1.0 / operand // Reciprocal
            "%" -> operand / 100.0 // Percentage
            else -> Double.NaN
        }
    }

    private fun factorial(n: Double): Double {
        // Check for non-negative integer
        if (n < 0 || n != n.toInt().toDouble()) return Double.NaN
        if (n == 0.0) return 1.0
        var result = 1.0
        for (i in 1..n.toInt()) {
            result *= i
        }
        return result
    }

    private fun formatNumber(number: Double?): String {
        if (number == null || number.isNaN() || number.isInfinite()) return "Error"
        return if (number % 1 == 0.0) {
            number.toLong().toString()
        } else {
            number.toString()
        }
    }
}