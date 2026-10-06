package com.kito.feature.attendance.presentation.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kito.core.designsystem.UIColors
import com.kito.core.designsystem.meshGradient
import com.kito.feature.attendance.domain.model.Attendance
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class,
    ExperimentalHazeApi::class
)
@Composable
fun AttendanceBarCard(
    attendance: List<Attendance>,
    onNavigate: () -> Unit,
    onClick: () -> Unit,
    sapLoggedIn: Boolean,
) {
    val isInPreview = LocalInspectionMode.current
    val colors = UIColors()
    val hazeState = rememberHazeState()
    val hazeNotLoggedIn = rememberHazeState()
    val meshColors = listOf(
        Color(0xFF77280F).copy(alpha = 0.82f), // burnt orange
        Color(0xFF753107).copy(alpha = 0.82f), // amber-700
        Color(0xFF62290A).copy(alpha = 0.82f), // amber-800
        Color(0xFF46180C).copy(alpha = 0.82f), // deep orange-brown

        // 🔥 new additions (subtle!)
        Color(0xFFA14B09).copy(alpha = 0.70f), // muted yellow (amber-500 toned down)
        Color(0xFF6B1414).copy(alpha = 0.75f), // brick red (not crimson)
    )
    val animatedPointMid = remember { Animatable(.8f) }
    val animatedPointTop = remember { Animatable(.8f) }
    val meshColorAnimators = remember {
        List(15) { index ->
            Animatable(meshColors[index % meshColors.size])
        }
    }
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val screenWidthDp = with(density) {
        windowInfo.containerSize.width.toDp()
    }

    val minWidth = 360.dp
    val maxWidth = 1000.dp

    val progress = ((screenWidthDp - minWidth) / (maxWidth - minWidth))
        .coerceIn(0f, 1f)

    val dynamicAspectRatio = lerp(0.25f, 0.35f, progress)

    if (!isInPreview) {
        LaunchedEffect(Unit) {
            meshColorAnimators.forEachIndexed { i, anim ->
                launch {
                    val random = Random(i * 97)
                    while (true) {
                        val nextColor = meshColors[random.nextInt(meshColors.size)]
                        anim.animateTo(
                            targetValue = nextColor,
                            animationSpec = tween(
                                durationMillis = random.nextInt(1800, 4200),
                                easing = LinearOutSlowInEasing
                            )
                        )
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            launch {
                while (true) {
                    animatedPointMid.animateTo(
                        targetValue = 0.3f,
                        animationSpec = tween(
                            durationMillis = 4000,
                            easing = LinearOutSlowInEasing
                        )
                    )
                    animatedPointMid.animateTo(
                        targetValue = 0.7f,
                        animationSpec = tween(
                            durationMillis = 4000,
                            easing = LinearOutSlowInEasing
                        )
                    )
                }
            }

            launch {
                while (true) {
                    animatedPointTop.animateTo(
                        targetValue = 0.2f,
                        animationSpec = tween(
                            durationMillis = 4000,
                            easing = LinearEasing
                        )
                    )
                    animatedPointTop.animateTo(
                        targetValue = 0.8f,
                        animationSpec = tween(
                            durationMillis = 4000,
                            easing = LinearEasing
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(26.dp))
            .clickable(
                onClick = {
                    onNavigate()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.hazeSource(hazeNotLoggedIn)
        ) {
            Box(
                modifier = Modifier.hazeSource(hazeState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .meshGradient(
                            points = listOf(

                                // ───── TOP ROW ─────
                                listOf(
                                    Offset(0f, 0f) to meshColorAnimators[0].value,
                                    Offset(0.25f, 0f) to meshColorAnimators[1].value,
                                    Offset(0.5f, 0f) to meshColorAnimators[2].value,
                                    Offset(0.75f, 0f) to meshColorAnimators[3].value,
                                    Offset(1f, 0f) to meshColorAnimators[4].value,
                                ),

                                // ───── MIDDLE ROW (curved glow band) ─────
                                listOf(
                                    Offset(-0.05f, 0.55f) to meshColorAnimators[5].value,
                                    Offset(
                                        0.2f,
                                        animatedPointTop.value
                                    ) to meshColorAnimators[6].value,
                                    Offset(0.5f, 0.6f) to meshColorAnimators[7].value,
                                    Offset(
                                        0.8f,
                                        animatedPointMid.value
                                    ) to meshColorAnimators[8].value,
                                    Offset(1.05f, 0.55f) to meshColorAnimators[9].value,
                                ),

                                // ───── BOTTOM ROW (independent animation per point) ─────
                                listOf(
                                    Offset(0f, 1f) to meshColorAnimators[10].value,
                                    Offset(0.25f, 1f) to meshColorAnimators[11].value,
                                    Offset(0.5f, 1f) to meshColorAnimators[12].value,
                                    Offset(0.75f, 1f) to meshColorAnimators[13].value,
                                    Offset(1f, 1f) to meshColorAnimators[14].value,
                                ),
                            ),
                            resolutionX = 30,
                        )
                ) {}
            }
            val subjects = if (sapLoggedIn) attendance else sampleAttendanceEntities
            val subjectCount = subjects.size

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val horizontalPadding = 24.dp // 12.dp start + 12.dp end
                val spacing = 12.dp

                if (subjectCount >= 6 || subjectCount == 0) {
                    LazyRow(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 12.dp,
                            alignment = Alignment.CenterHorizontally
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(subjects) { item ->
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AttendanceBarGraph(
                                    attendance = item,
                                    hazeState = hazeState,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(dynamicAspectRatio)
                                        .clip(RoundedCornerShape(16.dp))
                                        .weight(1f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.subjectName.toAbbreviation(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyLargeEmphasized,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        if (sapLoggedIn && attendance.isEmpty()) {
                            item {
                                Text(
                                    text = "Please Set Correct Year and Term in settings",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyLargeEmphasized,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .padding(horizontal = 40.dp)
                                )
                            }
                        }
                    }
                } else {
                    val contentWidth = availableWidth - horizontalPadding
                    val calculatedWidth = (contentWidth - (spacing * (subjectCount - 1))) / subjectCount
                    val targetTubeWidth = calculatedWidth.coerceAtLeast(36.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        subjects.forEach { item ->
                            AttendanceTubeItem(
                                attendance = item,
                                hazeState = hazeState,
                                tubeWidth = targetTubeWidth,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
        if (!sapLoggedIn) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(
                        shape = RoundedCornerShape(26.dp)
                    )
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }
                    .hazeEffect(state = hazeNotLoggedIn, style = HazeMaterials.ultraThin()) {
                        blurRadius = 15.dp
                        noiseFactor = 0.05f
                        inputScale = HazeInputScale.Auto
                    }
            ){
                Button(
                    onClick = {
                        onClick()
                    },
                    modifier = Modifier.align(Alignment.Center),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.progressAccent,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Text(
                        text = "Connect to sap",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelMediumEmphasized
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class, ExperimentalHazeApi::class)
@Composable
fun AttendanceTubeItem(
    attendance: Attendance,
    hazeState: HazeState,
    tubeWidth: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                    blurRadius = 15.dp
                    noiseFactor = 0.05f
                    inputScale = HazeInputScale.Auto
                    alpha = 0.6f
                }
        ) {
            WaterAnimation(
                text = attendance.percentage.toInt().toString() + "%",
                waterLevel = attendance.percentage.toFloat() / 100f,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = attendance.subjectName.toAbbreviation(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyLargeEmphasized,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalHazeApi::class)
@Composable
fun AttendanceBarGraph(
    attendance: Attendance,
    modifier: Modifier = Modifier,
    hazeState: HazeState
) {
    Box(
        modifier = modifier
            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                blurRadius = 15.dp
                noiseFactor = 0.05f
                inputScale = HazeInputScale.Auto
                alpha = 0.6f
            },
    ){
        WaterAnimation(
            text = attendance.percentage.toInt().toString() + "%",
            waterLevel = attendance.percentage.toFloat() / 100f,
        )
    }
}

@Composable
fun WaterAnimation(
    modifier: Modifier = Modifier.fillMaxSize(),
    text: String = "10%",
    waterColor: Color = UIColors().accentOrangeStart,
    waterLevel: Float
) {
    val isInPreview = LocalInspectionMode.current
    var waterLevelPercentage by remember { mutableFloatStateOf(1f) }

    val textMeasurer = rememberTextMeasurer()

    val animatedFraction by animateFloatAsState(
        targetValue = waterLevelPercentage.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = 1500,
            easing = LinearOutSlowInEasing
        ),
        label = "waterLevel"
    )

    var waveShift by remember { mutableFloatStateOf(0f) }

    val waterBrush = Brush.verticalGradient(
        0.0f to Color(0xFFFFF6E0),
        0.18f to Color(0xFFFFD37A),
        0.40f to Color(0xFFFF9E2B),
        0.70f to Color(0xFFC85E00),
        1.0f to Color(0xFF4A1F00)
    )

    LaunchedEffect(Unit, waterLevel) {
        waterLevelPercentage = waterLevel
        if (!isInPreview) {
            while (true) {
                withFrameNanos {
                    waveShift += 0.03f
                }
            }
        }
    }

    val phases = remember {
        List(4) { Random.nextFloat() * 2f * PI.toFloat() }
    }

    val density = LocalDensity.current

    Canvas(
        modifier.graphicsLayer(alpha = 0.99f)
    ) {
        val width = size.width
        val height = size.height

        val waterLevel = height * (1f - animatedFraction)

        val isExtreme = animatedFraction <= 0.01f || animatedFraction >= 0.99f

        val baseAmplitude = if (isExtreme) 0f else width * 0.05f
        val detailAmplitude = if (isExtreme) 0f else width * 0.02f

        val path = Path().apply {

            for (x in 0..width.toInt()) {

                val progress = x / width
                val desiredWaveLengthPx = width * 2.8f
                val waveCount = width / desiredWaveLengthPx
                val angle = progress * waveCount * 2f * PI

                // 🌊 Main wave
                val mainWave =
                    sin(angle + waveShift + phases[0]) * baseAmplitude

                // 🌊 Secondary ripple
                val ripple1 =
                    sin(angle * 2f + waveShift * 1.5f + phases[1]) * detailAmplitude

                // 🌊 Micro turbulence
                val ripple2 =
                    sin(angle * 3f - waveShift * 2f + phases[2]) * (detailAmplitude * 0.6f)

                val y = waterLevel + mainWave + ripple1 + ripple2

                if (x == 0) moveTo(x.toFloat(), y.toFloat())
                else lineTo(x.toFloat(), y.toFloat())
            }

            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = path,
            brush = waterBrush,
            style = Fill
        )

        val crestPath = Path().apply {
            for (x in 0..width.toInt()) {

                val progress = x / width
                val desiredWaveLengthPx = width * 2.8f
                val waveCount = width / desiredWaveLengthPx
                val angle = progress * waveCount * 2f * PI

                val mainWave =
                    sin(angle + waveShift + phases[0]) * baseAmplitude

                val ripple1 =
                    sin(angle * 2f + waveShift * 1.5f + phases[1]) * detailAmplitude

                val ripple2 =
                    sin(angle * 3f - waveShift * 2f + phases[2]) * (detailAmplitude * 0.6f)

                val y = waterLevel + mainWave + ripple1 + ripple2

                if (x == 0) moveTo(x.toFloat(), y.toFloat())
                else lineTo(x.toFloat(), y.toFloat())
            }
        }
        if (!isExtreme) {
            drawPath(
                path = crestPath,
                color = Color(0xFFFFE2A8).copy(alpha = 0.18f),
                style = Stroke(width = 20f, cap = StrokeCap.Round)
            )
            drawPath(
                path = crestPath,
                color = Color(0xFFFFF4CC),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
        }

        val dynamicFontSize = with(density) {
            (height * 0.09f).toSp()
        }

        val textLayoutResult = textMeasurer.measure(
            AnnotatedString(text),
            style = TextStyle(
                fontSize = dynamicFontSize,
                fontWeight = FontWeight.Bold,
                color = waterColor
            )
        )

        val textX = (width - textLayoutResult.size.width) / 2
        val textY = (height - textLayoutResult.size.height) / 2

        drawText(
            textLayoutResult,
            topLeft = Offset(textX, textY),
            blendMode = BlendMode.Xor
        )
    }
}

fun String.toAbbreviation(): String {
    return this
        .trim()
        .split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
        .map { it.first().uppercaseChar() }
        .joinToString("")
}
