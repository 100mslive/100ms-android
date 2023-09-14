package live.hms.roomkit.ui.notification;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;


public class RewindAnimationSetting implements AnimationSetting {

    private final Direction direction;
    private final int duration;
    private final Interpolator interpolator;

    private RewindAnimationSetting(
            Direction direction,
            int duration,
            Interpolator interpolator
    ) {
        this.direction = direction;
        this.duration = duration;
        this.interpolator = interpolator;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public Interpolator getInterpolator() {
        return interpolator;
    }

    public static class Builder {
        private Direction direction = Direction.Bottom;
        private int duration = Duration.Normal.duration;
        private Interpolator interpolator = new DecelerateInterpolator();

        public RewindAnimationSetting.Builder setDirection(Direction direction) {
            this.direction = direction;
            return this;
        }

        public RewindAnimationSetting.Builder setDuration(int duration) {
            this.duration = duration;
            return this;
        }

        public RewindAnimationSetting.Builder setInterpolator(Interpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        public RewindAnimationSetting build() {
            return new RewindAnimationSetting(
                    direction,
                    duration,
                    interpolator
            );
        }
    }

}
