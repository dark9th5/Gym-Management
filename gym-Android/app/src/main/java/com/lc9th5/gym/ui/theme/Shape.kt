package com.lc9th5.gym.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Extra small - for small chips, badges
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - for buttons, text fields
    small = RoundedCornerShape(8.dp),
    
    // Medium - for cards, dialogs
    medium = RoundedCornerShape(16.dp),
    
    // Large - for bottom sheets, large cards
    large = RoundedCornerShape(24.dp),
    
    // Extra large - for full-screen dialogs
    extraLarge = RoundedCornerShape(32.dp)
)

// Custom shapes for specific use cases
val BottomNavShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val TopBarShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
val CardShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(8.dp)
val AvatarShape = RoundedCornerShape(50)
