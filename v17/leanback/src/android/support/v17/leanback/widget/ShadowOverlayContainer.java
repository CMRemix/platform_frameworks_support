/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.content.Context;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Rect;

/**
 * ShadowOverlayContainer Provides a SDK version independent wrapper container
 * to take care of shadow and/or color overlay.
 * <p>
 * Shadow and color dimmer overlay are both optional.  When shadow is used,  it's
 * user's responsibility to properly call setClipChildren(false) on parent views if
 * the shadow can appear outside bounds of parent views.
 * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
 * before using shadow.  Depending on sdk version, optical bounds might be applied
 * to parent.
 * </p>
 * <p>
 * {@link #initialize(boolean, boolean, boolean)} must be first called on the container
 * to initialize shadows and/or color overlay.  Then call {@link #wrap(View)} to insert
 * wrapped view into container.
 * </p>
 * <p>
 * Call {@link #setShadowFocusLevel(float)} to control shadow alpha.
 * </p>
 * <p>
 * Call {@link #setOverlayColor(int)} to control overlay color.
 * </p>
 */
public class ShadowOverlayContainer extends ViewGroup {

    private boolean mInitialized;
    private View mColorDimOverlay;
    private Object mShadowImpl;
    private View mWrappedView;
    private static final Rect sTempRect = new Rect();

    public ShadowOverlayContainer(Context context) {
        this(context, null, 0);
    }

    public ShadowOverlayContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowOverlayContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Return true if the platform sdk supports shadow.
     */
    public static boolean supportsShadow() {
        return ShadowHelper.getInstance().supportsShadow();
    }

    /**
     * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
     * before using shadow.  Depending on sdk version, optical bounds might be applied
     * to parent.
     */
    public static void prepareParentForShadow(ViewGroup parent) {
        ShadowHelper.getInstance().prepareParent(parent);
    }

    /**
     * Initialize shadows, color overlay.
     * @deprecated use {@link #initialize(boolean, boolean, boolean)} instead.
     */
    @Deprecated
    public void initialize(boolean hasShadow, boolean hasColorDimOverlay) {
        initialize(hasShadow, hasColorDimOverlay, true);
    }

    /**
     * Initialize shadows, color overlay, and rounded corners.  All are optional.
     */
    public void initialize(boolean hasShadow, boolean hasColorDimOverlay, boolean roundedCorners) {
        if (mInitialized) {
            throw new IllegalStateException();
        }
        mInitialized = true;
        if (hasShadow) {
            mShadowImpl = ShadowHelper.getInstance().addShadow(this, roundedCorners);
        } else if (roundedCorners) {
            RoundedRectHelper.getInstance().setRoundedRectBackground(this,
                    android.graphics.Color.TRANSPARENT);
        }
        if (hasColorDimOverlay) {
            mColorDimOverlay = LayoutInflater.from(getContext())
                    .inflate(R.layout.lb_card_color_overlay, this, false);
            addView(mColorDimOverlay);
        }
    }

    /**
     * Set shadow focus level (0 to 1). 0 for unfocused, 1f for fully focused.
     */
    public void setShadowFocusLevel(float level) {
        if (mShadowImpl != null) {
            if (level < 0f) {
                level = 0f;
            } else if (level > 1f) {
                level = 1f;
            }
            ShadowHelper.getInstance().setShadowFocusLevel(mShadowImpl, level);
        }
    }

    /**
     * Set color (with alpha) of the overlay.
     */
    public void setOverlayColor(int overlayColor) {
        if (mColorDimOverlay != null) {
            mColorDimOverlay.setBackgroundColor(overlayColor);
        }
    }

    /**
     * Inserts view into the wrapper.
     */
    public void wrap(View view) {
        if (!mInitialized || mWrappedView != null) {
            throw new IllegalStateException();
        }
        if (mColorDimOverlay != null) {
            addView(view, indexOfChild(mColorDimOverlay));
        } else {
            addView(view);
        }
        mWrappedView = view;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mWrappedView == null) {
            throw new IllegalStateException();
        }
        // padding and child margin are not supported.
        // first measure the wrapped view, then measure the shadow view and/or overlay view.
        int childWidthMeasureSpec, childHeightMeasureSpec;
        LayoutParams lp = mWrappedView.getLayoutParams();
        if (lp.width == LayoutParams.MATCH_PARENT) {
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec
                    (MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
        }
        if (lp.height == LayoutParams.MATCH_PARENT) {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec
                    (MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY);
        } else {
            childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
        }
        mWrappedView.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        int measuredWidth = mWrappedView.getMeasuredWidth();
        int measuredHeight = mWrappedView.getMeasuredHeight();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == mWrappedView) {
                continue;
            }
            lp = child.getLayoutParams();
            if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec
                        (measuredWidth, MeasureSpec.EXACTLY);
            } else {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
            }

            if (lp.height == LayoutParams.MATCH_PARENT) {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec
                        (measuredHeight, MeasureSpec.EXACTLY);
            } else {
                childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
            }
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();
                child.layout(0, 0, width, height);
            }
        }
        if (mWrappedView != null) {
            sTempRect.left = (int) mWrappedView.getPivotX();
            sTempRect.top = (int) mWrappedView.getPivotY();
            offsetDescendantRectToMyCoords(mWrappedView, sTempRect);
            setPivotX(sTempRect.left);
            setPivotY(sTempRect.top);
        }
    }

}
