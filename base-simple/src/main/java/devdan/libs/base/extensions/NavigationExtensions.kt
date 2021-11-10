package devdan.libs.base.extensions

import androidx.annotation.IdRes
import androidx.navigation.NavController

fun NavController.navigateSafe(@IdRes resId: Int) {
    val action = currentDestination?.getAction(resId) ?: graph.getAction(resId)
    if (action != null && currentDestination?.id != action.destinationId) {
        navigate(resId)
    }
}