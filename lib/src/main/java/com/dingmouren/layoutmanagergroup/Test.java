package com.dingmouren.layoutmanagergroup;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;

/**
 * Created by thunderPunch on 2017/2/15
 * Description:
 */

public class Test extends RecyclerView.LayoutManager {

    private static final String TAG = "LadderLayoutManager";
    private static final int INVALIDATE_SCROLL_OFFSET = Integer.MAX_VALUE;

    private int mItemWidth;
    private int mItemHeight;
    private int mBetweenHeight;
    private int mScrollOffset = INVALIDATE_SCROLL_OFFSET;
    private float mItemHeightWidthRatio;//childview的纵横比。所有childview都会按该纵横比展示
    private float mScale;//chidview每一层级相对于上一层级的缩放量
    private int mItemCount;
    private float mVanishOffset = 0;//vanish消失
    private Interpolator mInterpolator;//插值器
    private Context mContext;

    public Test(Context context) {
        this(1F, 0.9f);
        this.mContext = context;
    }

    /**
     * @param itemHeightWidthRatio childview的纵横比。所有childview都会按该纵横比展示
     * @param scale                chidview每一层级相对于上一层级的缩放量
     */
    public Test(float itemHeightWidthRatio, float scale) {
        this.mItemHeightWidthRatio = itemHeightWidthRatio;
        this.mScale = scale;
        this.mInterpolator = new LinearInterpolator();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
    }


    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0 || state.isPreLayout()) return; //sate.isPreLayout()正在测量视图
        removeAndRecycleAllViews(recycler);

        mItemWidth = getHorizontalSpace();//item的宽
        mItemHeight = (int) (mItemHeightWidthRatio * mItemWidth);//item的高
        mBetweenHeight = (int) (mItemHeight * 0.5f);//初始值间隔 540
        mItemCount = getItemCount();
        mScrollOffset = makeScrollOffsetWithinRange(mScrollOffset);//初始值：100个  1080 * 100

        fill(recycler);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int pendingScrollOffset = mScrollOffset + dy;
        mScrollOffset = Math.min(Math.max(mItemHeight, pendingScrollOffset), mItemCount * mItemHeight);
        fill(recycler);
        return mScrollOffset - pendingScrollOffset + dy;
    }


    @Override
    public boolean canScrollVertically() {
        return true;
    }


    //--------------------------------------------------------------

    private void fill(RecyclerView.Recycler recycler) {
        int bottomItemPosition = (int) Math.floor(mScrollOffset / mItemHeight);//>=1 初始值100
        int remainSpace = getVerticalSpace() - mItemHeight;//固定值774

        int bottomItemVisibleSize = mScrollOffset % mItemHeight;//初始值0,最下面的item，可见的高度
        final float offsetPercent = mInterpolator.getInterpolation(bottomItemVisibleSize * 1.0f / mItemHeight);//[0,1) 初始值0，最下面item可见高度的比例


        ArrayList<ItemLayoutInfo> layoutInfos = new ArrayList<>();
        for (int i = bottomItemPosition - 1, j = 1; i >= 0; i--, j++) {
            double maxOffset = mBetweenHeight * Math.pow(mScale, j);//mScale初始值0.9, mScale^j
            int start = (int) (remainSpace - offsetPercent * maxOffset);// space - mItemHeight,99个itemHeight
            float scaleXY = (float) (Math.pow(mScale, j - 1) * (1 - offsetPercent * (1 - mScale)));
            float positonOffset = offsetPercent;
            float layoutPercent = start * 1.0f / getVerticalSpace();
            ItemLayoutInfo info = new ItemLayoutInfo(start, scaleXY, positonOffset, layoutPercent);
            layoutInfos.add(0, info);

            remainSpace = (int) (remainSpace - maxOffset);
            if (remainSpace <= 0) {
                info.start = (int) (remainSpace + maxOffset);
                info.positionOffsetPercent = 0;
                info.layoutPercent = info.start / getVerticalSpace();
                info.scaleXY = (float) Math.pow(mScale, j - 1);
                break;
            }
        }

        if (bottomItemPosition < mItemCount) {
            final int start = getVerticalSpace() - bottomItemVisibleSize;
            layoutInfos.add(new ItemLayoutInfo(start, 1.0f,
                    bottomItemVisibleSize * 1.0f / mItemHeight, start * 1.0f / getVerticalSpace()).
                    setIsBottom());
        } else {
            bottomItemPosition = bottomItemPosition - 1;//99
        }

        int layoutCount = layoutInfos.size();
        final int startPos = bottomItemPosition - (layoutCount - 1);
        final int endPos = bottomItemPosition;
        final int childCount = getChildCount();
        Log.e(TAG,"startPos:"+startPos+" endPos:"+endPos+" childCount:"+childCount);
        for (int i = childCount - 1; i >= 0; i--) {//回收
            View childView = getChildAt(i);
            int pos = getPosition(childView);
            if (pos > endPos || pos < startPos) {
                removeAndRecycleView(childView, recycler);
            }
        }

        detachAndScrapAttachedViews(recycler);

        for (int i = 0; i < layoutCount; i++) {
            fillChild(recycler.getViewForPosition(startPos + i), layoutInfos.get(i));
        }
    }

    private void fillChild(View view, ItemLayoutInfo layoutInfo) {
        addView(view);
        measureChildWithExactlySize(view);
        layoutDecoratedWithMargins(view, 0, layoutInfo.start,  mItemWidth, layoutInfo.start + mItemHeight );
        ViewCompat.setScaleX(view, layoutInfo.scaleXY);//控制缩放
        ViewCompat.setScaleY(view, layoutInfo.scaleXY);
    }

    private void measureChildWithExactlySize(View child) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(
                mItemWidth - lp.leftMargin - lp.rightMargin, View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(
                mItemHeight - lp.topMargin - lp.bottomMargin, View.MeasureSpec.EXACTLY);
        child.measure(widthSpec, heightSpec);
    }

    private int makeScrollOffsetWithinRange(int scrollOffset) {
        return Math.min(Math.max(mItemHeight, scrollOffset), mItemCount * mItemHeight);
    }


    /**
     * 获取item在竖直方向上可以显示的距离
     *
     * @return
     */
    public int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    /**
     * 获取item在水平方向上可以显示的距离
     *
     * @return
     */
    public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }


    //-----------------------------------------------------------------------------------------------
    private static class ItemLayoutInfo {
        float scaleXY;
        float layoutPercent;
        float positionOffsetPercent;
        int start;
        boolean isBottom;

        private ItemLayoutInfo(int top, float scale, float positonOffset, float percent) {
            this.start = top;
            this.scaleXY = scale;
            this.positionOffsetPercent = positonOffset;
            this.layoutPercent = percent;
        }

        private ItemLayoutInfo setIsBottom() {
            isBottom = true;
            return this;
        }

    }

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
