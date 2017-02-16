package top.wuhaojie.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wuhaojie on 2016/7/15 11:36.
 * 1.1 周荣华 增加数字和文字的滚动。
 */
public class ScrollNumber extends View {
    private static final String TAG = ScrollNumber.class.getSimpleName();

    /** 滚动策略模式 */
    public enum Strategy {
        /** 自然模式(从小到大循环播放) */
        NATURAL,
        /** 数字大小模式(根据数字大小向上递增或者向下递减播放) */
        BIGGER_OR_SMALLER
    }

    /** 总共的数字个数 0~9 */
    public static final int NUMBER_CHARS_TOTAL = 10;
    /** 加速器采用 0.05~0.80 */
    private static final float ACCELERATE_MAX_VALUE = 0.8f;
    /** 加速器偏移点 0.24(10个1500ms) */
    private static final float ACCELERATE_OFFSET_DEFAULT = 0.24f;
    private static final float ACCELERATE_OFFSET_MAX = 0.45f;
    private static final float ACCELERATE_OFFSET_MIN = 0.1f;
    /**
     * 移动变化百分比
     * 说明: 最大不能超过1.0f。
     */
    private static final float SCROLL_RATE_DEFAULT = 0.15f;
    /**
     * 最大移动变化百分比
     * 说明: 一个数字最少显示两次，因此百分比过超过0.5f
     */
    private static final float SCROLL_RATE_MAX = 0.5f;
    /**
     * 最小移动变化百分比
     * 说明: 一个数字最多显示百次，因此百分比过超过0.1f
     */
    private static final float SCROLL_RATE_MIN = 0.1f;
    /** 播放时长(10个1500ms) */
    public static final long ONE_LOOP_DURATION = 1500L;
    /**
     * 动画默认播放轮数 1轮
     * 说明: 如果数字相同，就不会播放，这种情况需要外层调用增加一轮。
     */
    public static final int SCROLL_LOOP_DEFAULT = 1;
    /**
     * 动画最大播放轮数 5轮
     * 说明: 播放轮数太多会导致占用太多的资源。
     */
    public static final int SCROLL_LOOP_MAX = 5;
    /**
     * 上下文
     */
    private Context mContext;
    /** 数字Text画笔 */
    private Paint mPaint;
    /** 文字Text画笔 */
    private Paint mTextPaint;
    /** 数字动画播放加速器 */
    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();
    /** ScrollNumberCallback回调处理 */
    private MultiScrollNumber.IScrollNumberCallback callback;

    /**
     * 全部滚动的步长
     */
    private int mDeltaNum;
    /** 当前播放步长 */
    private int mLeftStep;
    /**
     * 基准动画动画播放轮数(一轮是 0~9)
     * 说明: 例如 1-->2 如果增加一轮 1-->2-->3...->2
     */
    private int mAnimLoop = SCROLL_LOOP_DEFAULT;
    /**
     * 步长级别调整(即A-->B的基础上多播几轮)
     * 说明: 例如 1-->2 如果增加一轮 1-->2-->3...->2
     */
    private int mAnimLoopAdjust = 0;
    /**
     * 当前数字值(空数字采用空白字符占位)
     */
    private int mCurNum;
    /**
     * 目标数字的值
     */
    private int mTargetNum;
    /** 开始显示的文字值 */
    private String mCurText;
    /**
     * 下一个数字Text的值
     * 1)数字切换到数字 0-->1-->2-->3-->4-->5-->6-->7-->8-->9
     * 2)数字(文字或空白字符)切换到文字 文字-->文字
     * 3)文字(空白字符)切换到数字 文字-->0...->数字
     * 4)数字切换到无(空白字符) 数字-->消失(父类移除当前节点)
     * 5)文字切换到无(空白字符) 文字-->消失(父类移除移除当前节点)
     */
    private String mNextText;
    /** 目标显示的文字值 */
    private String mTargetText;
    /** 数字Text向上滚动的偏移量 */
    private float mOffset;
    /** 数字Text向上移动的偏移量百分比 */
    private float mOffsetRate = SCROLL_RATE_DEFAULT;
    /**
     * 加速度偏移值
     * 说明: 10以内的步长变化进行加速度偏移值的微调
     */
    private float mAccelateOffset = ACCELERATE_OFFSET_DEFAULT;
    /** 播放策略(默认为自然模式) */
    private Strategy mStrategy = Strategy.NATURAL;
    /** 数字动画播放模式 */
    private MultiScrollNumber.Mode mAnimMode = MultiScrollNumber.Mode.START_FIRST_ARRIVAL_LAST;
    /** 变化方向参数(向上递增播放) */
    private int mAnimDirection = 1;
    /** Text字体X中心位置 */
    private int mTextCenterX;
    /** Text字体高度 */
    private int mTextHeight;
    /** 文字Text字体高度 */
    private int mUnitTextHeight;
    /** 数字Text内容边框 */
    private Rect mTextBounds = new Rect();
    /** 文字Text内容边框 */
    private Rect mUnitTextBounds = new Rect();
    /** Text字体大小 */
    private int mTextSize = sp2px(MultiScrollNumber.SCROLL_NUMBER_TEXT_SIZE);
    /** 文字Text字体大小 */
    private int mUnitTextSize = sp2px(MultiScrollNumber.SCROLL_NUMBER_TEXT_SIZE);
    /** Text字体颜色 */
    private int mTextColor = 0xFF000000;
    /** 文字Text字体颜色 */
    private int mUnitTextColor = 0xFF000000;
    /** 数字Text字体Typeface */
    private Typeface mTypeface;
    /** 文字Text字体Typeface */
    private Typeface mUnitTypeface;
    /** 起始Text是否是数字 */
    private boolean isFromNumeric;
    /** 结束Text是否是数字 */
    private boolean isToNumeric;
    /** 是否需要移除当前项 */
    private boolean isNeedRemove;

    public ScrollNumber(Context context) {
        this(context, null);
    }

    public ScrollNumber(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollNumber(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        //初始化数字Text画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(mTextColor);
        //初始化文字Text画笔
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(mUnitTextSize);
        mTextPaint.setColor(mUnitTextColor);

        if (mTypeface != null) mPaint.setTypeface(mTypeface);

        //测量数字Text高度
        measureTextHeight();
        //测量文字Text高度
        measureUnitTextHeight();
        //设置内边距
        this.setPadding(dp2px(2),dp2px(2),dp2px(2),dp2px(2));
    }

    /**
     * 设置数字起始和目标Text
     * 说明: 默认动画播放一轮，播放时长为1500ms。一轮(0~9 个数字步长) 两轮(10~19个数字步长)
     *
     * @param from : 开始数字Text
     * @param to : 结束数字Text
     * @param delay : 开始播放动画的延时
     */
    public void setNumberChar(final String from, final String to, long delay) {
        setNumberChar(from, to, delay, ONE_LOOP_DURATION);
    }

    /**
     * 设置数字起始和目标Text
     * 说明: 默认动画播放一轮。一轮(0~9 个数字步长) 两轮(10~19个数字步长)
     *
     * @param from : 开始数字Text
     * @param to : 结束数字Text
     * @param delay : 开始播放动画的延时
     * @param duration ：动画播放时长
     */
    public void setNumberChar(final String from, final String to, long delay, long duration) {
        setNumberChar(from, to, delay, SCROLL_LOOP_DEFAULT, 0, duration);
    }

    /**
     * 设置数字起始和目标Text
     *
     * @param from : 开始数字Text
     * @param to : 结束数字Text
     * @param delay : 开始播放动画的延时
     * @param animLoop : 基准动画播放轮数
     * @param animStepMax : 预设最大动画播放总步长
     * @param duration ：动画播放时长
     */
    public void setNumberChar(final String from, final String to, long delay, final int animLoop, final int animStepMax, final long duration) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                calculateFromAndTargetNumber(from, to, animLoop, animStepMax, duration);
            }
        }, delay);
    }

    /**
     * 计算开始和结束的数值
     *
     * @param from : 开始数字Text
     * @param to : 结束数字Text
     * @param animLoop : 基准动画播放轮数
     * @param animStepMax : 预设最大动画播放总步长
     * @param duration ：动画播放时长
     */
    private void calculateFromAndTargetNumber(String from, String to, int animLoop, int animStepMax, long duration) {
        isNeedRemove = isEmptyChar(to);
        mCurText = GetStringNoEmpty(from);
        mTargetText = GetStringNoEmpty(to);
        mCurNum = getNumberValue(mCurText);
        mTargetNum = getNumberValue(mTargetText);
        mAnimLoop = Math.min(SCROLL_LOOP_MAX, Math.max(animLoop, SCROLL_LOOP_DEFAULT));
        //计算总共步长(默认播放一轮)
        mDeltaNum = calDeltaNum(mCurText, mTargetText, animStepMax);
        mLeftStep = mDeltaNum;
        //计算数字播放移动百分比(不能超过100%)
        calculateAccelateOffsetRate(mDeltaNum, duration);
        //计算下一个数字Text
        initNextNumberText(mCurText);
        Log.d(TAG, "calculateFromAndTargetNumber CurText: " + mCurText + " NextText: " + mNextText + " TargetText: " + mTargetText + " mDeltaNum:" + mDeltaNum + " OffsetRate:" + mOffsetRate + " AccelateOffset:" + mAccelateOffset);
        //刷新
        invalidate();
    }

    /** 计算初始化的下一个字符 */
    private void initNextNumberText(String mCurText) {
        if (isToNumeric) {
            //数字动画播放
            int tmpNum = 0;
            if(mAnimDirection > 0) {
                //向上移动(进行数据补位)
                tmpNum = (mTargetNum - mDeltaNum % NUMBER_CHARS_TOTAL + NUMBER_CHARS_TOTAL) % NUMBER_CHARS_TOTAL;
                mNextText = String.valueOf(calNextNumber(tmpNum));
            } else {
                //向下移动(当前起始值递减)
                tmpNum = mCurNum;
                mNextText = String.valueOf(calNextNumber(tmpNum));;
            }
        } else {
            //文字型动画播放(直接切换到目标数字Text)
            mNextText = mTargetText;
        }
    }

    /**
     * 计算数字移动的百分比
     * 说明：不能超过最大数字播放的移动百分比(10个数字1500ms播放完成)
     */
    private void calculateAccelateOffsetRate(int totalStep, long duration) {
        //采样的基点(10个数字播放时长1500ms，移动百分比0.15f)
        int curTotal = totalStep <= 0 ? 1 : totalStep;
        double accelerateOffset = ACCELERATE_OFFSET_DEFAULT;
        double rate = SCROLL_RATE_DEFAULT;
        double d1 = 0;
        double d2 = 0;
        //计算速度倍数(四舍五入)
        int scale = 1;
        if(MultiScrollNumber.Mode.START_FIRST_ARRIVAL_LAST == mAnimMode
                || MultiScrollNumber.Mode.START_FIRST_ARRIVAL_FIRST == mAnimMode
                || MultiScrollNumber.Mode.START_ARRIVAL_SAME_TIME == mAnimMode) {
            //需要保证数字到达顺序的动画模式(采用固定的加速度 等长时间播放 因此根据基准动画比较)
            d1 = (double)ONE_LOOP_DURATION / NUMBER_CHARS_TOTAL;
            d2 = duration / (mAnimLoop * NUMBER_CHARS_TOTAL);
        } else {
            //只滚一轮情况下
            d1 = (double)ONE_LOOP_DURATION / NUMBER_CHARS_TOTAL;
            d2 = duration / curTotal;
        }
        if(d1 >= d2) {
            //比普通播放速度快
            scale = (int) Math.round(d1 / d2);
            rate = (float) (SCROLL_RATE_DEFAULT + (SCROLL_RATE_MAX - SCROLL_RATE_DEFAULT) / 10 * scale);
            accelerateOffset = ACCELERATE_OFFSET_DEFAULT + ((ACCELERATE_OFFSET_MAX - ACCELERATE_OFFSET_DEFAULT) / 10 * scale);
        } else {
            scale = (int) Math.round(d2 / d1);
            rate = (float) (SCROLL_RATE_DEFAULT - (SCROLL_RATE_DEFAULT - SCROLL_RATE_MIN) / 10 * scale);
            accelerateOffset = ACCELERATE_OFFSET_DEFAULT + ((ACCELERATE_OFFSET_DEFAULT - ACCELERATE_OFFSET_MIN) / 10 * scale);
        }
        mOffsetRate = (float) Math.min(SCROLL_RATE_MAX, Math.max(SCROLL_RATE_MIN, rate));
        mAccelateOffset = (float) Math.min(ACCELERATE_OFFSET_MAX, Math.max(ACCELERATE_OFFSET_MIN, accelerateOffset));
    }

    /**
     * 计算总共变化的步长
     * 说明: 简单化。数字之间多步长动画，带文字的一个步长的动画。
     *
     * @param from : 开始数字Text
     * @param to : 结束数字Text
     * @param animStepMax : 预设最大动画播放总步长
     */
    private int calDeltaNum(String from, String to, int animStepMax) {
        isFromNumeric = isNumeric(from);
        isToNumeric = isNumeric(to);
        int totalStep = 0;
        if (isToNumeric) {
            int startNum = getNumberValue(from);
            int stopNum = getNumberValue(to);
            //数字之间的动画需要播放的步长个数
            if(Strategy.NATURAL == mStrategy) {
                //自然模式
                totalStep = (stopNum - startNum + NUMBER_CHARS_TOTAL) % NUMBER_CHARS_TOTAL + (mAnimLoop - 1) * NUMBER_CHARS_TOTAL;
                totalStep = Math.max(totalStep, animStepMax);
                mAnimDirection = 1;
            } else {
                //最短路径模式
                totalStep = Math.abs(stopNum - startNum) + (mAnimLoop - 1) * NUMBER_CHARS_TOTAL;
                totalStep = Math.max(totalStep, animStepMax);
                //移动方向(1表示向上递增播放 -1表示向下递减播放)
                mAnimDirection = stopNum >= startNum ? 1 : -1;
            }
        } else {
            //文字的动画一个步长完成
            totalStep = from.equals(to) ? 0 : 1;
            mAnimDirection = 1;
        }
        return totalStep;
    }

    /**
     * 计算下一个数字Text
     * 说明: 通过当前数字Text，计算下一个数字Text
     */
    private void calNextNumberText(String str) {
        if (isToNumeric) {
            //数字动画播放
            mCurNum = getNumberValue(str);
            mNextText = String.valueOf(calNextNumber(mCurNum));
        } else {
            //文字型动画播放(直接切换到目标数字Text)
            mNextText = mTargetText;
        }
    }

    /** 设置动画模式 */
    public void setScollAnimationMode(MultiScrollNumber.Mode mode) {
        this.mAnimMode = mode;
        calStrategy();
    }

    /** 计算当前模式的数字播放策略 */
    private ScrollNumber.Strategy calStrategy() {
        ScrollNumber.Strategy strategy = ScrollNumber.Strategy.NATURAL;
        if(MultiScrollNumber.Mode.SCOREBOARD == mAnimMode) {
            //如果记分牌模式动画播放采用最短距离策略
            strategy = ScrollNumber.Strategy.BIGGER_OR_SMALLER;
        }
        return strategy;
    }

    /** 设置数字滚动策略 */
    public void setStrategy(Strategy strategy) {
        this.mStrategy = strategy;
    }

    /** 设置数字 */
    public void setTextSize(int textSize) {
        this.mTextSize = sp2px(textSize);
        mPaint.setTextSize(mTextSize);
        measureTextHeight();
        requestLayout();
        invalidate();
    }

    /** 设置数字单位字体大小 */
    public void setUnitTextSize(int unitTextSize) {
        //设置文字Text字体大小和画笔
        this.mUnitTextSize = sp2px(unitTextSize);
        mTextPaint.setTextSize(mUnitTextSize);
        requestLayout();
        invalidate();
    }

    /** 设置数字字体 */
    public void setTextFont(String fileName) {
        if (TextUtils.isEmpty(fileName))
            throw new IllegalArgumentException("please check file name end with '.ttf' or '.otf'");
        mTypeface = Typeface.createFromAsset(mContext.getAssets(), fileName);
        if (mTypeface == null) throw new RuntimeException("please check your font!");
        mPaint.setTypeface(mTypeface);
        requestLayout();
        invalidate();
    }

    public void setUnitTextFont(String fileName) {
        if (TextUtils.isEmpty(fileName))
            throw new IllegalArgumentException("please check file name end with '.ttf' or '.otf'");
        mUnitTypeface = Typeface.createFromAsset(mContext.getAssets(), fileName);
        if (mUnitTypeface == null) throw new RuntimeException("please check your font!");
        mTextPaint.setTypeface(mUnitTypeface);
        requestLayout();
        invalidate();
    }

    /** 设置数字Text字体颜色 */
    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        mPaint.setColor(textColor);
        invalidate();
    }

    /** 设置文字Text字体颜色 */
    public void setUnitTextColor(int textColor) {
        this.mUnitTextColor = textColor;
        mTextPaint.setColor(textColor);
        invalidate();
    }

    /** 设置动画播放加速器 */
    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    /** 计算字体高度 */
    private void measureTextHeight() {
        String str = TextUtils.isEmpty(mCurText) ? "0" : mCurText;
        mPaint.getTextBounds(str, 0, 1, mTextBounds);
        mTextHeight = mTextBounds.height();
    }

    /** 测量文字Text高度 */
    private void measureUnitTextHeight() {
        String str = isEmptyChar(mCurText) || isNumeric(mCurText) ? "万" : mCurText;
        mTextPaint.getTextBounds(str, 0, 1, mUnitTextBounds);
        mUnitTextHeight = mUnitTextBounds.height();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);

        mTextCenterX = getMeasuredWidth() / 2;
        Log.d(TAG, "onMeasure TextCenterX: " + mTextCenterX + " height:" + getMeasuredHeight() + " TextHeight:" + height + "--getMeasuredWidth()=" + getMeasuredWidth() + "--getPaddingLeft()=" + getPaddingLeft());
    }

    private int measureHeight(int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int val = MeasureSpec.getSize(measureSpec);
        int result = 0;
        switch (mode) {
            case MeasureSpec.EXACTLY:
                result = val;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                mPaint.getTextBounds("0", 0, 1, mTextBounds);
                result = mTextBounds.height();
                break;
        }
        result = mode == MeasureSpec.AT_MOST ? Math.min(result, val) : result;
        return result + getPaddingTop() + getPaddingBottom() + dp2px(13);
    }

    private int measureWidth(int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int val = MeasureSpec.getSize(measureSpec);
        int result = 0;
        switch (mode) {
            case MeasureSpec.EXACTLY:
                result = val;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                mPaint.getTextBounds("0", 0, 1, mTextBounds);
                result = mTextBounds.width();
                break;
        }
        result = mode == MeasureSpec.AT_MOST ? Math.min(result, val) : result;
        return result + getPaddingLeft() + getPaddingRight() + dp2px(8);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(TextUtils.isEmpty(mCurText) || TextUtils.isEmpty(mTargetText)) {
            return;
        }
        if (mLeftStep > 0) {
            //需要播放文字或者数字未播放完成
            postDelayed(mScrollRunnable, 0);
            if (Math.abs(mOffset) >= 1) {
                //剩余步长减少1
                mLeftStep--;
                //表示当前的数字Text已经移出NextText数字完全进入
                mOffset += mAnimDirection;
                Log.d(TAG, "onDraw CurText: " + mCurText + " NextText: " + mNextText + " leftStep: " + mLeftStep + " Offset:" + mOffset);
                //当前数字Text切换为下一个数字Text
                mCurText = mNextText;
                //计算下一轮的NextText
                calNextNumberText(mCurText);
            }
            canvas.translate(0, mOffset * getMeasuredHeight());
            drawSelf(canvas);
            drawNext(canvas);
        } else {
            //动画已经播放完成
            mOffset = 0;
            canvas.translate(0, mOffset * getMeasuredHeight());
            drawSelf(canvas);
            //父控件移除当前的数字Text
            postDelayed(mAnimEndRunnable, 0);
        }
        Log.d(TAG, "onDraw mOffset: " + mOffset * getMeasuredHeight());
    }

    /** 计算当前数字的下一个数值 */
    private int calNextNumber(int number) {
        int nextNum = 0;
        number = number == -1 ? 9 : number;
        nextNum = (number + mAnimDirection + NUMBER_CHARS_TOTAL) % NUMBER_CHARS_TOTAL;
        return nextNum;
    }

    /**
     * 数字Text滚动偏移量的线程
     *
     */
    private Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if(isFromNumeric && isToNumeric) {
                //根据数字的位置计算偏移量
                int step = (mDeltaNum - mLeftStep);
                float x = (float) (ACCELERATE_MAX_VALUE * step / mDeltaNum);
                mOffset -= mAnimDirection * mOffsetRate * (1 - mInterpolator.getInterpolation(x) + mAccelateOffset);
                Log.d(TAG, "Runnable ACCELERATE: " + x + " mOffset: " + mOffset);
            } else {
                mOffset -= mAnimDirection * 0.15f * (1 - mInterpolator.getInterpolation(0) + 0.1);
            }
            invalidate();
        }
    };

    /** 动画播放完成 */
    private Runnable mAnimEndRunnable = new Runnable() {
        @Override
        public void run() {
            if(null != callback) {
                //如果设置了ScrollNumber的回调处理函数移除当前子项
                callback.animEnd(ScrollNumber.this);
            }
        }
    };

    /** 绘制下一个数字Text */
    private void drawNext(Canvas canvas) {
        int y = (int) (getMeasuredHeight() * (0.5f + mAnimDirection));
        drawTextWithPaint(canvas, mNextText, mTextCenterX, y);
    }

    /** 绘制当前的数字Text */
    private void drawSelf(Canvas canvas) {
        int y = getMeasuredHeight() / 2;
        drawTextWithPaint(canvas, mCurText, mTextCenterX, y);
    }

    private void drawTextWithPaint(Canvas canvas, String text, int centerX, int centerY) {
        Paint paint = getTextPaint(text);
        if(TextUtils.isEmpty(text) || isNumeric(text)) {
            String numberStr = text;
            if(TextUtils.isEmpty(numberStr)) {
                //文字Text采用空白字符填充
                numberStr = " ";
            }
            canvas.drawText(numberStr, centerX, centerY + mTextHeight / 2,
                    paint);
        } else {
            paint.getTextBounds(text, 0, 1, mUnitTextBounds);
            mUnitTextHeight = mUnitTextBounds.height();
            canvas.drawText(text, centerX, centerY + mUnitTextHeight / 2 - 2,
                    paint);
        }
    }

    /** 获取数字Text画笔 */
    private Paint getTextPaint(String text) {
        if(isNumeric(text)) {
            return mPaint;
        }
        return mTextPaint;
    }

    private int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, getResources().getDisplayMetrics());
    }

    private int sp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                dpVal, getResources().getDisplayMetrics());
    }

    /**
     * 判断是否是数字
     *
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        //-?[0-9]+.?[0-9]+  //正负整数
        Pattern pattern = Pattern.compile("[0-9]*"); //正整数
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    /** 获取数字的值 */
    public int getNumberValue(String str) {
        int number = 0;
        if(isNumeric(str)) {
            try {
                number = Integer.parseInt(str);
            }catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            //文字默认为-1
            number = -1;
        }
        return number;
    }

    /** 是否是空字符串 */
    public static boolean isEmptyChar(String value) {
        boolean isEmpty = false;
        if(TextUtils.isEmpty(value) || value.equals(" ")) {
            isEmpty = true;
        }
        return isEmpty;
    }

    /**
     * 获取字符串值（空值转化为""）
     */
    public String GetStringNoEmpty(String value) {
        StringBuilder builder = new StringBuilder();
        if(!TextUtils.isEmpty(value)) {
            builder.append(value);
        } else {
            builder.append(" ");
        }
        return builder.toString();
    }

    /** 设置ScrollNumber的回调处理函数 */
    public void setScrollNumberCallback(MultiScrollNumber.IScrollNumberCallback callback) {
        this.callback = callback;
    }

    /** 是否需要移除当前项(目标对应项为空数字) */
    public boolean isNeedRemove() {
        return isNeedRemove;
    }
}
