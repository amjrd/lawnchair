/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.celllayout.CellStagger;
import com.android.launcher3.config.DeviceProfile;
import com.android.launcher3.pageindicators.PageIndicatorDot;
import com.android.launcher3.pm.UserHandle;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.InsetStyleContainer;

import app.lawnchair.wallpaper.LawnchairWallpaperColor;
import app.lawnchair.qsb.QsbLayout;
import app.lawnchair.qsb.QsbView;
import app.lawnchair.qsb.SmartSpaceView;
import app.lawnchair.preferences.PreferenceManager2;
import app.lawnchair.hotseat.DisabledHotseat;
import app.lawnchair.hotseat.HotseatMode;
import app.lawnchair.hotseat.InsettablePagedView;
import app.lawnchair.hotseat.LawnchairHotseat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * View class that represents the hotseat (aka, the dock).
 */
public class Hotseat extends FrameLayout implements Insettable,
        HorizontalInsettable, LawnchairHotseat {

    // Ratio of hotseat height to transfer to workspace if needed to
    // notch design specs
    public static final float WORKSPACE_TO_HOTSEAT_HEIGHT_RATIO = 0.44f;

    public static final int ALPHA_CHANNEL_PREVIEW_RENDERER = 0;
    public static final int ALPHA_CHANNEL_TASKBAR_SCALER = 1;
    public static final int ALPHA_CHANNEL_ASSISTANT_VISIBILITY = 2;
    public static final int ALPHA_CHANNEL_QUICK_SWITCH = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALPHA_CHANNEL_PREVIEW_RENDERER,
            ALPHA_CHANNEL_TASKBAR_SCALER,
            ALPHA_CHANNEL_ASSISTANT_VISIBILITY,
            ALPHA_CHANNEL_QUICK_SWITCH})
    public @interface AlphaChannel {}

    private final boolean mHasVerticalHotseat;
    private final ActivityContext mActivity;

    private Workspace<?> mWorkspace;
    private boolean mSendClicksToWorkspace;

    private final View mQsb;
    private final View mIconsContainer;
    private final PageIndicatorDot mPageIndicator;

    @ViewDebug.ExportedProperty(category = "launcher")
    @Nullable
    private final MultiValueAlpha.AlphaProperty mQsbAlpha;
    private final MultiValueAlpha mIconsAlpha;

    private PreferenceManager2 preferenceManager;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivity = ActivityContext.lookupContext(context);
        mHasVerticalHotseat = false;

        preferenceManager = PreferenceManager2.getInstance(context);

        mQsb = LayoutInflater.from(context).inflate(preferenceManager.getHotseatMode().get().getLayoutResourceId(), this, false);
        addView(mQsb);

        mIconsContainer = LayoutInflater.from(context).inflate(R.layout.hotseat_icons_container, this, false);
        addView(mIconsContainer);

        mPageIndicator = (PageIndicatorDot) LayoutInflater.from(context).inflate(R.layout.hotseat_page_indicator, this, false);
        addView(mPageIndicator);

        mIconsAlpha = new MultiValueAlpha(mIconsContainer, 4);
        mIconsAlpha.setUpdateVisibility(true);

        setupBackground();
    }

    private void setupBackground() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float density = displayMetrics.density;

        int insetHorizontalLeft = preferenceManager.getHotseatBgHorizontalInsetLeft().get();
        if (insetHorizontalLeft <= 0) {
            insetHorizontalLeft = (int) (18 * density);
        }

        int insetHorizontalRight = preferenceManager.getHotseatBgHorizontalInsetRight().get();
        if (insetHorizontalRight <= 0) {
            insetHorizontalRight = (int) (18 * density);
        }

        int insetVerticalTop = preferenceManager.getHotseatBgVerticalInsetTop().get();
        if (insetVerticalTop <= 0) {
            insetVerticalTop = (int) (6 * density);
        }

        int insetVerticalBottom = preferenceManager.getHotseatBgVerticalInsetBottom().get();
        if (insetVerticalBottom <= 0) {
            insetVerticalBottom = (int) (14 * density);
        }

        float cornerRadiusDp = preferenceManager.getHotseatBgBackgroundCornerRadius().get();
        int cornerRadius = 24;
        if (cornerRadiusDp < 24) {
            cornerRadius = (int) cornerRadiusDp;
        }
        int cornerRadiusPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                cornerRadius,
                displayMetrics
        );

        boolean isDark = (getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        int transparency = preferenceManager.getHotseatBgAlpha().get();
        int alphavalue = (transparency * 255) / 100;

        int finalColor = isDark ? Color.argb(alphavalue, 45, 45, 48) : Color.argb(alphavalue, 245, 245, 250);

        GradientDrawable background = new GradientDrawable();
        background.setColor(finalColor);
        background.setCornerRadius(cornerRadiusPx);

        int strokeWidth = (int) (1 * density);
        int strokeColor = isDark ? Color.argb(40, 255, 255, 255) : Color.argb(40, 0, 0, 0);

        background.setStroke(strokeWidth, strokeColor);

        InsetDrawable bg = new InsetDrawable(background,
                insetHorizontalLeft, insetVerticalTop, insetHorizontalRight, insetVerticalBottom);
        setBackground(bg);
    }

    @Override
    public MultiValueAlpha.AlphaProperty getIconsAlpha(@AlphaChannel int channel) {
        return mIconsAlpha.getProperty(channel);
    }

    @Override
    public View getIconsContainer() {
        return mIconsContainer;
    }

    @Override
    public MultiValueAlpha.AlphaProperty getQsbAlpha() {
        return mQsbAlpha;
    }

    @Override
    public View getQsb() {
        return mQsb;
    }

    @Override
    public boolean isVertical() {
        return mHasVerticalHotseat;
    }

    @Override
    public void setWorkspace(Workspace<?> workspace) {
        mWorkspace = workspace;
        getHotseatPagedView().setWorkspace(workspace);
    }

    @Override
    public void setSendClicksToWorkspace(boolean sendClicksToWorkspace) {
        mSendClicksToWorkspace = sendClicksToWorkspace;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mSendClicksToWorkspace) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSendClicksToWorkspace) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setHorizontalInsets(float insets) {
        if (mQsb instanceof HorizontalInsettable) {
            ((HorizontalInsettable) mQsb).setHorizontalInsets(insets);
        }
    }

    @Override
    public void setInsets(Rect insets) {
        DeviceProfile dp = mActivity.getDeviceProfile();
        setPadding(insets.left, insets.top, insets.right, insets.bottom);
    }

    @Override
    public void setDragAndDropAccessibilityDelegate(
            DragAndDropAccessibilityDelegate accessibilityDelegate) {
        if (accessibilityDelegate instanceof Insettable) {
            ((Insettable) accessibilityDelegate).setInsets(this, new Rect());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        alignPageIndicator();
    }

    private void alignPageIndicator() {
        if (mPageIndicator != null && mPageIndicator.getVisibility() == VISIBLE) {
            DeviceProfile dp = mActivity.getDeviceProfile();
            int pageIndicatorHeight = (int) (dp.getPageIndicatorSize() * preferenceManager.getPageIndicatorHeightFactor().get());
            int paddingBottom = getPaddingBottom();
            int y = getMeasuredHeight() - pageIndicatorHeight - paddingBottom;

            mPageIndicator.layout(
                    (getMeasuredWidth() - mPageIndicator.getMeasuredWidth()) / 2,
                    y,
                    (getMeasuredWidth() + mPageIndicator.getMeasuredWidth()) / 2,
                    y + pageIndicatorHeight
            );
        }
    }

    public InsettablePagedView getHotseatPagedView() {
        return (InsettablePagedView) mIconsContainer;
    }

    public CellLayout getCellLayout(int page) {
        return getHotseatPagedView().getPageAt(page);
    }

    public CellLayout getCurrentPageLayout() {
        return getHotseatPagedView().getCurrentPage();
    }

    public PageIndicatorDot getPageIndicator() {
        return mPageIndicator;
    }
}
