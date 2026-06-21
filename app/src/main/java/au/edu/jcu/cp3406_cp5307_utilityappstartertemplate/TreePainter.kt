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
import androidx.compose.ui.unit.dp

// ponytail: Dynamic Canvas drawing with mathematical sway and particle calculations. Zero image files.

@Composable
fun TreeCanvas(
    species: TreeSpecies,
    stage: TreeStage,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TreeSway")
    

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val groundY = height - 24.dp.toPx()
        val centerX = width / 2f

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
    }
}
