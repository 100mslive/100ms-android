package live.hms.roomkit.ui.notification;

import android.view.animation.Interpolator;

public interface AnimationSetting {
    Direction getDirection();
    int getDuration();
    Interpolator getInterpolator();
}
