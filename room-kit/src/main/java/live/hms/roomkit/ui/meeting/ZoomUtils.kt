package live.hms.roomkit.ui.meeting

import android.graphics.Bitmap
import android.graphics.Matrix


//val matrix = Matrix()
//matrix.postScale(zoomScale, zoomScale);
//return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
/**
 * Returns a bitmap that is zoomed in based on input
 * @param zoomScale scale at which image will be zoomed. 1.0f = original size
 * @param bmp bitmap to be modified
 * @param xPercentage X-position to be in the center, as a percentage of the bitmap width, 0-100f
 * @param yPercentage Y-position to be in the center, as a percentage of the bitmap height, 0-100f
 */
fun getZoomedBitmap(
    src: Bitmap,
    expectedWidth: Int,
    expectedHeight: Int
): Bitmap {
    // Check if the image is big enough
    val widthDelta = src.width - expectedWidth
    val heightDelta = src.height - expectedHeight
    val scaleFactor : Float = if(widthDelta < 0 || heightDelta < 0){
        // Find the biggest stretch that might be required.
        val isHeightLess = heightDelta < widthDelta
        if(isHeightLess) {
            //scale factor depends on height
            expectedHeight.toFloat()/src.height
        } else {
            expectedWidth.toFloat() / src.width
        }
    } else {
        1.0f
    }
    val matrix = Matrix()
    if(scaleFactor != 1.0f) {
        matrix.postScale(scaleFactor, scaleFactor)
    }

    // We've got to crop the image to the expected size.
    // We either scale up first or scale down first.

    return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
}