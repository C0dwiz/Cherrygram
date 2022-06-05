package uz.unnarsx.cherrygram.prefviews;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.Property;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.Switch;

import java.util.ArrayList;

public class TextCheckWithIconCell extends FrameLayout {
    private boolean isAnimatingToThumbInsteadOfTouch;

    private TextView textView;
    private Switch checkBox;
    private boolean needDivider;
    private int height = 50;
    private int animatedColorBackground;
    private float animationProgress;
    private Paint animationPaint;
    private float lastTouchX;
    private ObjectAnimator animator;
    private boolean drawCheckRipple;
    public final RLottieImageView imageView;
    private ImageView valueImageView;
    private int offsetFromImage;
    public int imageLeft;
    private boolean inDialogs;

    public static final Property<TextCheckWithIconCell, Float> ANIMATION_PROGRESS = new AnimationProperties.FloatProperty<TextCheckWithIconCell>("animationProgress") {
        @Override
        public void setValue(TextCheckWithIconCell object, float value) {
            object.setAnimationProgress(value);
            object.invalidate();
        }

        @Override
        public Float get(TextCheckWithIconCell object) {
            return object.animationProgress;
        }
    };

    public TextCheckWithIconCell(Context context) {
        this(context, false);
    }

    public TextCheckWithIconCell(Context context, boolean dialog) {
        super(context);

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(dialog ? Theme.key_dialogTextBlack : Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 70, 0, 70, 0));

        imageView = new RLottieImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(dialog ? Theme.key_dialogIcon : Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
        addView(imageView, LayoutHelper.createFrame(26, 26, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 21, 0, 21, 0));

        checkBox = new Switch(context);
        checkBox.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        addView(checkBox, LayoutHelper.createFrame(37, 20, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 22, 0, 22, 0));

        setClipChildren(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        lastTouchX = event.getX();
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(height) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setDivider(boolean divider) {
        needDivider = divider;
        setWillNotDraw(!divider);
    }

    public void setTextAndCheckAndIcon(String text, int resId, boolean checked, boolean divider) {
        imageLeft = 21;
        offsetFromImage = 71;
        textView.setText(text);
        checkBox.setChecked(checked, false);
        needDivider = divider;
        imageView.setImageResource(resId);
        imageView.setVisibility(VISIBLE);
        LayoutParams layoutParams = (LayoutParams) textView.getLayoutParams();
        layoutParams.height = LayoutParams.MATCH_PARENT;
        layoutParams.topMargin = 0;
        textView.setLayoutParams(layoutParams);
        setWillNotDraw(!divider);
    }

    @Override
    public void setPressed(boolean pressed) {
        if (drawCheckRipple) {
            checkBox.setDrawRipple(pressed);
        }
        super.setPressed(pressed);
    }

    public void setEnabled(boolean value, ArrayList<Animator> animators) {
        super.setEnabled(value);
        if (animators != null) {
            animators.add(ObjectAnimator.ofFloat(textView, "alpha", value ? 1.0f : 0.5f));
            animators.add(ObjectAnimator.ofFloat(checkBox, "alpha", value ? 1.0f : 0.5f));
        } else {
            textView.setAlpha(value ? 1.0f : 0.5f);
            checkBox.setAlpha(value ? 1.0f : 0.5f);
        }
    }

    public void setChecked(boolean checked) {
        checkBox.setChecked(checked, true);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    @Override
    public void setBackgroundColor(int color) {
        clearAnimation();
        animatedColorBackground = 0;
        super.setBackgroundColor(color);
    }

    public void setBackgroundColorAnimated(boolean checked, int color) {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (animatedColorBackground != 0) {
            setBackgroundColor(animatedColorBackground);
        }
        if (animationPaint == null) {
            animationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        checkBox.setOverrideColor(checked ? 1 : 2);
        animatedColorBackground = color;
        animationPaint.setColor(animatedColorBackground);
        animationProgress = 0.0f;
        animator = ObjectAnimator.ofFloat(this, ANIMATION_PROGRESS, 0.0f, 1.0f);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setBackgroundColor(animatedColorBackground);
                animatedColorBackground = 0;
                invalidate();
            }
        });
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        animator.setDuration(240).start();
    }

    private void setAnimationProgress(float value) {
        animationProgress = value;
        float tx = getLastTouchX();
        float rad = Math.max(tx, getMeasuredWidth() - tx) + AndroidUtilities.dp(40);
        float cx = tx;
        int cy = getMeasuredHeight() / 2;
        float animatedRad = rad * animationProgress;
        checkBox.setOverrideColorProgress(cx, cy, animatedRad);
    }

    public void setBackgroundColorAnimatedReverse(int color) {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        int from = animatedColorBackground != 0 ? animatedColorBackground : getBackground() instanceof ColorDrawable ? ((ColorDrawable) getBackground()).getColor() : 0;
        if (animationPaint == null) animationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        animationPaint.setColor(from);

        setBackgroundColor(color);
        checkBox.setOverrideColor(1);
        animatedColorBackground = color;
        animator = ObjectAnimator.ofFloat(this, ANIMATION_PROGRESS, 1, 0).setDuration(240);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setBackgroundColor(color);
                animatedColorBackground = 0;
                invalidate();
            }
        });
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        animator.start();
    }

    private float getLastTouchX() {
        return isAnimatingToThumbInsteadOfTouch ? (LocaleController.isRTL ? AndroidUtilities.dp(22) : getMeasuredWidth() - AndroidUtilities.dp(42)) : lastTouchX;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (animatedColorBackground != 0) {
            float tx = getLastTouchX();
            float rad = Math.max(tx, getMeasuredWidth() - tx) + AndroidUtilities.dp(40);
            float cx = tx;
            int cy = getMeasuredHeight() / 2;
            float animatedRad = rad * animationProgress;
            canvas.drawCircle(cx, cy, animatedRad, animationPaint);
        }
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(70), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(70) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    public void setAnimatingToThumbInsteadOfTouch(boolean animatingToThumbInsteadOfTouch) {
        isAnimatingToThumbInsteadOfTouch = animatingToThumbInsteadOfTouch;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Switch");
        info.setCheckable(true);
        info.setChecked(checkBox.isChecked());
        info.setContentDescription(checkBox.isChecked() ? LocaleController.getString("NotificationsOn", R.string.NotificationsOn) : LocaleController.getString("NotificationsOff", R.string.NotificationsOff));
    }
}