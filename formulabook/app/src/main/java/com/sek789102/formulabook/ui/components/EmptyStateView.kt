package com.sek789102.formulabook.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.ui.theme.Dimens

enum class EmptyStateIllustration(
    @param:DrawableRes val drawableRes: Int,
    @param:DrawableRes val mascotDrawableRes: Int
) {
    NOTES(R.drawable.ill_empty_notes, R.drawable.img_mascot_humanities),
    SEARCH(R.drawable.ill_empty_search, R.drawable.img_mascot_english),
    TAGS(R.drawable.ill_empty_tags, R.drawable.img_mascot_tags),
    STUDY(R.drawable.ill_empty_study, R.drawable.img_mascot_study),
    DONE(R.drawable.ill_empty_done, R.drawable.img_mascot_trophy),
    EXAMS(R.drawable.ill_empty_exams, R.drawable.img_mascot_focus),
    TRASH(R.drawable.ill_empty_trash, R.drawable.img_mascot_tags)
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    illustration: EmptyStateIllustration? = EmptyStateIllustration.NOTES,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .widthIn(max = Dimens.SheetMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space16, vertical = Dimens.Space16),
        shape = RoundedCornerShape(Dimens.Radius24),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.ElevationCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.Space24, vertical = Dimens.Space24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (illustration != null) {
                Box(
                    modifier = Modifier.size(168.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(156.dp)
                            .clip(RoundedCornerShape(Dimens.Radius24))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = illustration.drawableRes),
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(136.dp)
                        )
                    }
                    StudentMascotAvatar(
                        sizeDp = 52,
                        drawableRes = illustration.mascotDrawableRes,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
                Spacer(Modifier.height(Dimens.Space16))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Dimens.Space8))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(Dimens.Space24))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(Dimens.Radius12),
                    modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
