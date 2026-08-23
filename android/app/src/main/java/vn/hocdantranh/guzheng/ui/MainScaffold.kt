package vn.hocdantranh.guzheng.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.hocdantranh.guzheng.ui.lessons.LessonDetailRoute
import vn.hocdantranh.guzheng.ui.lessons.LessonsRoute
import vn.hocdantranh.guzheng.ui.video.VideoRoute
import vn.hocdantranh.guzheng.ui.theme.VintageColors

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.LESSONS.route

    Scaffold(
        bottomBar = {
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

            Destination.entries
                .filterNot { it == Destination.LESSONS || it == Destination.VIDEO }
                .forEach { destination ->
                    composable(destination.route) {
                        PlaceholderScreen(title = stringResource(destination.labelRes))
                    }
                }
        }
    }
}
