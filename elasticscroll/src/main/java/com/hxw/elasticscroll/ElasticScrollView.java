package com.hxw.elasticscroll;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;

/**
 * @author hxw on 2017/6/28.
 * 弹性滑动布局,和scrollView 一样的用法
 */

public class ElasticScrollView extends NestedScrollView {
    /**
     * 移动因子, 是一个百分比, 比如手指移动了100px, 那么View就只移动50px
     * 目的是达到一个延迟的效果
     */
    private static final float MOVE_FACTOR = 0.5f;

    /**
     * 松开手指后, 界面回到正常位置需要的动画时间
     */
    private static final int ANIM_TIME = 300;
    /**
     * 用于记录正常的布局位置
     */
    private final Rect originalRect = new Rect();
    /**
     * ScrollView的子View, 也是ScrollView的唯一一个子View
     */
    private View contentView;
    /**
     * 手指按下时的Y值, 用于在移动时计算移动距离
     * 如果按下时不能上拉和下拉, 会在手指移动时更新为当前手指的Y值
     */
    private float startY;

    public ElasticScrollView(Context context) {
        super(context);
    }

    public ElasticScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            contentView = getChildAt(0);
        }
    }

    /**
     * 在事件分发中, 处理上拉和下拉的逻辑
     *
     * @param ev 动作类型
     * @return true or false
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (contentView == null) {
            return super.onTouchEvent(ev);
        }

        int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //记录按下时的Y值
                startY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:

                if (contentView.getTop() == originalRect.top) {
                    break;  //如果没有移动布局， 则跳过执行
                }

                // 开启动画
                TranslateAnimation anim = new TranslateAnimation(0, 0, contentView.getTop(),
                        originalRect.top);
                anim.setDuration(ANIM_TIME);
                contentView.startAnimation(anim);
                /*
                 * 把布局设置回原始正常的位置, 这句代码不写在动画的监听里, 写在监听的开始和结束里视图都会有闪烁。
                 * 这里不用属性动画的原因是属性动画移动后视图显示不会回到原来的位置, 而下面这句移动视图位置后，
                 * 显示和视图位置的差还是被保留着。
                 */
                contentView.layout(originalRect.left, originalRect.top,
                        originalRect.right, originalRect.bottom);
                break;
            case MotionEvent.ACTION_MOVE:

                boolean down = isCanPullDown();
                boolean up = isCanPullUp();
                //在移动的过程中， 既没有滚动到可以上拉的程度， 也没有滚动到可以下拉的程度
                if (!down && !up) {
                    //记录现在的Y值
                    startY = ev.getY();
                    break;
                }

                //计算手指移动的距离
                float nowY = ev.getY();
                int deltaY = (int) (nowY - startY);

                //是否应该移动布局
                boolean shouldMove =
                        (down && deltaY > 0)    //可以下拉， 并且手指向下移动
                                || (up && deltaY < 0)//可以上拉， 并且手指向上移动
                                || (up && down); //既可以上拉也可以下拉（这种情况出现在ScrollView包裹的控件比ScrollView还小）
                if (shouldMove) {
                    //计算偏移量
                    int offset = (int) (deltaY * MOVE_FACTOR);
                    //随着手指的移动而移动布局
                    contentView.layout(originalRect.left, originalRect.top + offset,
                            originalRect.right, originalRect.bottom + offset);
                    //若是移动了布局, 就不走super的逻辑
                    return true;
                } else {
                    contentView.layout(originalRect.left, originalRect.top,
                            originalRect.right, originalRect.bottom);
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 这方法是在 onFinishInflate 之后调用
     *
     * @param changed 是否改变
     * @param l       left
     * @param t       top
     * @param r       right
     * @param b       bottom
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (contentView == null) {
            return;
        }

        //ScrollView中的唯一子控件的位置信息, 这个位置信息在整个控件的生命周期中保持不变
        originalRect.set(contentView.getLeft(), contentView.getTop(),
                contentView.getRight(), contentView.getBottom());

    }

    /**
     * 判断是否滚动到顶部，可以继续下拉
     */
    private boolean isCanPullDown() {
        return getScrollY() == 0 || contentView.getTop() > originalRect.top;
    }

    /**
     * 判断是否滚动到底部，可以继续上拉
     */
    private boolean isCanPullUp() {
        return contentView.getHeight() <= getHeight() + getScrollY()
                || contentView.getTop() < originalRect.top;
    }
}
