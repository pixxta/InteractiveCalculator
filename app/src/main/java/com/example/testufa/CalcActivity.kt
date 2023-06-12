package com.example.testufa

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import net.objecthunter.exp4j.ExpressionBuilder
import androidx.compose.foundation.*

import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlin.random.Random
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.*


class EmptyActivity : ComponentActivity() {
    private lateinit var calcDataDao: CalcDataDao
    private lateinit var calcField: CalcFieldDao

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        super.onCreate(savedInstanceState)
        val data = intent.getStringExtra("NUMBER_EXTRA").toString().split("#")
        var number = ""
        var com = ""
        if (data.size >= 2) {
            number = data[0]
            com = data[1]
        }
        setContent {
            var t = ""
            val database = CalcDatabase.getDatabase(applicationContext)
            calcDataDao = database.calcDataDao()
            calcField = database.calcFieldDao()
            lifecycleScope.launch {
                val calcDataList = withContext(Dispatchers.IO) {
                    calcDataDao.getAllCalcData()

                }
                println("АЛЁ")
                for (calcData in calcDataList) {
                    println("s1: ${calcData.s1}, s2: ${calcData.s2}, id: ${calcData.id}")
                }

                val calcField = withContext(Dispatchers.IO) {
                    calcField.getAllCalcFields()

                }
            }
            ThreeColumnLayout(calcDataDao, calcField, number, com)
        }
    }

    private fun deleteAllData() {
        runBlocking {
            withContext(Dispatchers.IO) {
                calcDataDao.deleteAllCalcData()
            }
        }
    }
}



@Composable
fun ThreeColumnLayout(calcDataDao: CalcDataDao, calcField: CalcFieldDao, flag: String, comment: String) {
    val buttonClicks = remember { mutableStateOf(0) }
    val flag1 = remember { mutableStateOf(0) }
    var currentExpression by remember { mutableStateOf("") }
    var isOpeningBracket by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var lastEvaluationResult by remember { mutableStateOf("") }
    val evaluatedResult = evaluateExpression(currentExpression)
    var h1 by remember { mutableStateOf("") }
    var h2 by remember { mutableStateOf(flag) }
    val itemsList = remember { mutableStateListOf<HistData>() }
    val itemsList1 = remember { mutableStateListOf<String>() }

    LaunchedEffect(buttonClicks.value) {
        withContext(Dispatchers.IO) {
            if (h1!="" && h2!= "")
            {
                val newData = CalcData(0, h1, h2)
                calcDataDao.insertCalcData(newData)
                println("ОДНА ВОВО")
            }

            val dataList = calcDataDao.getAllCalcData()
            itemsList.clear()
            itemsList.addAll(dataList.map { HistData(it.s1, it.s2) })
            itemsList.reverse()
        }
    }


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if(flag != "" && flag1.value==0) {
                flag1.value++


                currentExpression = flag
            }

        }
    }




    if (evaluatedResult != "ОШИБКА") {
        lastEvaluationResult = evaluatedResult
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
        }
    }

    Row(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 5.dp)
                .width(80.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("NUMBER_EXTRA", "$h2#$comment")
                    }
                    launcher.launch(intent)
                },
                modifier = Modifier
                    .fillMaxSize(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                    228,
                    226,
                    226,
                    255
                )
                )

            ) {
                Text("<", fontSize = 45.sp)
            }
        }

        // Поле для истории
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 5.dp, vertical = 10.dp)
                .background(Color(
                    228,
                    226,
                    226,
                    255
                ), RoundedCornerShape(40.dp))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp, start = 30.dp)
                    .align(Alignment.TopEnd)
            ) {
                Text(
                    text = "История",
                    modifier = Modifier.align(Alignment.Start),
                    color = Color.DarkGray,
                    fontSize = 30.sp
                )
                LazyColumn(reverseLayout = true, modifier = Modifier.fillMaxSize()) {
                    items(itemsList) { item ->
                        // Основные элементы списка
                        Text(
                            text = "="+item.s2,
                            modifier = Modifier
                                .padding(end = 16.dp, bottom = 40.dp, top=4.dp)
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.End),
                            fontSize = 40.sp,
                            color = Color.DarkGray,
                        )
                        Text(
                            text = item.s1,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.End),
                            fontSize = 40.sp,
                            color = Color.DarkGray,
                        )

                    }
                }
            }

        }


        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .padding(horizontal = 5.dp, vertical = 10.dp)
        ) {
            // Поле для вывода чисел
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color.LightGray, RoundedCornerShape(40.dp))
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp, bottom = 50.dp) // Добавляем отступы справа и снизу
                ) {
                    Text(
                        text = currentExpression,
                        modifier = Modifier.align(Alignment.End),
                        color = Color.DarkGray,
                        fontSize = 110.sp,
                        lineHeight = 150.sp // Устанавливаем высоту строки для создания отступа между текстом
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // Добавляем отступ между текстами
                    Text(
                        text = "= " + lastEvaluationResult,
                        modifier = Modifier.align(Alignment.End),
                        color = Color.DarkGray,
                        fontSize = 80.sp
                    )
                }
            }




            // Поле для кнопок
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .height(IntrinsicSize.Max)

                    ) {
                        Button(
                            onClick = { currentExpression += "7" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("7", fontSize = 25.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { currentExpression += "8" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("8", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { currentExpression += "9" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("9", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = {
                                if (currentExpression.isNotEmpty() && (currentExpression.last().isDigit() && currentExpression.last() != '.' || currentExpression.last() == ')' || currentExpression.last() == '(')) {
                                    currentExpression += "/"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(191, 197, 249)),
                        ) {
                            Text("/", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { currentExpression = ""
                                isOpeningBracket = true
                                lastEvaluationResult = ""},
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                247,
                                184,
                                184,
                                255
                            )
                            ),
                        ) {
                            Text("AC", fontSize = 25.sp, color = Color.Black)
                        }
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .height(IntrinsicSize.Max)
                    ) {
                        Button(
                            onClick = { currentExpression += "4" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("4", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { currentExpression += "5" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("5", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { currentExpression += "6" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("6", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = {
                                if (currentExpression.isNotEmpty() && (currentExpression.last().isDigit() && currentExpression.last() != '.' || currentExpression.last() == ')' || currentExpression.last() == '(')) {
                                    currentExpression += "*"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(191, 197, 249)),
                        ) {
                            Text("*", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = {
                                val lastChar = currentExpression.lastOrNull()
                                if (isOpeningBracket) {
                                    if (currentExpression.isEmpty() || lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/') {
                                        currentExpression += "("
                                        isOpeningBracket = false
                                    }
                                } else if (lastChar != null && lastChar.isDigit()) {
                                    currentExpression += ")"
                                    isOpeningBracket = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(191, 197, 249)),
                        ) {
                            Text(if (isOpeningBracket) "(" else ")", fontSize = 25.sp, color = Color.Black)
                        }




                    }

                    Row(
                        Modifier
                            .weight(1f)
                            .height(IntrinsicSize.Max)
                    ) {
                        Button(
                            onClick = { currentExpression += "1" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("1", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { currentExpression += "2" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("2", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { currentExpression += "3" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("3", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = {
                                if (currentExpression.isNotEmpty() && (currentExpression.last().isDigit() && currentExpression.last() != '.' || currentExpression.last() == ')' || currentExpression.last() == '(')) {
                                    currentExpression += "-"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(191, 197, 249)),
                        ) {
                            Text("-", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(191, 197, 249)),
                        ) {
                            Text("%", fontSize = 25.sp, color = Color.Black)
                        }
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .height(IntrinsicSize.Max)
                    ) {
                        Button(
                            onClick = { currentExpression += "0" },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(
                                228,
                                226,
                                226,
                                255
                            )
                            ),
                        ) {
                            Text("0", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = {
                                val lastNumber = currentExpression.split(Regex("[^\\d.]")).lastOrNull()
                                if (currentExpression.isNotEmpty() && currentExpression.last().isDigit() && !lastNumber?.contains(".")!!) {
                                    currentExpression += "."
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(228, 226, 226, 255))
                        ) {
                            Text(".", fontSize = 50.sp, color = Color.Black)
                        }



                        Button(
                            onClick = {
                                if (currentExpression.isNotEmpty()) {
                                    currentExpression = currentExpression.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(228, 226, 226, 255)),
                        ) {
                            Text("C", fontSize = 25.sp, color = Color.Black)
                        }
                        Button(
                            onClick = {
                                if (currentExpression.isNotEmpty() && (currentExpression.last().isDigit() && currentExpression.last() != '.' || currentExpression.last() == ')' || currentExpression.last() == '(')) {
                                    currentExpression += "+"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(191, 197, 249)),
                        ) {
                            Text("+", fontSize = 25.sp, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                try {
                                    val result = ExpressionBuilder(currentExpression).build().evaluate()
                                    var hist = currentExpression
                                    currentExpression = if (result % 1 == 0.0) {
                                        result.toLong().toString()
                                    } else {
                                        String.format("%.5f", result)
                                    }

                                    h1 = hist
                                    h2 = currentExpression
                                    buttonClicks.value++


                                } catch (e: Exception) {
                                    println("Ошибка")

                                }


                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(5.dp),
                            shape = RoundedCornerShape(45.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(191, 197, 249))
                        ) {
                            Text("=", fontSize = 25.sp, color = Color.Black)
                        }


                    }
                }
            }
        }
    }
}


data class HistData(val s1: String, var s2: String)











fun evaluateExpression(expression: String): String {
    return try {
        val result = ExpressionBuilder(expression).build().evaluate()

        if (result % 1 == 0.0) {
            result.toLong().toString()
        } else {
            String.format("%.5f", result)
        }

    } catch (e: Exception) {
        "ОШИБКА"
    }
}







