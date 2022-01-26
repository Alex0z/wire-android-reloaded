package com.wire.android.ui.main.conversation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.main.conversation.navigation.ConversationsNavigationItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Conversation(viewModel: ConversationViewModel = ConversationViewModel()) {
    val uiState by viewModel.state.collectAsState()
    val navController = rememberNavController()

    Scaffold(
        floatingActionButton = { FloatingActionButton(stringResource(R.string.label_new), {}) },
        bottomBar = { WireBottomNavigationBar(ConversationNavigationItems(uiState), navController) }
    ) {
        with(uiState) {
            NavHost(navController, startDestination = ConversationsNavigationItem.All.route) {
                composable(route = ConversationsNavigationItem.All.route, content = { AllConversationScreen(newActivities, conversations) })
                composable(route = ConversationsNavigationItem.Calls.route, content = { CallScreen(missedCalls, callHistory) })
                composable(route = ConversationsNavigationItem.Mentions.route, content = { MentionScreen(unreadMentions, allMentions) })
            }
        }
    }
}

@Composable
private fun ConversationNavigationItems(
    uiState: ConversationState
): List<WireBottomNavigationItemData> {
    return ConversationsNavigationItem.values().map {
        when (it) {
            ConversationsNavigationItem.All -> it.toBottomNavigationItemData(uiState.newActivityCount)
            ConversationsNavigationItem.Calls -> it.toBottomNavigationItemData(uiState.missedCallsCount)
            ConversationsNavigationItem.Mentions -> it.toBottomNavigationItemData(uiState.unreadMentionsCount)
        }
    }
}

