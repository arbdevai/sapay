package com.arbdev.sapay.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.arbdev.sapay.data.model.BannerItem
import com.arbdev.sapay.ui.theme.CardSurface
import com.arbdev.sapay.ui.theme.PrimaryBlue
import com.arbdev.sapay.ui.theme.PrimaryBlueDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BannerCarousel(
    banners: List<BannerItem>,
    modifier: Modifier = Modifier,
    onBannerClick: ((BannerItem) -> Unit)? = null
) {
    if (banners.isEmpty()) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { banners.size })

    // Auto-scroll logic (every 4 seconds)
    LaunchedEffect(pagerState.pageCount, pagerState.isScrollInProgress) {
        if (banners.size > 1 && !pagerState.isScrollInProgress) {
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(
                    nextPage,
                    animationSpec = tween(durationMillis = 650)
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) { page ->
            val banner = banners[page]
            val imageUrl = banner.bestImageUrl

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                    .clickable {
                        if (onBannerClick != null) {
                            onBannerClick(banner)
                        } else if (!banner.url.isNullOrBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!imageUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = imageUrl,
                            contentDescription = banner.name ?: "Banner Promo Starlite",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFFE2E8F0),
                                                    Color(0xFFF1F5F9),
                                                    Color(0xFFE2E8F0)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryBlue
                                    )
                                }
                            },
                            error = {
                                FallbackBannerCard(banner = banner)
                            }
                        )
                    } else {
                        FallbackBannerCard(banner = banner)
                    }

                    // Bottom Gradient Shadow & Info Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.72f)
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (!banner.name.isNullOrBlank()) {
                                    Text(
                                        text = banner.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!banner.description.isNullOrBlank()) {
                                    Text(
                                        text = banner.description,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!banner.url.isNullOrBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Buka Link",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Animated Dots Indicator
        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                banners.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 7.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "dot_width"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) PrimaryBlue else Color(0xFFCBD5E1)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackBannerCard(banner: BannerItem) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PrimaryBlueDark,
                        PrimaryBlue,
                        Color(0xFF0284C7)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = banner.name ?: "Starlite Internet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = banner.description ?: "Koneksi Cepat & Stabil Tanpa Kuota",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
