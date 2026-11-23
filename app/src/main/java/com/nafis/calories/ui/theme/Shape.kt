package com.nafis.calories.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Extra small: Small chips, tags
    extraSmall = RoundedCornerShape(8.dp),

    // Small: Small buttons, input fields
    small = RoundedCornerShape(12.dp),

    // Medium: Standard cards, buttons
    medium = RoundedCornerShape(16.dp),

    // Large: Primary cards, images
    large = RoundedCornerShape(20.dp),

    // Extra large: Modal sheets, large containers
    extraLarge = RoundedCornerShape(24.dp)
)