package com.example.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.ui.theme.TestTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.test.ui.theme.CalculatorLogic

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestTheme {
                CalculatorUI()
            }
        }
    }
}

@Composable
fun CalculatorUI() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            var isScientificMode by remember { mutableStateOf(false) }

            val calculatorLogic = remember { CalculatorLogic() }
            var displayValue by remember { mutableStateOf(calculatorLogic.getDisplay()) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = displayValue,
                    fontSize = 32.sp,
                    textAlign = TextAlign.End
                )
            }

            val operatorColor = Color(0xFFFF9800)
            val specialColor = Color(0xFFC0C0C0)
            val digitColor = Color(0xFFF0F0F0)

            // Button layout data
            val mainButtons = listOf(
                listOf("AC", "Del", "x^y", "/"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("Sc", "0", ".", "=")
            )

            val scientificButtons = listOf(
                listOf("log", "ln", "sin", "cos", "tan"),
                listOf("sqrt", "asin", "acos", "atan", "x!")
            )

            if (isScientificMode) {
                scientificButtons.forEach { rowButtons ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowButtons.forEach { buttonText ->
                            CalcButton(buttonText, background = specialColor) {
                                // Scientific functions apply to the current result/display
                                calculatorLogic.onScientificFunction(buttonText)
                                displayValue = calculatorLogic.getDisplay()
                            }
                        }
                    }
                }
            }

            // Main calculator grid rendered with a loop
            mainButtons.forEachIndexed { index, rowButtons ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Extra scientific buttons in the main grid when in scientific mode
                    if (isScientificMode) {
                        when (index) {
                            0 -> CalcButton("1/x", background = specialColor) {
                                calculatorLogic.onScientificFunction("1/x")
                                displayValue = calculatorLogic.getDisplay()
                            }
                            1 -> CalcButton("(", background = specialColor) {
                                calculatorLogic.onOperator("(")
                                displayValue = calculatorLogic.getDisplay()
                            }
                            2 -> CalcButton(")", background = specialColor) {
                                calculatorLogic.onOperator(")")
                                displayValue = calculatorLogic.getDisplay()
                            }
                            3 -> CalcButton("e", background = specialColor) {
                                calculatorLogic.onConstant("e")
                                displayValue = calculatorLogic.getDisplay()
                            }
                            4 -> CalcButton("%", background = specialColor) {
                                calculatorLogic.onScientificFunction("%")
                                displayValue = calculatorLogic.getDisplay()
                            }
                        }
                    }

                    // Loop through the buttons in the current row
                    rowButtons.forEach { buttonText ->
                        val (backgroundColor, onClickAction) = when (buttonText) {
                            "AC" -> specialColor to {
                                calculatorLogic.onClear()
                                displayValue = calculatorLogic.getDisplay()
                            }
                            "Del" -> specialColor to {
                                calculatorLogic.onDelete()
                                displayValue = calculatorLogic.getDisplay()
                            }
                            "Sc" -> specialColor to { isScientificMode = !isScientificMode }
                            "=" -> operatorColor to {
                                calculatorLogic.onEquals()
                                displayValue = calculatorLogic.getDisplay()
                            }
                            "x^y", "/", "×", "−", "+" -> operatorColor to {
                                val operatorToSend = when (buttonText) {
                                    "×" -> "x"
                                    "−" -> "-"
                                    "x^y" -> "^"
                                    else -> buttonText
                                }
                                calculatorLogic.onOperator(operatorToSend)
                                displayValue = calculatorLogic.getDisplay()
                            }
                            else -> digitColor to {
                                calculatorLogic.onDigit(buttonText)
                                displayValue = calculatorLogic.getDisplay()
                            }
                        }

                        CalcButton(textButton = buttonText, background = backgroundColor, onClick = onClickAction)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.CalcButton(
    textButton: String,
    weight: Float = 1f,
    background: Color = Color(0xFFEEEEEE),
    onClick: () -> Unit = {}
) {
    Button(
        modifier = Modifier
            .weight(weight)
            .aspectRatio(1f)
            .padding(4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = textButton,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorUIPreview() {
    TestTheme {
        CalculatorUI()
    }
}