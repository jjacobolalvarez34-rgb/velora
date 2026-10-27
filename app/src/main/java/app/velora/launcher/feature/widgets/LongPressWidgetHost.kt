package app.velora.launcher.feature.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Los widgets son `View`s que se quedan con sus toques, así que Compose no ve el toque largo.
 * Esta vista lo detecta antes que el widget (`onInterceptTouchEvent`) y avisa: el inicio entra
 * en modo edición de ese widget.
 */
internal class LongPressWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPress: (() -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var fired = false

    private val trigger = Runnable {
        fired = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
        // El widget no debe recibir el "soltar" como un toque.
        cancelLongPress()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fired = false
                downX = event.x
                downY = event.y
                postDelayed(trigger, ViewConfiguration.getLongPressTimeout().toLong())
            }

            MotionEvent.ACTION_MOVE ->
                if (abs(event.x - downX) > slop || abs(event.y - downY) > slop) removeCallbacks(trigger)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(trigger)
        }
        return fired
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(trigger)
        super.onDetachedFromWindow()
    }
}

/** El host de Velora crea siempre [LongPressWidgetHostView]. */
internal class VeloraAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?,
    ): AppWidgetHostView = LongPressWidgetHostView(context)
}
