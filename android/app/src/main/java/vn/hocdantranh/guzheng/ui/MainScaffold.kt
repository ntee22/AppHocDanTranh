package vn.hocdantranh.guzheng.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.hocdantranh.guzheng.ui.lessons.LessonDetailRoute
import vn.hocdantranh.guzheng.ui.lessons.LessonsRoute
import vn.hocdantranh.guzheng.ui.sheet.SheetPageZoomScreen
import vn.hocdantranh.guzheng.ui.sheet.SheetRoute
import vn.hocdantranh.guzheng.ui.sheet.SheetViewModel
import vn.hocdantranh.guzheng.ui.sheet.SheetViewerScreen
import vn.hocdantranh.guzheng.ui.video.VideoRoute
import vn.hocdantranh.guzheng.ui.theme.VintageColors

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.LESSONS.route

    Scaffold(
        bottomBar = {
            // The sheet viewer and page zoom cover the bar, as their iOS fullScreenCover does.
            if (!isFullScreenRoute(currentRoute)) {
                NavigationBar(containerColor = VintageColors.paperWarm) {
                    Destination.entries.forEach { destination ->
                        val label = stringResource(destination.labelRes)
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VintageColors.sageDark,
                                selectedTextColor = VintageColors.sageDark,
                                indicatorColor = VintageColors.sageSoft,
                                unselectedIconColor = VintageColors.inkMuted,
                                unselectedTextColor = VintageColors.inkMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.LESSONS.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.LESSONS.route) {
                LessonsRoute(
                    onLessonClick = { lesson ->
                        navController.navigate(lessonDetailRoute(lesson.lessonNumber))
                    }
                )
            }

            composable(
                route = LESSON_DETAIL_ROUTE,
                arguments = listOf(navArgument(ARG_LESSON_NUMBER) { type = NavType.IntType }),
            ) { entry ->
                LessonDetailRoute(
                    lessonNumber = entry.arguments?.getInt(ARG_LESSON_NUMBER) ?: 1,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Destination.VIDEO.route) {
                VideoRoute()
            }

            composable(Destination.SHEET.route) {
                SheetRoute(
                    onSheetClick = { sheet ->
                        navController.navigate(sheetViewerRoute(sheet.id))
                    }
                )
            }

            composable(
                route = SHEET_VIEWER_ROUTE,
                arguments = listOf(navArgument(ARG_SHEET_ID) { type = NavType.StringType }),
            ) { entry ->
                val sheetId = entry.arguments?.getString(ARG_SHEET_ID)
                val viewModel = sheetTabViewModel(navController, entry)
                val state by viewModel.uiState.collectAsState()
                val sheet = sheetId?.let(state::sheetById)

                // Deep link to an id the catalog does not hold: pop rather than show nothing.
                LaunchedEffect(sheet, state.isLoading) {
                    if (sheet == null && !state.isLoading) navController.popBackStack()
                }

                if (sheet != null) {
                    SheetViewerScreen(
                        sheet = sheet,
                        onBack = { navController.popBackStack() },
                        onPageClick = { index ->
                            navController.navigate(sheetPageRoute(sheet.id, index))
                        },
                    )
                }
            }

            composable(
                route = SHEET_PAGE_ROUTE,
                arguments = listOf(
                    navArgument(ARG_SHEET_ID) { type = NavType.StringType },
                    navArgument(ARG_PAGE_INDEX) { type = NavType.IntType },
                ),
            ) { entry ->
                val sheetId = entry.arguments?.getString(ARG_SHEET_ID)
                val pageIndex = entry.arguments?.getInt(ARG_PAGE_INDEX) ?: 0
                val viewModel = sheetTabViewModel(navController, entry)
                val state by viewModel.uiState.collectAsState()
                val page = sheetId?.let(state::sheetById)?.pages?.getOrNull(pageIndex)

                LaunchedEffect(page, state.isLoading) {
                    if (page == null && !state.isLoading) navController.popBackStack()
                }

                if (page != null) {
                    SheetPageZoomScreen(
                        url = page,
                        pageNumber = pageIndex + 1,
                        onClose = { navController.popBackStack() },
                    )
                }
            }

            Destination.entries
                .filterNot {
                    it == Destination.LESSONS ||
                        it == Destination.VIDEO ||
                        it == Destination.SHEET
                }
                .forEach { destination ->
                    composable(destination.route) {
                        PlaceholderScreen(title = stringResource(destination.labelRes))
                    }
                }
        }
    }
}

/**
 * The [SheetViewModel] owned by the sheet tab's back-stack entry.
 *
 * Scoping it to the tab rather than to each destination means the viewer and the zoom
 * screen reuse the catalog the list already loaded, instead of each running its own
 * Firestore query.
 */
@Composable
private fun sheetTabViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): SheetViewModel {
    val context = LocalContext.current
    val parentEntry = remember(entry) {
        navController.getBackStackEntry(Destination.SHEET.route)
    }
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = SheetViewModel.factory(context),
    )
}
