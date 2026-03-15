package com.stackusers.ui.screens.userdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.stackusers.R
import com.stackusers.data.model.BadgeCounts
import com.stackusers.data.model.TopTag
import com.stackusers.data.model.UserDetail
import com.stackusers.ui.UiState
import com.stackusers.ui.components.ErrorState
import com.stackusers.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * User detail screen showing full profile information for a given user
 *
 * @param userId        The StackOverflow user ID, passed from the nav graph
 * @param onNavigateUp  Called when the user taps the back arrow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: Long,
    onNavigateUp: () -> Unit,
    viewModel: UserDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUserDetail(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState is UiState.Success) {
                            (uiState as UiState.Success<UserDetail>).data.user.displayName
                        } else {
                            stringResource(R.string.title_user_detail)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_up)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is UiState.Loading, is UiState.Idle -> LoadingIndicator(
                modifier = Modifier.padding(paddingValues)
            )
            is UiState.Success -> UserDetailContent(
                userDetail = state.data,
                modifier = Modifier.padding(paddingValues)
            )
            is UiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.retry(userId) },
                modifier = Modifier.padding(paddingValues)
            )
            is UiState.Empty -> ErrorState(
                message = stringResource(R.string.error_user_not_found),
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun UserDetailContent(
    userDetail: UserDetail,
    modifier: Modifier = Modifier
) {
    val user = userDetail.user

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = stringResource(R.string.cd_user_avatar, user.displayName),
            placeholder = painterResource(R.drawable.baseline_person_24),
            error = painterResource(R.drawable.baseline_person_24),
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.label_reputation, user.reputation.formatReputation()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        if (userDetail.topTags.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.section_top_tags))
            Spacer(modifier = Modifier.height(8.dp))
            TopTagsSection(tags = userDetail.topTags)
            Spacer(modifier = Modifier.height(24.dp))
        }

        SectionHeader(title = stringResource(R.string.section_badges))
        Spacer(modifier = Modifier.height(8.dp))
        BadgesRow(badgeCounts = user.badgeCounts)
        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        if (!user.location.isNullOrBlank()) {
            IconLabelRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = user.location
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        IconLabelRow(
            icon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            },
            label = stringResource(
                R.string.label_member_since,
                user.creationDate.toFormattedDate()
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopTagsSection(tags: List<TopTag>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            AssistChip(
                onClick = {},
                label = { Text(tag.tagName, style = MaterialTheme.typography.labelMedium) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    labelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun BadgesRow(badgeCounts: BadgeCounts) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BadgeItem(count = badgeCounts.gold, color = Color(0xFFFFD700), label = stringResource(R.string.badge_gold))
        BadgeItem(count = badgeCounts.silver, color = Color(0xFFC0C0C0), label = stringResource(R.string.badge_silver))
        BadgeItem(count = badgeCounts.bronze, color = Color(0xFFCD7F32), label = stringResource(R.string.badge_bronze))
    }
}

@Composable
private fun BadgeItem(count: Int, color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun IconLabelRow(
    icon: @Composable () -> Unit,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

private fun Int.formatReputation(): String {
    return when {
        this >= 1_000_000 -> "${this / 1_000_000}m"
        this >= 1_000 -> "${this / 1_000}k"
        else -> this.toString()
    }
}

private fun Long.toFormattedDate(): String {
    val date = Date(this * 1000L)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}
