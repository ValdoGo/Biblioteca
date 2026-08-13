package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Estados de Animação
    val offsetX = remember { Animatable(-320f) } // movimento da esquerda para o centro
    val walkStep = remember { Animatable(0f) }   // stealth crawl oscillation
    val bookReveal = remember { Animatable(0f) } // revelação do livro / logo completo
    val logoScale = remember { Animatable(0.85f) }
    val whiteFadeAlpha = remember { Animatable(0f) } // transição fade-to-white final

    LaunchedEffect(Unit) {
        // 1. Gato rasteja / caminha com movimento furtivo da esquerda até o centro
        launch {
            // Oscilação stealth do passo do gato
            walkStep.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        offsetX.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1800, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f))
        )

        // 2. Ao chegar ao centro, transição para sentar e revelar o livro (Logo do Gato Lendo)
        bookReveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        logoScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )

        // 3. Mantém a cena por 1 segundo após o logo estar totalmente formado
        delay(1000)

        // 4. Fade suave para branco (fade-to-white) indicando abertura da aplicação
        whiteFadeAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing)
        )

        // 5. Notifica que o Splash terminou para navegar à tela principal ou login
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Conteúdo do Splash em proporção cinematográfica
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                if (bookReveal.value < 0.8f) {
                    // SILHUETA DO GATO EM MOVIMENTO STEALTH
                    val currentOffsetY = (kotlin.math.sin(walkStep.value * Math.PI) * 6).dp

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX.value.dp, y = currentOffsetY)
                            .size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(68.dp),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_logo_1786614660714),
                                    contentDescription = "Silhouette Cat Crawling",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                } else {
                    // LOGO COMPLETO DO GATO LENDO LIVRO FORMADO NO CENTRO
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .scale(logoScale.value)
                            .alpha(bookReveal.value)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 12.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_logo_1786614660714),
                                    contentDescription = "MozOn Reading Cat Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "MozOn",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = "Biblioteca Digital Académica",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }
        }

        // Overlay de Transição Fade to White
        if (whiteFadeAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = whiteFadeAlpha.value))
            )
        }
    }
}
