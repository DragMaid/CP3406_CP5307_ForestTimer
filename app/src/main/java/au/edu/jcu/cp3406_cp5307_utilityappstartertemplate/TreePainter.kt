package au.edu.jcu.cp3406_cp5307_utilityappstartertemplate

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

// ponytail: Dynamic Canvas drawing with mathematical sway and particle calculations. Zero image files.

@Composable
fun TreeCanvas(
    species: TreeSpecies,
    stage: TreeStage,
    weather: WeatherCondition = WeatherCondition.SUNNY,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TreeSway")

    // Dynamic sway specs based on weather
    val swayDuration = when (weather) {
        WeatherCondition.WINDY -> 900
        WeatherCondition.STORM -> 700
        else -> 2500
    }
    val swayMaxAngle = when (weather) {
        WeatherCondition.WINDY -> 6f
        WeatherCondition.STORM -> 9f
        else -> 1.5f
    }
    
    // Smooth swaying animation to bring the trees to life
    val swayAngle by infiniteTransition.animateFloat(
        initialValue = -swayMaxAngle,
        targetValue = swayMaxAngle,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = swayDuration, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    // Flowing petals animation for Cherry Blossom
    val petalProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "petals"
    )

    // Falling rain animation
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )

    // Occasional thunder flash for storms
    val thunderAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 8000
                0.0f at 0
                0.0f at 5000
                0.8f at 5100
                0.0f at 5200
                0.9f at 5300
                0.0f at 5500
                0.0f at 8000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "thunder"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val groundY = height - 24.dp.toPx()
        val centerX = width / 2f

        // --- LAYER 1: SKY (Sun and Clouds) ---
        if (weather == WeatherCondition.SUNNY) {
            val sunRadius = 14.dp.toPx()
            val sunCenter = Offset(width - 24.dp.toPx(), 24.dp.toPx())
            drawCircle(
                color = Color(0xFFFBC02D),
                radius = sunRadius,
                center = sunCenter
            )
            for (i in 0 until 8) {
                val angle = i * 45
                val rad = Math.toRadians(angle.toDouble())
                val startX = sunCenter.x + (sunRadius + 3.dp.toPx()) * Math.cos(rad).toFloat()
                val startY = sunCenter.y + (sunRadius + 3.dp.toPx()) * Math.sin(rad).toFloat()
                val endX = sunCenter.x + (sunRadius + 8.dp.toPx()) * Math.cos(rad).toFloat()
                val endY = sunCenter.y + (sunRadius + 8.dp.toPx()) * Math.sin(rad).toFloat()
                drawLine(
                    color = Color(0xFFFBC02D),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        if (weather == WeatherCondition.CLOUDY || weather == WeatherCondition.STORM) {
            val cloudColor = if (weather == WeatherCondition.STORM) Color(0xFF78909C) else Color(0xFFECEFF1)
            // Left Cloud
            val c1x = 20.dp.toPx()
            val c1y = 20.dp.toPx()
            drawCircle(cloudColor, 10.dp.toPx(), Offset(c1x, c1y))
            drawCircle(cloudColor, 14.dp.toPx(), Offset(c1x + 10.dp.toPx(), c1y - 3.dp.toPx()))
            drawCircle(cloudColor, 10.dp.toPx(), Offset(c1x + 20.dp.toPx(), c1y))
            drawRect(cloudColor, Offset(c1x, c1y - 3.dp.toPx()), Size(20.dp.toPx(), 13.dp.toPx()))

            // Right Cloud
            val c2x = width - 60.dp.toPx()
            val c2y = 25.dp.toPx()
            drawCircle(cloudColor, 8.dp.toPx(), Offset(c2x, c2y))
            drawCircle(cloudColor, 12.dp.toPx(), Offset(c2x + 8.dp.toPx(), c2y - 2.dp.toPx()))
            drawCircle(cloudColor, 8.dp.toPx(), Offset(c2x + 16.dp.toPx(), c2y))
            drawRect(cloudColor, Offset(c2x, c2y - 2.dp.toPx()), Size(16.dp.toPx(), 10.dp.toPx()))
        }

        // --- LAYER 2: GROUND & TREE ---
        // Draw Soil / Ground Line
        drawRect(
            color = Color(0xFF6D4C41),
            topLeft = Offset(0f, groundY),
            size = Size(width, 24.dp.toPx())
        )
        drawRect(
            color = Color(0xFF4E342E),
            topLeft = Offset(0f, groundY + 4.dp.toPx()),
            size = Size(width, 20.dp.toPx())
        )

        // Sway pivot point is the base of the trunk at the ground level
        val pivot = Offset(centerX, groundY)

        rotate(degrees = if (stage == TreeStage.SEED) 0f else swayAngle, pivot = pivot) {
            when (stage) {
                TreeStage.SEED -> {
                    // Draw a cute little seed in the soil
                    drawOval(
                        color = Color(0xFF8D6E63),
                        topLeft = Offset(centerX - 10.dp.toPx(), groundY - 8.dp.toPx()),
                        size = Size(20.dp.toPx(), 12.dp.toPx())
                    )
                    // Highlight on the seed
                    drawOval(
                        color = Color(0xFFA1887F),
                        topLeft = Offset(centerX - 6.dp.toPx(), groundY - 7.dp.toPx()),
                        size = Size(10.dp.toPx(), 6.dp.toPx())
                    )
                }

                TreeStage.SPROUT -> {
                    // Draw a small green stem
                    val stemPath = Path().apply {
                        moveTo(centerX, groundY)
                        cubicTo(
                            centerX, groundY - 15.dp.toPx(),
                            centerX + 8.dp.toPx(), groundY - 25.dp.toPx(),
                            centerX + 12.dp.toPx(), groundY - 35.dp.toPx()
                        )
                    }
                    androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()).let { stroke ->
                        drawPath(path = stemPath, color = Color(0xFF81C784), style = stroke)
                    }

                    // Two tiny leaves
                    drawOval(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(centerX + 10.dp.toPx(), groundY - 38.dp.toPx()),
                        size = Size(12.dp.toPx(), 6.dp.toPx())
                    )
                    drawOval(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(centerX + 2.dp.toPx(), groundY - 32.dp.toPx()),
                        size = Size(8.dp.toPx(), 5.dp.toPx())
                    )
                }

                TreeStage.YOUNG_TREE -> {
                    // Thin trunk
                    val trunkWidth = 6.dp.toPx()
                    val trunkHeight = 40.dp.toPx()
                    val trunkColor = if (species == TreeSpecies.BIRCH) Color(0xFFEEEEEE) else Color(0xFF5D4037)
                    
                    drawRect(
                        color = trunkColor,
                        topLeft = Offset(centerX - trunkWidth / 2, groundY - trunkHeight),
                        size = Size(trunkWidth, trunkHeight)
                    )
                    if (species == TreeSpecies.BIRCH) {
                        // Birch bark lines
                        drawLine(Color(0xFF333333), Offset(centerX - trunkWidth/2, groundY - 15.dp.toPx()), Offset(centerX, groundY - 15.dp.toPx()), strokeWidth = 1.dp.toPx())
                        drawLine(Color(0xFF333333), Offset(centerX, groundY - 30.dp.toPx()), Offset(centerX + trunkWidth/2, groundY - 30.dp.toPx()), strokeWidth = 1.dp.toPx())
                    }

                    // Small foliage canopy
                    val canopyColor = when (species) {
                        TreeSpecies.OAK -> Color(0xFF388E3C)
                        TreeSpecies.PINE -> Color(0xFF1B5E20)
                        TreeSpecies.CHERRY_BLOSSOM -> Color(0xFFF48FB1)
                        TreeSpecies.MAPLE -> Color(0xFFE64A19)
                        TreeSpecies.BIRCH -> Color(0xFF81C784)
                    }
                    drawCircle(
                        color = canopyColor,
                        radius = 24.dp.toPx(),
                        center = Offset(centerX, groundY - trunkHeight - 10.dp.toPx())
                    )
                }

                TreeStage.NEARLY_MATURE -> {
                    // Medium trunk
                    val trunkWidth = 10.dp.toPx()
                    val trunkHeight = 60.dp.toPx()
                    val trunkColor = if (species == TreeSpecies.BIRCH) Color(0xFFEEEEEE) else Color(0xFF5D4037)

                    drawRect(
                        color = trunkColor,
                        topLeft = Offset(centerX - trunkWidth / 2, groundY - trunkHeight),
                        size = Size(trunkWidth, trunkHeight)
                    )
                    if (species == TreeSpecies.BIRCH) {
                        // Birch lines
                        drawLine(Color(0xFF333333), Offset(centerX - trunkWidth/2, groundY - 15.dp.toPx()), Offset(centerX + 2.dp.toPx(), groundY - 15.dp.toPx()), strokeWidth = 1.5f.dp.toPx())
                        drawLine(Color(0xFF333333), Offset(centerX - 2.dp.toPx(), groundY - 35.dp.toPx()), Offset(centerX + trunkWidth/2, groundY - 35.dp.toPx()), strokeWidth = 1.5f.dp.toPx())
                        drawLine(Color(0xFF333333), Offset(centerX - trunkWidth/2, groundY - 50.dp.toPx()), Offset(centerX, groundY - 50.dp.toPx()), strokeWidth = 1.5f.dp.toPx())
                    }

                    // Branch lines
                    drawLine(trunkColor, Offset(centerX, groundY - 45.dp.toPx()), Offset(centerX - 15.dp.toPx(), groundY - 55.dp.toPx()), strokeWidth = 4.dp.toPx())
                    drawLine(trunkColor, Offset(centerX, groundY - 40.dp.toPx()), Offset(centerX + 15.dp.toPx(), groundY - 52.dp.toPx()), strokeWidth = 4.dp.toPx())

                    // Overlapping circles for dense foliage
                    val foliageColor = when (species) {
                        TreeSpecies.OAK -> Color(0xFF2E7D32)
                        TreeSpecies.PINE -> Color(0xFF1B5E20)
                        TreeSpecies.CHERRY_BLOSSOM -> Color(0xFFF06292)
                        TreeSpecies.MAPLE -> Color(0xFFD84315)
                        TreeSpecies.BIRCH -> Color(0xFF66BB6A)
                    }

                    if (species == TreeSpecies.PINE) {
                        // Pine triangles
                        val path = Path().apply {
                            moveTo(centerX, groundY - trunkHeight - 20.dp.toPx())
                            lineTo(centerX - 30.dp.toPx(), groundY - trunkHeight + 10.dp.toPx())
                            lineTo(centerX + 30.dp.toPx(), groundY - trunkHeight + 10.dp.toPx())
                            close()
                            
                            moveTo(centerX, groundY - trunkHeight)
                            lineTo(centerX - 24.dp.toPx(), groundY - trunkHeight + 25.dp.toPx())
                            lineTo(centerX + 24.dp.toPx(), groundY - trunkHeight + 25.dp.toPx())
                            close()
                        }
                        drawPath(path, foliageColor)
                    } else {
                        // Standard tree cloud
                        drawCircle(foliageColor, 30.dp.toPx(), Offset(centerX, groundY - trunkHeight - 12.dp.toPx()))
                        drawCircle(foliageColor, 22.dp.toPx(), Offset(centerX - 20.dp.toPx(), groundY - trunkHeight - 8.dp.toPx()))
                        drawCircle(foliageColor, 22.dp.toPx(), Offset(centerX + 20.dp.toPx(), groundY - trunkHeight - 8.dp.toPx()))
                    }
                }

                TreeStage.MATURE -> {
                    // Thick trunk
                    val trunkWidth = 16.dp.toPx()
                    val trunkHeight = 85.dp.toPx()
                    val trunkColor = if (species == TreeSpecies.BIRCH) Color(0xFFF5F5F5) else Color(0xFF4E342E)

                    drawRect(
                        color = trunkColor,
                        topLeft = Offset(centerX - trunkWidth / 2, groundY - trunkHeight),
                        size = Size(trunkWidth, trunkHeight)
                    )

                    if (species == TreeSpecies.BIRCH) {
                        // Birch bark details
                        for (i in 1..6) {
                            val y = groundY - (i * 12).dp.toPx()
                            val left = i % 2 == 0
                            drawLine(
                                color = Color(0xFF212121),
                                start = Offset(if (left) centerX - trunkWidth / 2 else centerX - 2.dp.toPx(), y),
                                end = Offset(if (left) centerX + 2.dp.toPx() else centerX + trunkWidth / 2, y),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    // Main thick branches
                    drawLine(trunkColor, Offset(centerX, groundY - 60.dp.toPx()), Offset(centerX - 25.dp.toPx(), groundY - 75.dp.toPx()), strokeWidth = 6.dp.toPx())
                    drawLine(trunkColor, Offset(centerX, groundY - 55.dp.toPx()), Offset(centerX + 25.dp.toPx(), groundY - 72.dp.toPx()), strokeWidth = 6.dp.toPx())
                    drawLine(trunkColor, Offset(centerX, groundY - 70.dp.toPx()), Offset(centerX + 15.dp.toPx(), groundY - 90.dp.toPx()), strokeWidth = 4.dp.toPx())

                    // Canopy setup
                    when (species) {
                        TreeSpecies.OAK -> {
                            // Large majestic Oak cloud
                            val greenDeep = Color(0xFF1B5E20)
                            val greenOak = Color(0xFF2E7D32)
                            val greenLight = Color(0xFF4CAF50)

                            // Underlayer shadow
                            drawCircle(greenDeep, 45.dp.toPx(), Offset(centerX, groundY - trunkHeight - 15.dp.toPx()))
                            drawCircle(greenDeep, 35.dp.toPx(), Offset(centerX - 35.dp.toPx(), groundY - trunkHeight - 5.dp.toPx()))
                            drawCircle(greenDeep, 35.dp.toPx(), Offset(centerX + 35.dp.toPx(), groundY - trunkHeight - 5.dp.toPx()))

                            // Middle layer
                            drawCircle(greenOak, 40.dp.toPx(), Offset(centerX, groundY - trunkHeight - 20.dp.toPx()))
                            drawCircle(greenOak, 32.dp.toPx(), Offset(centerX - 30.dp.toPx(), groundY - trunkHeight - 10.dp.toPx()))
                            drawCircle(greenOak, 32.dp.toPx(), Offset(centerX + 30.dp.toPx(), groundY - trunkHeight - 10.dp.toPx()))

                            // Highlights
                            drawCircle(greenLight, 25.dp.toPx(), Offset(centerX - 10.dp.toPx(), groundY - trunkHeight - 35.dp.toPx()))
                            drawCircle(greenLight, 20.dp.toPx(), Offset(centerX + 15.dp.toPx(), groundY - trunkHeight - 30.dp.toPx()))
                        }

                        TreeSpecies.PINE -> {
                            // Majestic Pine tree with 3 tiers of overlapping dark-green triangles
                            val pineColor = Color(0xFF0F5132)
                            val pineMid = Color(0xFF198754)
                            
                            val p1 = Path().apply {
                                // Bottom tier
                                moveTo(centerX, groundY - trunkHeight - 10.dp.toPx())
                                lineTo(centerX - 48.dp.toPx(), groundY - trunkHeight + 25.dp.toPx())
                                lineTo(centerX + 48.dp.toPx(), groundY - trunkHeight + 25.dp.toPx())
                                close()

                                // Middle tier
                                moveTo(centerX, groundY - trunkHeight - 35.dp.toPx())
                                lineTo(centerX - 38.dp.toPx(), groundY - trunkHeight - 2.dp.toPx())
                                lineTo(centerX + 38.dp.toPx(), groundY - trunkHeight - 2.dp.toPx())
                                close()

                                // Top tier
                                moveTo(centerX, groundY - trunkHeight - 60.dp.toPx())
                                lineTo(centerX - 28.dp.toPx(), groundY - trunkHeight - 25.dp.toPx())
                                lineTo(centerX + 28.dp.toPx(), groundY - trunkHeight - 25.dp.toPx())
                                close()
                            }
                            drawPath(p1, pineColor)

                            // Highlight path overlays
                            val pHighlight = Path().apply {
                                moveTo(centerX, groundY - trunkHeight - 60.dp.toPx())
                                lineTo(centerX - 12.dp.toPx(), groundY - trunkHeight - 25.dp.toPx())
                                lineTo(centerX + 12.dp.toPx(), groundY - trunkHeight - 25.dp.toPx())
                                close()

                                moveTo(centerX, groundY - trunkHeight - 35.dp.toPx())
                                lineTo(centerX - 18.dp.toPx(), groundY - trunkHeight - 2.dp.toPx())
                                lineTo(centerX + 18.dp.toPx(), groundY - trunkHeight - 2.dp.toPx())
                                close()
                            }
                            drawPath(pHighlight, pineMid)
                        }

                        TreeSpecies.CHERRY_BLOSSOM -> {
                            // Beautiful fluffy pink Cherry Blossom clouds
                            val pinkDeep = Color(0xFFC2185B)
                            val pinkMid = Color(0xFFE91E63)
                            val pinkLight = Color(0xFFF8BBD0)

                            drawCircle(pinkDeep, 42.dp.toPx(), Offset(centerX, groundY - trunkHeight - 12.dp.toPx()))
                            drawCircle(pinkDeep, 32.dp.toPx(), Offset(centerX - 35.dp.toPx(), groundY - trunkHeight - 2.dp.toPx()))
                            drawCircle(pinkDeep, 32.dp.toPx(), Offset(centerX + 35.dp.toPx(), groundY - trunkHeight - 2.dp.toPx()))

                            drawCircle(pinkMid, 36.dp.toPx(), Offset(centerX, groundY - trunkHeight - 18.dp.toPx()))
                            drawCircle(pinkMid, 28.dp.toPx(), Offset(centerX - 28.dp.toPx(), groundY - trunkHeight - 10.dp.toPx()))
                            drawCircle(pinkMid, 28.dp.toPx(), Offset(centerX + 28.dp.toPx(), groundY - trunkHeight - 10.dp.toPx()))

                            drawCircle(pinkLight, 25.dp.toPx(), Offset(centerX - 5.dp.toPx(), groundY - trunkHeight - 32.dp.toPx()))
                            drawCircle(pinkLight, 18.dp.toPx(), Offset(centerX + 18.dp.toPx(), groundY - trunkHeight - 25.dp.toPx()))
                        }

                        TreeSpecies.MAPLE -> {
                            // Vibrant Maple with Orange and Red cloud tones
                            val mapRed = Color(0xFFB71C1C)
                            val mapOrange = Color(0xFFE65100)
                            val mapAmber = Color(0xFFF57C00)

                            drawCircle(mapRed, 44.dp.toPx(), Offset(centerX, groundY - trunkHeight - 12.dp.toPx()))
                            drawCircle(mapRed, 34.dp.toPx(), Offset(centerX - 32.dp.toPx(), groundY - trunkHeight - 2.dp.toPx()))
                            drawCircle(mapRed, 34.dp.toPx(), Offset(centerX + 32.dp.toPx(), groundY - trunkHeight - 2.dp.toPx()))

                            drawCircle(mapOrange, 38.dp.toPx(), Offset(centerX, groundY - trunkHeight - 18.dp.toPx()))
                            drawCircle(mapOrange, 28.dp.toPx(), Offset(centerX - 25.dp.toPx(), groundY - trunkHeight - 8.dp.toPx()))
                            drawCircle(mapOrange, 28.dp.toPx(), Offset(centerX + 25.dp.toPx(), groundY - trunkHeight - 8.dp.toPx()))

                            drawCircle(mapAmber, 22.dp.toPx(), Offset(centerX - 8.dp.toPx(), groundY - trunkHeight - 30.dp.toPx()))
                            drawCircle(mapAmber, 18.dp.toPx(), Offset(centerX + 15.dp.toPx(), groundY - trunkHeight - 24.dp.toPx()))
                        }

                        TreeSpecies.BIRCH -> {
                            // Birch with delicate bright green foliage
                            val birchGreenDeep = Color(0xFF2E7D32)
                            val birchGreen = Color(0xFF4CAF50)
                            val birchGreenLight = Color(0xFFC8E6C9)

                            // Tall, slender oval clouds
                            drawOval(birchGreenDeep, topLeft = Offset(centerX - 30.dp.toPx(), groundY - trunkHeight - 30.dp.toPx()), size = Size(60.dp.toPx(), 45.dp.toPx()))
                            drawOval(birchGreen, topLeft = Offset(centerX - 24.dp.toPx(), groundY - trunkHeight - 35.dp.toPx()), size = Size(48.dp.toPx(), 40.dp.toPx()))
                            drawOval(birchGreenLight, topLeft = Offset(centerX - 15.dp.toPx(), groundY - trunkHeight - 40.dp.toPx()), size = Size(30.dp.toPx(), 25.dp.toPx()))

                            // Some smaller side branch foliage
                            drawCircle(birchGreenDeep, 18.dp.toPx(), Offset(centerX - 30.dp.toPx(), groundY - trunkHeight + 10.dp.toPx()))
                            drawCircle(birchGreen, 18.dp.toPx(), Offset(centerX + 30.dp.toPx(), groundY - trunkHeight + 12.dp.toPx()))
                        }
                    }
                }
            }
        }

        // Render falling Cherry Blossom petals on top of the ground
        if (species == TreeSpecies.CHERRY_BLOSSOM && stage.ordinal >= TreeStage.YOUNG_TREE.ordinal) {
            // Draw 4 animated petals floating downward
            val petalColor = Color(0xFFF48FB1)
            for (i in 1..4) {
                // Determine animated coordinate offsets mathematically
                val seedOffset = i * 0.25f
                val currentPetalProgress = (petalProgress + seedOffset) % 1.0f
                
                val petalX = centerX - 60.dp.toPx() + (i * 30.dp.toPx() + currentPetalProgress * 20.dp.toPx()) % (120.dp.toPx())
                val startY = groundY - 95.dp.toPx()
                val pathHeight = 90.dp.toPx()
                val petalY = startY + currentPetalProgress * pathHeight

                if (petalY < groundY) {
                    rotate(degrees = currentPetalProgress * 360f, pivot = Offset(petalX, petalY)) {
                        drawOval(
                            color = petalColor,
                            topLeft = Offset(petalX - 3.dp.toPx(), petalY - 1.5f.dp.toPx()),
                            size = Size(6.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
        }

        // --- LAYER 3: WEATHER EFFECTS (Rain, Wind, Thunder) ---
        if (weather == WeatherCondition.RAINY || weather == WeatherCondition.STORM) {
            val rainColor = Color(0x9990CAF9)
            val rainCount = 12
            for (i in 0 until rainCount) {
                val xOffset = (i * (width / rainCount))
                val yStart = (rainProgress * height + (i * 17) % height) % height
                val yEnd = yStart + 10.dp.toPx()
                drawLine(
                    color = rainColor,
                    start = Offset(xOffset - 3.dp.toPx(), yStart),
                    end = Offset(xOffset, yEnd),
                    strokeWidth = 1.5f.dp.toPx()
                )
            }
        }

        if (weather == WeatherCondition.WINDY) {
            val windColor = Color(0x66B0BEC5)
            for (w in 0..1) {
                val windY = groundY - 35.dp.toPx() - (w * 30.dp.toPx())
                val windX = (rainProgress * width * 1.5f) - (width * 0.25f)
                drawLine(
                    color = windColor,
                    start = Offset(windX - 25.dp.toPx(), windY),
                    end = Offset(windX, windY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (weather == WeatherCondition.STORM && thunderAlpha > 0.1f) {
            drawRect(
                color = Color(0xFFFFFDE7).copy(alpha = thunderAlpha * 0.25f),
                topLeft = Offset(0f, 0f),
                size = Size(width, height)
            )
        }
    }
}
