package com.sek789102.formulabook.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.ui.theme.Dimens

@Composable
fun StudentMascotAvatar(
    sizeDp: Int = 64,
    @DrawableRes drawableRes: Int = R.drawable.img_student_mascot,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(sizeDp.dp)
            .then(
                if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick) else Modifier
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.BorderSelected, MaterialTheme.colorScheme.primaryContainer),
        shadowElevation = Dimens.ElevationCard
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription ?: stringResource(R.string.mascot_avatar_desc),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun StudentMascotCompanionCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    @DrawableRes drawableRes: Int = R.drawable.img_student_mascot,
    avatarContentDescription: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.Radius16),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.ElevationNone
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentMascotAvatar(
                sizeDp = 56,
                drawableRes = drawableRes,
                contentDescription = avatarContentDescription
            )
            Spacer(Modifier.width(Dimens.Space12))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
