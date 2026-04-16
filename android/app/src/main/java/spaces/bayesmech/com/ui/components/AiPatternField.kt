package spaces.bayesmech.com.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

private const val GridSize = 19

@Composable
fun AiPatternField(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
) {
    val random = remember { Random(17) }
    val patternIndex = remember { mutableIntStateOf(0) }
    val greyDot = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val blackDot = Color.Black
    val dotTargets = remember {
        mutableStateListOf<Float>().apply {
            addAll(generateTargetPattern(patternIndex.intValue, random))
        }
    }

    LaunchedEffect(isAnimating) {
        while (isAnimating) {
            patternIndex.intValue += 1
            val nextPattern = generateTargetPattern(patternIndex.intValue, random)
            dotTargets.indices.forEach { index ->
                dotTargets[index] = nextPattern[index]
            }
            delay(900)
        }
    }

    val animatedDots = dotTargets.map { target ->
        animateFloatAsState(
            targetValue = if (isAnimating) target else target * 0.65f,
            animationSpec = tween(durationMillis = 720),
            label = "ai-dot",
        ).value
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
                shape = RoundedCornerShape(36.dp),
            )
            .padding(30.dp),
    ) {
        Canvas(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x11FFFFFF),
                        Color(0x00FFFFFF),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
                size = Size(size.width, size.height),
            )

            val cell = size.width / GridSize
            val radius = min(cell, size.height / GridSize) * 0.18f
            for (row in 0 until GridSize) {
                for (col in 0 until GridSize) {
                    val amount = animatedDots[row * GridSize + col]
                    drawCircle(
                        color = lerpColor(greyDot, blackDot, amount),
                        radius = radius + (amount * radius * 0.34f),
                        center = Offset(
                            x = (col + 0.5f) * cell,
                            y = (row + 0.5f) * cell,
                        ),
                    )
                }
            }
        }
    }
}

private fun generateTargetPattern(
    tick: Int,
    random: Random,
): List<Float> {
    val seed = tick % 6
    val offsets = listOf(-1, 0, 1)
    val dx = offsets[random.nextInt(offsets.size)]
    val dy = offsets[random.nextInt(offsets.size)]
    val selectedShape = when (seed) {
        0 -> birdShape(dx, dy)
        1 -> goatShape(dx, dy)
        2 -> fishShape(dx, dy)
        3 -> cometShape(dx, dy)
        4 -> wingShape(dx, dy)
        else -> spiralShape(dx, dy)
    }

    return List(GridSize * GridSize) { index ->
        val row = index / GridSize
        val col = index % GridSize
        val key = row to col
        when {
            key in selectedShape -> random.nextFloat() * 0.28f + 0.72f
            random.nextFloat() < 0.05f -> random.nextFloat() * 0.26f + 0.30f
            else -> random.nextFloat() * 0.12f
        }
    }
}

private fun birdShape(dx: Int, dy: Int): Set<Pair<Int, Int>> = asciiShape(
    rows = listOf(
        "...................",
        "...................",
        ".........#.........",
        ".......#####.......",
        ".....#########.....",
        "...######.######...",
        "..#####.....#####..",
        "...###.......###...",
        "....###.....###....",
        ".....#########.....",
        "......#######......",
        ".......#####.......",
        "........###........",
        ".......##.##.......",
        "......##...##......",
        ".....##.....##.....",
        "...................",
        "...................",
        "...................",
    ),
    dx = dx,
    dy = dy,
)

private fun goatShape(dx: Int, dy: Int): Set<Pair<Int, Int>> = asciiShape(
    rows = listOf(
        "...................",
        "...................",
        "......##.....##....",
        "......###...###....",
        ".......#######.....",
        "......#########....",
        ".....###########...",
        ".....####..#####...",
        "....#####...####...",
        "...#############...",
        "...############....",
        "....##########.....",
        "....##..##..##.....",
        "...##...##...##....",
        "...##...##...##....",
        "....#...##...#.....",
        "...................",
        "...................",
        "...................",
    ),
    dx = dx,
    dy = dy,
)

private fun fishShape(dx: Int, dy: Int): Set<Pair<Int, Int>> = asciiShape(
    rows = listOf(
        "...................",
        "...................",
        "...................",
        ".......######......",
        ".....##########....",
        "...##############..",
        "..######....######.",
        ".#####........#####",
        "..######....######.",
        "...##############..",
        ".....##########....",
        ".......######......",
        ".........###.......",
        "........#####......",
        ".......##...##.....",
        "...................",
        "...................",
        "...................",
        "...................",
    ),
    dx = dx,
    dy = dy,
)

private fun cometShape(dx: Int, dy: Int): Set<Pair<Int, Int>> = asciiShape(
    rows = listOf(
        "...................",
        "...................",
        "..#................",
        "...##..............",
        "....###............",
        ".....####..........",
        "......#####........",
        ".......######......",
        "........#######....",
        ".........########..",
        "..........#########",
        "...........#######.",
        "............#####..",
        ".............###...",
        "..............##...",
        "...............#...",
        "...................",
        "...................",
        "...................",
    ),
    dx = dx,
    dy = dy,
)

private fun wingShape(dx: Int, dy: Int): Set<Pair<Int, Int>> = asciiShape(
    rows = listOf(
        "...................",
        "...................",
        ".....###...........",
        "....######.........",
        "...#########.......",
        "..###########......",
        "..#####..#####.....",
        "...###....#####....",
        "....##.....#####...",
        "....###.....#####..",
        "...#####.....###...",
        "..#######....##....",
        ".#########..###....",
        "..############.....",
        "...##########......",
        ".....######........",
        "...................",
        "...................",
        "...................",
    ),
    dx = dx,
    dy = dy,
)

private fun spiralShape(dx: Int, dy: Int): Set<Pair<Int, Int>> = buildSet {
    val center = 9f
    repeat(150) { step ->
        val angle = step * 0.28f
        val radius = 1.2f + (step / 20f)
        val col = (center + kotlin.math.cos(angle) * radius).toInt() + dx
        val row = (center + kotlin.math.sin(angle) * radius).toInt() + dy
        if (row in 0 until GridSize && col in 0 until GridSize) {
            add(row to col)
        }
    }
}

private fun asciiShape(
    rows: List<String>,
    dx: Int,
    dy: Int,
): Set<Pair<Int, Int>> = buildSet {
    rows.forEachIndexed { rowIndex, row ->
        row.forEachIndexed { colIndex, char ->
            if (char == '#') {
                val shiftedRow = rowIndex + dy
                val shiftedCol = colIndex + dx
                if (shiftedRow in 0 until GridSize && shiftedCol in 0 until GridSize) {
                    add(shiftedRow to shiftedCol)
                }
            }
        }
    }
}

private fun DrawScope.lerpColor(
    start: Color,
    end: Color,
    fraction: Float,
): Color = Color(
    red = start.red + (end.red - start.red) * fraction,
    green = start.green + (end.green - start.green) * fraction,
    blue = start.blue + (end.blue - start.blue) * fraction,
    alpha = start.alpha + (end.alpha - start.alpha) * fraction,
)
