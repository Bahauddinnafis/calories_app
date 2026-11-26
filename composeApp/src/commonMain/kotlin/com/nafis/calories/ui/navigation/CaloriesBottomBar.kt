package com.nafis.calories.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.PrimarySoft

enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Beranda", Icons.Filled.Home),
    History("Riwayat", Icons.AutoMirrored.Filled.List),
    Target("Target", Icons.Filled.TrackChanges),
    Profile("Profil", Icons.Filled.Person)
}

private val InactiveColor = Color(0xFF94A3B8)

@Composable
fun CaloriesBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(AppTab.Home, selectedTab, onTabSelected)
                NavBarItem(AppTab.History, selectedTab, onTabSelected)
                Spacer(modifier = Modifier.weight(1f))
                NavBarItem(AppTab.Target, selectedTab, onTabSelected)
                NavBarItem(AppTab.Profile, selectedTab, onTabSelected)
            }
        }

        Surface(
            modifier = Modifier
                .size(58.dp)
                .offset(y = (-4).dp)
                .border(4.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(20.dp))
                .clickable(onClick = onScanClick),
            shape = RoundedCornerShape(20.dp),
            color = Primary,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CenterFocusWeak,
                    contentDescription = "Scan Makanan",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavBarItem(tab: AppTab, selectedTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    val selected = tab == selectedTab
    val contentColor = if (selected) Primary else InactiveColor
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onTabSelected(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) PrimarySoft else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            fontSize = 11.sp
        )
    }
}
