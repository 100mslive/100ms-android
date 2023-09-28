package live.hms.roomkit.util

import androidx.constraintlayout.widget.ConstraintLayout
import android.view.View

import androidx.constraintlayout.widget.ConstraintSet

inline fun ConstraintLayout.applyConstraint(operations: Constraint.() -> Unit) {
    val constraint = Constraint(this)
    operations(constraint)
    constraint.applyTo(this)
}

class Constraint(val layout: ConstraintLayout) : ConstraintSet() {
    init {
        clone(layout)
    }

    inline fun <V : View> V.start_toStartOf(targetId: Int, margin: Int = 0) {
        connect(this.id, START, targetId, START, margin)
    }

    inline fun <V : View> V.start_toEndOf(targetId: Int, margin: Int = 0) {
        connect(this.id, START, targetId, END, margin)
    }

    inline fun <V : View> V.end_toEndOf(targetId: Int, margin: Int = 0) {
        connect(this.id, END, targetId, END, margin)
    }

    inline fun <V : View> V.end_toStartOf(targetId: Int, margin: Int = 0) {
        connect(this.id, END, targetId, START, margin)
    }

    inline fun <V : View> V.top_toTopOf(targetId: Int, margin: Int = 0) {
        connect(this.id, TOP, targetId, TOP, margin)
    }

    inline fun <V : View> V.top_toBottomOf(targetId: Int, margin: Int = 0) {
        connect(this.id, TOP, targetId, BOTTOM, margin)
    }

    inline fun <V : View> V.bottom_toTopOf(targetId: Int, margin: Int = 0) {
        connect(this.id, BOTTOM, targetId, TOP, margin)
    }

    inline fun <V : View> V.bottom_toBottomOf(targetId: Int, margin: Int = 0) {
        connect(this.id, BOTTOM, targetId, BOTTOM, margin)
    }

    inline fun <V : View> V.start_toStartOfParent(margin: Int = 0) {
        connect(this.id, START, PARENT_ID, START, margin)
    }

    inline fun <V : View> V.start_toEndOfParent(margin: Int = 0) {
        connect(this.id, START, PARENT_ID, END, margin)
    }

    inline fun <V : View> V.end_toEndOfParent(margin: Int = 0) {
        connect(this.id, END, PARENT_ID, END, margin)
    }

    inline fun <V : View> V.end_toStartOfParent(margin: Int = 0) {
        connect(this.id, END, PARENT_ID, START, margin)
    }

    inline fun <V : View> V.top_toTopOfParent(margin: Int = 0) {
        connect(this.id, TOP, PARENT_ID, TOP, margin)
    }

    inline fun <V : View> V.top_toBottomOfParent(margin: Int = 0) {
        connect(this.id, TOP, PARENT_ID, BOTTOM, margin)
    }

    inline fun <V : View> V.bottom_toTopOfParent(margin: Int = 0) {
        connect(this.id, BOTTOM, PARENT_ID, TOP, margin)
    }

    inline fun <V : View> V.bottom_toBottomOfParent(margin: Int = 0) {
        connect(this.id, BOTTOM, PARENT_ID, BOTTOM, margin)
    }

    inline fun <V : View> V.clearStart() {
        clear(this.id, START)
    }

    inline fun <V : View> V.clearEnd() {
        clear(this.id, END)
    }

    inline fun <V : View> V.clearTop() {
        clear(this.id, TOP)
    }

    inline fun <V : View> V.clearBottom() {
        clear(this.id, BOTTOM)
    }

    inline fun <V : View> V.baseline_toBaselineOf(targetId: Int) {
        connect(this.id, BASELINE, targetId, BASELINE)
    }

    inline fun <V : View> V.widthPercent(percent: Float) {
        constrainPercentWidth(this.id, percent)
    }
}