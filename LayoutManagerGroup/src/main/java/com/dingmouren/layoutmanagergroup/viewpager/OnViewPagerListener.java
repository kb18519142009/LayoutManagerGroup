package com.dingmouren.layoutmanagergroup.viewpager;

/**
 * Created by 钉某人
 * github: https://github.com/DingMouRen
 * email: naildingmouren@gmail.com
 * 用于ViewPagerLayoutManager的监听
 */

public interface OnViewPagerListener {

    /*释放的监听*/
    void onPageRelease(int position);

    /*选中的监听以及判断是否滑动到底部*/
    void onPageSelected(int position,boolean isBottom);

}
