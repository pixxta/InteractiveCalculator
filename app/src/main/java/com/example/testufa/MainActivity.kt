package com.example.testufa

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.testufa.ui.theme.TestUfaTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.math.BigInteger
import java.util.*
import kotlin.math.sqrt
class MainActivity : ComponentActivity() {

    private lateinit var dragboxesDao: DragBoxesDao
    private lateinit var calcField: CalcFieldDao

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        val data = intent.getStringExtra("NUMBER_EXTRA").toString().split("#")
        var number = ""
        var com = ""
        if (data.size >= 2) {
            number = data[0]
            com = data[1]
        }
        super.onCreate(savedInstanceState)
        setContent {
            val database = CalcDatabase.getDatabase(applicationContext)
            dragboxesDao = database.dragBoxesDao()
            calcField = database.calcFieldDao()

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val BoxList = dragboxesDao.getAllDragBoxes()
                    for (boxes in BoxList) {
                        println("id: ${boxes.id}, idbox: ${boxes.idbox}, Число: ${boxes.digit}, Комментарий: ${boxes.comment}, X: ${boxes.positionX}, Y: ${boxes.positionY}")
                    }
                }
            }

            OpenCalcActivityButton()
            DraggableBoxList(dragboxesDao, calcField, number, com)
        }
    }
    private fun deleteAllData() {
        runBlocking {
            withContext(Dispatchers.IO) {
                dragboxesDao.deleteAllDragBoxes()
            }
        }
    }
}







@Composable
fun showAlertDialogWithFourButtons(
    number1: String,
    number2: String,
    onButton1Click: () -> Unit,
    onButton2Click: () -> Unit,
    onButton3Click: () -> Unit,
    onButton4Click: () -> Unit,
    onSwapClick: () -> Unit,
    onCancelClick: () -> Unit
) {

    AlertDialog(
        onDismissRequest = {},
        title = { Text("") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = number1,
                        fontSize = calculateFontSize(number1),
                        modifier = Modifier.padding(start = 30.dp)
                    )
                    Button(
                        onClick = { onSwapClick() },
                        modifier = Modifier.padding(8.dp).align(Alignment.CenterVertically)
                    ) {
                        Text("Swap")
                    }
                    Text(
                        text = number2,
                        fontSize = calculateFontSize(number2),
                        modifier = Modifier.padding(end = 30.dp)
                    )
                }
            }
        },
        buttons = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onButton1Click() },
                        modifier = Modifier
                            .padding(8.dp)
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        Text("+")
                    }
                    Button(
                        onClick = { onButton2Click() },
                        modifier = Modifier
                            .padding(8.dp)
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        Text("-")
                    }
                    Button(
                        onClick = { onButton3Click() },
                        modifier = Modifier
                            .padding(8.dp)
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        Text("*")
                    }
                    Button(
                        onClick = { onButton4Click() },
                        modifier = Modifier
                            .padding(8.dp)
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        Text("/")
                    }
                }
                OutlinedButton(
                    onClick = { onCancelClick() },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Отмена")
                }
            }
        },
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White,
        contentColor = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    )
}




fun calculateFontSize(text: String): TextUnit {
    val maxLength = 2
    val defaultFontSize = 50.sp

    return if (text.length > maxLength) {
        val scaleFactor = maxLength.toFloat() / text.length.toFloat()
        (defaultFontSize * scaleFactor)
    } else {
        defaultFontSize
    }
}





data class DraggableBoxListData(val boxes: List<DraggableBoxData>, val boxPositions: Map<String, Offset>)

@Composable
fun DraggableBoxList(dragboxesDao: DragBoxesDao, calcField: CalcFieldDao, newBoxNum: String, com: String): DraggableBoxListData {
    val boxes = remember { mutableStateListOf<DraggableBoxData>() }
    val boxPositions = remember { mutableStateMapOf<String, Offset>() }
    val buttonClicks = remember { mutableStateOf(0) }
    val buttonClicks1 = remember { mutableStateOf(0) }
    var buttonClicks2 = remember { mutableStateOf("") }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val isOverlap = remember { mutableStateOf(false) } // Флаг для отслеживания перекрытия
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("56") }
    var x by remember { mutableStateOf("") }
    var y by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }




    LaunchedEffect(buttonClicks.value) {
        withContext(Dispatchers.IO) {
            if (buttonClicks.value==0 && !newBoxNum.isNullOrEmpty() && newBoxNum.toIntOrNull()!=null){
                val newBox = DraggableBoxData(UUID.randomUUID().toString(), newBoxNum, "")
                boxes.add(newBox)
                boxPositions[newBox.id] = Offset.Zero
                val newbox =
                    DragBoxes(0, boxes.last().id, boxes.last().text, com, boxPositions.entries.lastOrNull()?.value?.x.toString(), boxPositions.entries.lastOrNull()?.value?.y.toString())
                dragboxesDao.insertDragBoxes(newbox)
            }
            if (buttonClicks.value>0){
                val newbox =
                    DragBoxes(0, boxes.last().id, boxes.last().text, boxes.last().comment, boxPositions.entries.lastOrNull()?.value?.x.toString(), boxPositions.entries.lastOrNull()?.value?.y.toString())
                dragboxesDao.insertDragBoxes(newbox)
            }


            val dataList = dragboxesDao.getAllDragBoxes()
            boxes.clear()
            boxPositions.clear()
            boxes.addAll(dataList.map { DraggableBoxData(it.idbox.toString(), it.digit, it.comment)})
            boxPositions.putAll(dataList.associate { it.idbox.toString() to Offset(it.positionX.toFloat(), it.positionY.toFloat()) })
        }
    }

    LaunchedEffect(buttonClicks.value) {
        withContext(Dispatchers.IO) {
            calcField.insertCalcField(CalcField(0, ""))
        }
    }

    LaunchedEffect(buttonClicks1.value) {
        withContext(Dispatchers.IO) {
            if (buttonClicks1.value!=0) {
                val BoxList = dragboxesDao.getAllDragBoxes()
                if (BoxList.isNotEmpty()) {
                    dragboxesDao.deleteAllDragBoxes()
                    for (i in 0 until boxes.size) {
                        val boxx = boxes[i]
                        val poss = boxPositions[boxx.id]
                        if (poss != null && i < BoxList.size) {
                            dragboxesDao.insertDragBoxes(
                                DragBoxes(
                                    BoxList[i].id,
                                    boxx.id,
                                    boxx.text,
                                    boxx.comment,
                                    poss.x.toString(),
                                    poss.y.toString()
                                )
                            )
                        }
                    }
                }
            }
        }
    }






    Column(modifier = Modifier.fillMaxSize()) {
        var newBoxText by remember { mutableStateOf("0") }

        Text(
            text = "Создать число",
            fontSize = 30.sp,
            modifier = Modifier
                .clickable {
                    val newBox = DraggableBoxData(UUID.randomUUID().toString(), newBoxText, "")
                    boxes.add(newBox)
                    boxPositions[newBox.id] = Offset.Zero
                    newBoxText = "0"
                    buttonClicks.value++
                }
                .padding(16.dp)
                .align(Alignment.End)

        )



        boxes.forEachIndexed { index, box ->
            key(box) {
                DraggableBox(
                    box = box,
                    position = boxPositions[box.id] ?: (Offset.Zero),
                    onTextEntered = { enteredText ->
                        box.text = enteredText
                        buttonClicks1.value++
                    },
                    onCommentEntered = { enteredText ->
                        box.comment = enteredText
                        buttonClicks1.value++
                    },
                    onDeleteClicked = {
                        boxes.remove(box)
                        boxPositions.remove(box.id)
                        buttonClicks1.value++
                    },
                    onPositionChanged = { newPosition ->
                        boxPositions[box.id] = newPosition
                    },
                    onDragFinished = { newPosition ->
                        boxPositions[box.id] = newPosition

                        val positionX = newPosition.x.toString()
                        val positionY = newPosition.y.toString()

                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val BoxList = dragboxesDao.getAllDragBoxes()
                                var id = 0L
                                for (boxes in BoxList) {
                                    if (boxes.idbox==box.id){
                                        id = boxes.id
                                    }
                                }

                                if (id != null) {
                                    val updatedDragBoxes = DragBoxes(id, box.id, box.text, box.comment, positionX, positionY)
                                    dragboxesDao.updateDragBoxes(updatedDragBoxes)
                                } else {
                                    // Обработка случая, когда id равно null
                                }

                            }
                        }

                        println("Позиции блоков:")
                        boxPositions.forEach { (boxId, position) ->
                            println("ID блока: $boxId, Позиция: $position")
                        }
                        if (checkForOverlap(box.id, newPosition, boxPositions)) {
                            isOverlap.value = true // Установка флага перекрытия
                        }
                        println("- - - - - -")
                        buttonClicks1.value++
                    },
                    onGoToCalc = {buttonClicks2.value=box.id}
                )
            }
        }



        // Вызов AlertDialog на основе флага перекрытия
        if (isOverlap.value) {
            val overlappedBoxes = boxes.filter { checkForOverlap(it.id, boxPositions[it.id] ?: Offset.Zero, boxPositions) }
            val number1 = overlappedBoxes.getOrNull(0)?.text ?: ""
            val number2 = overlappedBoxes.getOrNull(1)?.text ?: ""

            val comment1 = overlappedBoxes.getOrNull(0)?.comment ?: ""
            val comment2 = overlappedBoxes.getOrNull(1)?.comment ?: ""

            showAlertDialogWithFourButtons(
                number1 = number1,
                number2 = number2,
                onButton1Click = {
                    val sum = overlappedBoxes.sumBy { it.text.toIntOrNull() ?: 0 }
                    println("Сумма: $sum")

                    if (overlappedBoxes.size > 1) {
                        val remainingBox = overlappedBoxes.first()
                        val removedBoxes = overlappedBoxes.filter { it != remainingBox }
                        val removedBoxIds = removedBoxes.map { it.id }

                        removedBoxIds.forEach { removedBoxId ->
                            boxPositions.remove(removedBoxId)
                        }

                        boxes.removeAll(removedBoxes)

                        remainingBox.text = sum.toString()
                        remainingBox.comment = when {
                            comment1.isEmpty() && comment2.isEmpty() -> ""
                            comment1.isEmpty() -> comment2
                            comment2.isEmpty() -> comment1
                            else -> "$comment1+$comment2"
                        }

                    }
                    buttonClicks1.value++
                    isOverlap.value = false
                },
                onButton2Click = {
                    val result = overlappedBoxes.foldIndexed(0) { index, acc, box ->
                        val value = box.text.toIntOrNull() ?: 0
                        if (index == 0) value else acc - value
                    }
                    println("Вычитание: $result")

                    if (overlappedBoxes.size > 1) {
                        val remainingBox = overlappedBoxes.first()
                        val removedBoxes = overlappedBoxes.filter { it != remainingBox }
                        val removedBoxIds = removedBoxes.map { it.id }


                        removedBoxIds.forEach { removedBoxId ->
                            boxPositions.remove(removedBoxId)
                        }


                        boxes.removeAll(removedBoxes)


                        remainingBox.text = result.toString()
                        remainingBox.comment = when {
                            comment1.isEmpty() && comment2.isEmpty() -> ""
                            comment1.isEmpty() -> comment2
                            comment2.isEmpty() -> comment1
                            else -> "$comment2-$comment1"
                        }
                    }
                    buttonClicks1.value++
                    isOverlap.value = false
                },
                onButton3Click = {
                    val product = overlappedBoxes.fold(BigInteger.ONE) { acc, box ->
                        val value = box.text.toBigIntegerOrNull() ?: BigInteger.ZERO
                        acc * value
                    }

                    println("Произведение: $product")

                    if (overlappedBoxes.size > 1) {
                        val remainingBox = overlappedBoxes.first()
                        val removedBoxes = overlappedBoxes.filter { it != remainingBox }
                        val removedBoxIds = removedBoxes.map { it.id }


                        removedBoxIds.forEach { removedBoxId ->
                            boxPositions.remove(removedBoxId)
                        }


                        boxes.removeAll(removedBoxes)

                        remainingBox.text = product.toString()
                        remainingBox.comment = when {
                            comment1.isEmpty() && comment2.isEmpty() -> ""
                            comment1.isEmpty() -> comment2
                            comment2.isEmpty() -> comment1
                            else -> "$comment1*$comment2"
                        }
                    }
                    buttonClicks1.value++
                    isOverlap.value = false
                },

                onButton4Click = {
                    val firstValue = overlappedBoxes.getOrNull(0)?.text?.toDoubleOrNull() ?: 0.0
                    val secondValue = overlappedBoxes.getOrNull(1)?.text?.toDoubleOrNull() ?: 0.0

                    if (secondValue != 0.0) {
                        val result = firstValue / secondValue
                        println("Деление: $result")

                        if (overlappedBoxes.size > 1) {
                            val remainingBox = overlappedBoxes.first()
                            val removedBoxes = overlappedBoxes.filter { it != remainingBox }
                            val removedBoxIds = removedBoxes.map { it.id }


                            removedBoxIds.forEach { removedBoxId ->
                                boxPositions.remove(removedBoxId)
                            }


                            boxes.removeAll(removedBoxes)


                            remainingBox.text = result.toString()
                            remainingBox.comment = when {
                                comment1.isEmpty() && comment2.isEmpty() -> ""
                                comment1.isEmpty() -> comment2
                                comment2.isEmpty() -> comment1
                                else -> "$comment1/$comment2"
                            }
                        }

                    } else {
                        println("Деление на ноль не допускается")
                    }
                    buttonClicks1.value++
                    isOverlap.value = false
                }
                ,
                onCancelClick = { isOverlap.value = false },
                onSwapClick = {

                    if (overlappedBoxes.size == 2) {
                        val firstBox = overlappedBoxes[0]
                        val secondBox = overlappedBoxes[1]
                        val tempText = firstBox.text
                        firstBox.text = secondBox.text
                        secondBox.text = tempText

                        val tempComment = firstBox.comment
                        firstBox.comment = secondBox.comment
                        secondBox.comment = tempComment


                        val firstBoxPosition = boxPositions[firstBox.id]
                        val secondBoxPosition = boxPositions[secondBox.id]
                        if (firstBoxPosition != null && secondBoxPosition != null) {
                            boxPositions[firstBox.id] = secondBoxPosition
                            boxPositions[secondBox.id] = firstBoxPosition
                        }
                    }
                    buttonClicks1.value++
                },
            )
        }
    }
    return DraggableBoxListData(boxes, boxPositions)
}



@Composable
fun DraggableBox(
    box: DraggableBoxData,
    position: Offset,
    onTextEntered: (String) -> Unit,
    onCommentEntered: (String) -> Unit,
    onDeleteClicked: () -> Unit,
    onPositionChanged: (Offset) -> Unit,
    onDragFinished: (Offset) -> Unit,
    onGoToCalc: (Offset) -> Unit
) {
    var offsetX by remember { mutableStateOf(position.x) }
    var offsetY by remember { mutableStateOf(position.y) }

    val showDialog = remember { mutableStateOf(false) }

    val boxWidth = max(calculateBoxWidth(box.text.length), calculateBoxWidth(box.comment.length))
    val boxHeight = calculateBoxHeight(box.text.length)*2

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        onDragFinished(Offset(offsetX, offsetY))
                    },
                    onDragCancel = { }
                ) { _, dragAmount ->
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    onPositionChanged(Offset(offsetX, offsetY))
                }
            }
            .size(width = boxWidth, height = boxHeight)
            .clickable {
                showDialog.value = true
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Text(
                text = box.text,
                modifier = Modifier
                    .size(width = 100.dp, height = 50.dp),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = box.comment,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,

            )
        }
        if (showDialog.value) {
            var textent = showTextDialog(
                initialText = box.text,
                initialComment = box.comment,
                onTextEntered = {
                    onTextEntered(it)
                    showDialog.value = false
                },
                onCommentEntered = {
                    onCommentEntered(it)
                    showDialog.value = false
                },
                onDismiss = {
                    showDialog.value = false
                },
                onDeleteClicked = onDeleteClicked
            )
        }
    }
}




fun checkForOverlap(
    boxId: String,
    newPosition: Offset,
    boxPositions: Map<String, Offset>
): Boolean {
    boxPositions.forEach { (otherBoxId, otherPosition) ->
        if (boxId != otherBoxId) {
            val distance = calculateDistance(newPosition, otherPosition)
            if (distance <= 250) {
                return true
            }
        }
    }
    return false
}

fun calculateDistance(point1: Offset, point2: Offset): Float {
    val dx = point2.x - point1.x
    val dy = point2.y - point1.y
    return sqrt(dx * dx + dy * dy)
}







@Composable
fun calculateBoxWidth(textLength: Int): Dp {
    val minWidth = 120.dp
    val maxWidth = 300.dp
    val textWidth = (textLength * 30).dp

    return min(max(textWidth, minWidth), maxWidth)
}


@Composable
fun calculateBoxHeight(textLength: Int): Dp {
    val minHeight = 70.dp
    val maxHeight = 120.dp
    val textHeight = (textLength * 2).dp

    return min(max(textHeight, minHeight), maxHeight)
}

data class DraggableBoxData(val id: String, var text: String, var comment: String){
    var isChanged: Boolean by mutableStateOf(false)
}


@Composable
fun showTextDialog(initialText: String, onTextEntered: (String) -> Unit, onCommentEntered: (String) -> Unit, onDismiss: () -> Unit, onDeleteClicked: () -> Unit, initialComment: String): String {
    var text by remember { mutableStateOf(initialText) }
    var commenttext by remember { mutableStateOf(initialComment) }
    val dialog = remember { mutableStateOf(true) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
        }
    }

    if (dialog.value) {
        AlertDialog(
            onDismissRequest = { dialog.value = false; onDismiss() },
            title = {
                Text("Введите число", fontSize = 18.sp)
            },
            text = {
                Column {
                TextField(
                    value = text,
                    onValueChange = { newText ->
                        val filteredText = if (newText.isNotEmpty()) {
                            if (newText.first() == '.' || (newText.first() == '0' && newText.length > 1 && newText[1] != '.')) {
                                newText.drop(1).trimStart('0')
                            } else {
                                newText.filterIndexed { index, char ->
                                    index == 0 || (char.isDigit() || (char == '.' && newText.indexOf('.') == index))
                                }
                            }
                        } else {
                            newText
                        }
                        text = filteredText
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 30.sp)
                )

                TextField(
                    value = commenttext,
                    onValueChange = { newText ->
                        commenttext = newText
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 30.sp)
                )
                    }
            },
            confirmButton = {
                Button(onClick = {
                    val enteredText = if (text.isEmpty()) "0" else text
                    onTextEntered(enteredText)
                    onCommentEntered(commenttext)
                    dialog.value = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Row {
                    Button(onClick = {
                        dialog.value = false
                        val enteredText = if (text.isEmpty()) "0" else text
                        onTextEntered(enteredText)
                        val number = enteredText
                        val commentToCalct = commenttext
                        val intent = Intent(context, EmptyActivity::class.java).apply {
                            putExtra("NUMBER_EXTRA", "$number#$commentToCalct")
                        }
                        launcher.launch(intent)
                        onDeleteClicked()
                    }) {
                        Text("Перенести в калькулятор")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onDeleteClicked()
                        dialog.value = false
                        onDismiss()
                    }) {
                        Text("Удалить")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        dialog.value = false
                        onDismiss()
                    }) {
                        Text("Отмена")
                    }

                }
            }
        )
    }
    return text
}





@Composable
fun OpenCalcActivityButton() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // plug
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Button(
            onClick = {
                val number = ""
                val intent = Intent(context, EmptyActivity::class.java).apply {
                    putExtra("NUMBER_EXTRA", number)
                }
                launcher.launch(intent)
            },
            modifier = Modifier
                .padding(end = 16.dp)
                .size(80.dp),
            shape = CircleShape
        ) {
            Text(">", fontSize = 45.sp)
        }
    }
}






