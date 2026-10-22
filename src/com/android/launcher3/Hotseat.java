package com.android.launcher3;

import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.android.launcher3.LauncherAnimUtils.VIEW_TRANSLATE_X;
import static com.android.launcher3.util.MultiTranslateDelegate.INDEX_BUBBLE_ADJUSTMENT_ANIM;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.celllayout.CellPosMapper;
import com.android.launcher3.celllayout.CellPosMapper.CellPos;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.pageindicators.PageIndicatorDots;
import com.android.launcher3.util.HorizontalInsettableView;
import com.android.launcher3.util.MultiPropertyFactory;
import com.android.launcher3.util.MultiPropertyFactory.MultiProperty;
import com.android.launcher3.util.MultiTranslateDelegate;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.DoubleShadowBubbleTextView;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import app.lawnchair.hotseat.DisabledHotseat;
import app.lawnchair.hotseat.HotseatMode;
import app.lawnchair.hotseat.HotseatPagedView;
import app.lawnchair.hotseat.LawnchairHotseat;
import app.lawnchair.preferences.PreferenceManager;
import app.lawnchair.preferences2.PreferenceCacheExtensionsKt;
import app.lawnchair.preferences2.PreferenceManager2;

public class Hotseat extends FrameLayout implements Insettable, HorizontalInsettableView, LawnchairHotseat {

    public static final float WORKSPACE_TO_HOTSEAT_HEIGHT_RATIO = 0.44f;

    public static final int ALPHA_CHANNEL_TASKBAR_ALIGNMENT = 0;
    public static final int ALPHA_CHANNEL_PREVIEW_RENDERER = 1;
    public static final int ALPHA_CHANNEL_TASKBAR_STASH = 2;
    public static final int ALPHA_CHANNEL_ASSISTANT_VISIBILITY = 3;
    public static final int ALPHA_CHANNEL_CHANNELS_COUNT = 4;

    @Retention(RetentionPolicy.RUNTIME)
    @IntDef({ALPHA_CHANNEL_TASKBAR_ALIGNMENT, ALPHA_CHANNEL_PREVIEW_RENDERER,
            ALPHA_CHANNEL_TASKBAR_STASH, ALPHA_CHANNEL_ASSISTANT_VISIBILITY})
    public @interface HotseatQsbAlphaId {
    }

    public static final int ICONS_TRANSLATION_X_NAV_BAR_ALIGNMENT = 0;
    public static final int ICONS_TRANSLATION_X_CHANNELS_COUNT = 1;

    private static final int QSB_INDEX_WORKSPACE_SCALE = 0;
    private static final int QSB_COUNT = 1;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final boolean mHasVerticalHotseat;
    private final ActivityContext mActivity;
    private Workspace<?> mWorkspace;
    private boolean mSendClicksToWorkspace;

    private final View mQsb;
    private final View mIconsContainer;
    private final PageIndicatorDots mPageIndicator;

    private final MultiPropertyFactory<View> mIconsTranslationXFactory;
    private @Nullable MultiTranslateDelegate mQsbTranslationX;
    private final MultiValueAlpha mQsbAlphaChannels;
    private final MultiValueAlpha mIconsAlpha;

    private PreferenceManager preferenceManager;
    private PreferenceManager2 preferenceManager2;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mActivity = ActivityContext.lookupContext(context);
        mHasVerticalHotseat = mActivity.getDeviceProfile().isVerticalBarLayout();

        preferenceManager = PreferenceManager.getInstance(context);
        preferenceManager2 = PreferenceManager2.getInstance(context);

        mQsb = LayoutInflater.from(context).inflate(preferenceManager2.getHotseatMode().get().getLayoutResourceId(), this, false);
        addView(mQsb);
        mIconsContainer = LayoutInflater.from(context).inflate(R.layout.hotseat_icons_container, this, false);
        addView(mIconsContainer);

        mPageIndicator = LayoutInflater.from(context).inflate(R.layout.hotseat_page_indicator, this, false).findViewById(R.id.page_indicator_dots);
        addView(mPageIndicator);

        if (mQsb instanceof Reorderable qsbReorderable) {
            mQsbTranslationX = qsbReorderable.getTranslateDelegate()
                    .getTranslationX(MultiTranslateDelegate.INDEX_NAV_BAR_ANIM);
        }
        mIconsTranslationXFactory = new MultiPropertyFactory<>(mIconsContainer,
                VIEW_TRANSLATE_X, ICONS_TRANSLATION_X_CHANNELS_COUNT, Float::sum);
        mQsbAlphaChannels = new MultiValueAlpha(mQsb, ALPHA_CHANNEL_CHANNELS_COUNT);
        mQsbAlphaChannels.setUpdateVisibility(true);

        mIconsAlpha = new MultiValueAlpha(mIconsContainer, 1);
        mIconsAlpha.setUpdateVisibility(true);

        setUpBackground();
        setClipChildren(false);
        setClipToPadding(false);
    }

    private void setUpBackground() {
        var displayMetrics = getResources().getDisplayMetrics();
        float density = displayMetrics.density;

        int insetHorizontalLeft = preferenceManager.getHotseatBGHorizontalInsetLeft().get();
        if (insetHorizontalLeft <= 0) insetHorizontalLeft = (int) (18 * density);
        int insetHorizontalRight = preferenceManager.getHotseatBGHorizontalInsetRight().get();
        if (insetHorizontalRight <= 0) insetHorizontalRight = (int) (18 * density);
        int insetVerticalTop = preferenceManager.getHotseatBGVerticalInsetTop().get();
        if (insetVerticalTop <= 0) insetVerticalTop = (int) (6 * density);
        int insetVerticalBottom = preferenceManager.getHotseatBGVerticalInsetBottom().get();
        if (insetVerticalBottom <= 0) insetVerticalBottom = (int) (14 * density);

        float cornerRadiusDp = PreferenceCacheExtensionsKt.firstCached(
                preferenceManager2.getHotseatBackgroundCornerRadius());
        if (cornerRadiusDp < 24f) cornerRadiusDp = 28f;
        float cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp, displayMetrics);

        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int transparency = preferenceManager.getHotseatBGAlpha().get();
        if (transparency >= 100 || transparency <= 0) transparency = isDark ? 40 : 45;
        int alphaValue = (transparency * 255) / 100;

        int finalColor = isDark ? Color.argb(alphaValue, 45, 45, 48) : Color.argb(alphaValue, 245, 245, 250);
        GradientDrawable background = new GradientDrawable();
        background.setColor(finalColor);
        background.setCornerRadius(cornerRadius);
        int strokeWidth = Math.max(1, (int) (1 * density));
        int strokeColor = isDark ? Color.argb(40, 255, 255, 255) : Color.argb(80, 255, 255, 255);
        background.setStroke(strokeWidth, strokeColor);

        InsetDrawable bg = new InsetDrawable(background, insetHorizontalLeft, insetVerticalTop, insetHorizontalRight, insetVerticalBottom);
        setBackground(bg);
    }

    public MultiProperty getIconsTranslationX() {
        return mIconsTranslationXFactory.get(ICONS_TRANSLATION_X_NAV_BAR_ALIGNMENT);
    }

    @Nullable
    public MultiTranslateDelegate getQsbTranslationX() {
        return mQsbTranslationX;
    }

    public MultiValueAlpha.AlphaProperty getQsbAlpha(@HotseatQsbAlphaId int alphaChannelIndex) {
        return mQsbAlphaChannels.getProperty(alphaChannelIndex);
    }

    public MultiValueAlpha.AlphaProperty getIconsAlpha() {
        return mIconsAlpha.getProperty(0);
    }

    public MultiProperty getIconsMultiTranslateY() {
        return ((Reorderable) mIconsContainer).getTranslateDelegate().getTranslationY(INDEX_BUBBLE_ADJUSTMENT_ANIM);
    }

    public MultiProperty getQsbMultiTranslateY() {
        return ((Reorderable) mQsb).getTranslateDelegate().getTranslationY(INDEX_BUBBLE_ADJUSTMENT_ANIM);
    }

    public void setWorkspace(Workspace<?> w) {
        mWorkspace = w;
        getHotseatPagedView().setWorkspace(w);
    }

    public void setSendClicksToWorkspace(boolean sendClicksToWorkspace) {
        mSendClicksToWorkspace = sendClicksToWorkspace;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mSendClicksToWorkspace;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mSendClicksToWorkspace;
    }

    @Override
    public void setHorizontalInsets(float insets) {
        if (mQsb instanceof HorizontalInsettableView) {
            ((HorizontalInsettableView) mQsb).setHorizontalInsets(insets);
        }
    }

    @Override
    public void setInsets(Rect insets) {
        DeviceProfile dp = mActivity.getDeviceProfile();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();

        if (dp.isVerticalBarLayout()) {
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (dp.isSeascape()) {
                lp.gravity = Gravity.LEFT;
                lp.width = dp.hotseatBarSizePx + insets.left;
            } else {
                lp.gravity = Gravity.RIGHT;
                lp.width = dp.hotseatBarSizePx + insets.right;
            }
        } else {
            lp.gravity = Gravity.BOTTOM;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = dp.hotseatBarSizePx + insets.bottom;
        }
        Rect padding = dp.getHotseatPadding(getContext());
        setPadding(padding.left, padding.top, padding.right, padding.bottom);
        setLayoutParams(lp);
        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    public void updateSpringOffset() {
        getHotseatPagedView().updateSpringOffset();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        DeviceProfile dp = mActivity.getDeviceProfile();
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        if (dp.isVerticalBarLayout()) {
            width = dp.hotseatBarSizePx;
        } else {
            height = dp.hotseatBarSizePx;
        }
        int qsbWidth = dp.hotseatQsbWidth;
        int qsbHeight = dp.hotseatQsbHeight;
        mQsb.measure(makeMeasureSpec(qsbWidth, MeasureSpec.EXACTLY),
                makeMeasureSpec(qsbHeight, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        alignPageIndicator();
    }

    private void alignPageIndicator() {
        if (mPageIndicator != null && mPageIndicator.getVisibility() != GONE) {
            DeviceProfile dp = mActivity.getDeviceProfile();
            int pageIndicatorHeight = (int) (dp.getPageIndicatorSize() * preferenceManager2.getPageIndicatorHeightFactor().get());
            int paddingBottom = dp.getHotseatPadding(getContext()).bottom;
            int y = getMeasuredHeight() - pageIndicatorHeight - paddingBottom;
            mPageIndicator.layout(
                    (getMeasuredWidth() - mPageIndicator.getMeasuredWidth()) / 2,
                    y,
                    (getMeasuredWidth() + mPageIndicator.getMeasuredWidth()) / 2,
                    y + pageIndicatorHeight);
        }
    }

    public View getQsb() {
        return mQsb;
    }

    public View getIconsContainer() {
        return mIconsContainer;
    }

    public HotseatPagedView getHotseatPagedView() {
        return ((HotseatPagedView) mIconsContainer);
    }

    public CellLayout getShortcuts() {
        return getHotseatPagedView().getCurrentPage();
    }

    public CellLayout getCellLayout(int page) {
        return getHotseatPagedView().getPageAt(page);
    }

    public CellLayout getCellLayout() {
        return getHotseatPagedView().getCurrentPage();
    }

    public PageIndicatorDots getPageIndicator() {
        return mPageIndicator;
    }

    public CellPos getCellPos() {
        CellLayout targetLayout = getHotseatPagedView().getCurrentPage();
        return CellPosMapper.DEFAULT.mapModelToPresenter(new CellPos(0, 0, 0));
    }

    public CellLayoutLayoutParams getLayoutParams() {
        return (CellLayoutLayoutParams) super.getLayoutParams();
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.println(prefix + "Hotseat:");
        writer.println(prefix + "\tmHasVerticalHotseat=" + mHasVerticalHotseat);
        writer.println(prefix + "\tmWorkspace=" + mWorkspace);
        writer.println(prefix + "\tmSendClicksToWorkspace=" + mSendClicksToWorkspace);
    }
}
