package top.wuhaojie.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wuhaojie on 2016/7/19 20:39.
 * 1.1 周荣华 2017/2/9 增加字符串分组初始化处理
 */
public class MultiScrollNumber extends LinearLayout {
    private static final String TAG = ScrollNumber.class.getSimpleName();

    /** 动画播放模式 */
    public enum Mode {
        /**
         * 低位先启动低位后到达
         */
        START_FIRST_ARRIVAL_LAST,
        /**
         * 同时启动同时到达
         */
        START_ARRIVAL_SAME_TIME,
        /**
         * 低位先启动低位先到达
         */
        START_FIRST_ARRIVAL_FIRST,
        /** 台历模式(有变化的数字往前翻) */
        CALENDAR,
        /** 记分牌模式(有变化的数字根据距离决定往前还是往后翻) */
        SCOREBOARD
    }


    /**
     * 动画播放回调函数
     */
    public interface IScrollNumberCallback {
        public void animEnd(ScrollNumber scrollNumber);
    }

    /** 默认数字Text */
    private static final String DEFAULT_NUMBER_CHAR = "0";
    /** 默认非数字Text */
    private static final String DEFAULT_TEXT_CHAR = " ";
    /** 默认空数字Text */
    private static final String DEFAULT_EMPTY_CHAR = " ";
    /** 默认数字播放延时处理 */
    private static final int NUMBER_ANIM_DELAY = 90;
    /** 默认数字初始化延时处理 */
    private static final int NUMBER_INIT_DELAY = 10;
    /** 默认数字播放延时处理 */
    private static final long NUMBER_ANIM_DURATION = 1500L;

    /** 默认数字字体大小 */
    public static final int SCROLL_NUMBER_TEXT_SIZE = 25;
    /** 默认文字字体大小 */
    public static final int SCROLL_UNIT_TEXT_SIZE = 18;
    private Context mContext;
    /** 目标数字Text(逆序) */
    private List<String> mTargetNumbers = new ArrayList<String>();
    /** 初始数字Text(逆序) */
    private List<String> mPrimaryNumbers = new ArrayList<String>();
    /** 滚动的数字项 */
    private List<ScrollNumber> mScrollNumbers = new ArrayList<ScrollNumber>();
    /** 默认数字字体大小 */
    private int mTextSize = SCROLL_NUMBER_TEXT_SIZE;
    /** 默认文字字体大小 */
    private int mUnitTextSize = SCROLL_UNIT_TEXT_SIZE;
    /** 数字Text字体颜色 */
    private int[] mTextColors = new int[]{R.color.purple01};
    /** 数字Text背景资源 */
    private int numberResId;
    /** 文字Text背景资源 */
    private int numberUnitResId;
    /** 需要滚动的最高位 */
    private int mAnimStartPosition;
    /** 基准动画播放轮数(默认一轮) */
    private int mAnimLoop = ScrollNumber.SCROLL_LOOP_DEFAULT;
    /** 起始目标数字字符串 A */
    private String numTextPrimary = "";
    /** 起始目标数字字符串对应的中间字符 A' */
    private String numTextMiddle = "";
    /** 目标数字字符串 B */
    private String numTextTarget = "";
    /** 格式化起始目标数字字符串 A */
    private String fmtNumTextPrimary = "";
    /** 格式化起始目标数字字符串对应的中间字符 A' */
    private String fmtNumTextMiddle = "";
    /** 格式化目标数字字符串 B */
    private String fmtNumTextTarget = "";
    /** 数字Text字体 */
    private String mFontFileName;
    /** 文字Text字体 */
    private String mUnitFontFileName;
    /** 数字动画播放模式 */
    private Mode mAnimMode = Mode.START_FIRST_ARRIVAL_LAST;

    /** 数字播放加速器 */
    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    /** 数字滚动项移除子项 */
    private IScrollNumberCallback callback = new IScrollNumberCallback() {
        @Override
        public void animEnd(ScrollNumber scrollNumber) {
            if(null != scrollNumber && scrollNumber.isNeedRemove()) {
                scrollNumber.setScrollNumberCallback(null);
                //如果当前ScrollNumber需要移除的话从父控件移除
                removeScrollNumber(scrollNumber);
            }
        }
    };

    public MultiScrollNumber(Context context) {
        this(context, null);
    }

    public MultiScrollNumber(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public MultiScrollNumber(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;

        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.MultiScrollNumber);
        String primaryNumber = typedArray.getString(R.styleable.MultiScrollNumber_primary_number);
        String targetNumber = typedArray.getString(R.styleable.MultiScrollNumber_target_number);
        int numberSize = typedArray.getInteger(R.styleable.MultiScrollNumber_number_size, SCROLL_NUMBER_TEXT_SIZE);
        int numberUnitSize = typedArray.getInteger(R.styleable.MultiScrollNumber_number_unit_size, SCROLL_UNIT_TEXT_SIZE);
        numberResId = typedArray.getInteger(R.styleable.MultiScrollNumber_numberBackground, 0);
        numberUnitResId = typedArray.getInteger(R.styleable.MultiScrollNumber_numberUnitBackground, 0);

        //初始化数字
        setNumber(primaryNumber, targetNumber);
        //设置数字字体大小
        setTextSize(numberSize);
        //设置文字字体大小
        setUnitTextSize(numberUnitSize);

        typedArray.recycle();

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
    }

    /**
     * 通过字符串获取数字text列表
     * 说明: 高位放高位数字
     */
    private List<String> getNumberStringList(String primaryNumber) {
        List<String> numbers = null;
        if(!TextUtils.isEmpty(primaryNumber)) {
            numbers = new ArrayList<String>();
            for(int i = 0; i < primaryNumber.length(); i++) {
                numbers.add(primaryNumber.substring(i, i + 1));
            }
            Collections.reverse(numbers);
        }
        return numbers;
    }

    /** 设置滚动模式 */
    public void setScollAnimationMode(Mode mode) {
        this.mAnimMode = mode;
    }

    /**
     * 初始化数字Text
     * 说明: 只初始化数据不播放动画
     */
    public void setNumber(String str) {
        if(TextUtils.isEmpty(str)) {
            return;
        }
        setNumber(numTextTarget, str);
    }

    /**
     * 初始化数字Text
     * 说明: 只初始化数据A-->B 初始化从A-->A'，此过程不播放动画。播放动画通过调用play方法实现。
     *
     * @param from : 起始数字字符串 A
     * @param to : 目标数字字符串 B
     *
     */
    public void setNumber(String from, String to) {
        if(TextUtils.isEmpty(from) && TextUtils.isEmpty(to)) {
            return;
        }
        //保存起始数字和目标数字的值
        numTextPrimary = from;
        numTextTarget = to;
        //生产中间过度数据数据A'
        //格式化数字串
        StringBuilder fromBuffer = new StringBuilder();
        StringBuilder toBuffer = new StringBuilder();
        formatNumberGroupByChar(from, to, fromBuffer, toBuffer);
        Log.d(TAG, "setNumber " + fromBuffer.toString() + "fromBuffer [" + fromBuffer.toString() + "] length: " + fromBuffer.length() + " toBuffer [" + toBuffer.toString() + "] length: " + fromBuffer.length());
        //格式化打洞(去掉目标数字需要移除的Text项目)
        formatPrimaryByTarget(fromBuffer, toBuffer);
        fmtNumTextMiddle = fromBuffer.toString();
        fmtNumTextTarget = toBuffer.toString();
        fmtNumTextPrimary = fmtNumTextMiddle;
        numTextTarget = fmtNumTextTarget;
        mAnimStartPosition = initAnimStartPosition(fromBuffer, toBuffer);
        //数字Text列表初始化为中间数字A'
        setNumber(getNumberStringList(fmtNumTextTarget), getNumberStringList(fmtNumTextTarget), NUMBER_INIT_DELAY, false);
        Log.d(TAG, "setNumber fmtNumTextMiddle [" + fmtNumTextMiddle + "] fmtNumTextPrimary [" + fmtNumTextTarget + "]");
    }

    /** 格式化打洞 */
    private void formatPrimaryByTarget(StringBuilder fromBuffer, StringBuilder toBuffer) {
        String subStr = "";
        for(int i = toBuffer.length(); i > 0; i--) {
            subStr = toBuffer.substring(i - 1, i);
            if(ScrollNumber.isEmptyChar(subStr)) {
                //如果目标数字为空移除起始数字对应位置
                fromBuffer.delete(i - 1, i);
                toBuffer.delete(i - 1, i);
            } else if(ScrollNumber.isNumeric(subStr)) {
                //如果目标数字不为空且为数字起始数字对应位置初始化为0
                if(ScrollNumber.isEmptyChar(fromBuffer.substring(i - 1, i))) {
                    fromBuffer.replace(i - 1, i, DEFAULT_NUMBER_CHAR);
                }
            } else {
                //如果目标数字不为空且为是文字Text起始数字对应位置不需要处理
                if(ScrollNumber.isEmptyChar(fromBuffer.substring(i - 1, i))) {
                    fromBuffer.replace(i - 1, i, subStr);
                }
            }
        }
    }

    /**
     * 设置播放的数字字符串
     * 说明: 起始字符串默认为上一个目标字符串。初始化+动画播放 A-->B
     * 如果数字没有变化不播放动画
     *
     * @param str : 目标数字字符串
     */
    public void setNumberWithAnimationNumberChanged(String str) {
        if (TextUtils.isEmpty(str) || str.equals(numTextTarget)) {
            //如果为空或者与目标数字内容相同不进行处理
            return;
        }
        setNumberWithAnimation(str);
    }

    /**
     * 设置播放的数字字符串
     * 说明: 起始字符串默认为上一个目标字符串。初始化+动画播放 A-->B
     *
     * @param str : 目标数字字符串
     */
    public void setNumberWithAnimation(String str) {
        if (TextUtils.isEmpty(str)) {
            //如果为空或者与目标数字内容相同不进行处理
            return;
        }
        Log.d(TAG, "----setNumberWithAnimation from: " + numTextTarget + " to: " + str);
        setNumberWithAnimation(numTextTarget, str);
    }

    /**
     * 设置播放的数字字符串
     * 说明: 初始化+动画播放
     *
     * @param from : 起始数字字符串
     * @param to : 目标数字字符串
     */
    public void setNumberWithAnimation(String from, String to) {
        if(TextUtils.isEmpty(from) && TextUtils.isEmpty(to)) {
            return;
        }
        //保存起始数字和目标数字的值
        numTextPrimary = from;
        numTextTarget = to;
        //中间数字即目标数字
        numTextMiddle = to;
        //格式化数字串
        StringBuilder fromBuffer = new StringBuilder();
        StringBuilder toBuffer = new StringBuilder();
        formatNumberGroupByChar(from, to, fromBuffer, toBuffer);
        //格式化打洞
        formatPrimaryByTarget(fromBuffer, toBuffer);
        fmtNumTextPrimary = fromBuffer.toString();
        fmtNumTextTarget = toBuffer.toString();
        fmtNumTextMiddle = fmtNumTextTarget;
        mAnimStartPosition = initAnimStartPosition(fromBuffer, toBuffer);
        Log.d(TAG, "setNumberWithAnimation format from: " + fmtNumTextPrimary + " to: " + fmtNumTextTarget);
        //数字Text列表初始化
        setNumber(getNumberStringList(fmtNumTextPrimary), getNumberStringList(fmtNumTextTarget), NUMBER_ANIM_DELAY, true);
    }

    /**
     * 播放动画最高位的位置
     * 说明: 查找动画需要播放的最高位。其下低位都需要播放动画
     */
    private int initAnimStartPosition(StringBuilder fmtNumTextPrimary, StringBuilder fmtNumTextTarget) {
        int animStartPos = fmtNumTextPrimary.length();
        if(Mode.SCOREBOARD == mAnimMode || Mode.CALENDAR == mAnimMode) {
            //记分牌和日历模式不播放高位未变化Text
            for(int i = 0; i < fmtNumTextPrimary.length(); i++) {
                if(!fmtNumTextPrimary.substring(i, i+1).equals(fmtNumTextTarget.substring(i, i+1))) {
                    animStartPos = fmtNumTextPrimary.length() - i - 1;
                    break;
                }
            }
        }
        return animStartPos;
    }

    /**
     * 播放动画
     * 说明: 从中间状态播放到目标状态 动画播放 A'-->B
     */
    public void play() {
        //数字Text列表初始化为中间数字A'
        setNumber(getNumberStringList(fmtNumTextMiddle), getNumberStringList(fmtNumTextTarget), NUMBER_ANIM_DELAY, true);
    }

    /**
     * 数字Text格式化
     * 说明: 这是数字格式化的核心算法。
     * 1)自然分组格式化补位：按照单位格式化，“兆亿万”格式化初始串和结束串，
     * 如果对应的单位分组没有数字，此分组的单位也为空。
     * 例如初始数字10万，结束数字1亿300，
     * 格式化结果为：初始数字：空空10万空空空 结束数字：1亿空空空300
     */
    private void formatNumber(String from, String to, StringBuilder fromBuffer, StringBuilder toBuffer) {
        if(TextUtils.isEmpty(from) && TextUtils.isEmpty(to)) {
            return;
        }
        if(!TextUtils.isEmpty(from) && !TextUtils.isEmpty(to)) {
            //两个字符串都不为空需要格式化
            initNumberWithUnit(from, to, fromBuffer, toBuffer);
        } else {
            //有一个字符串为空
            initEmptyNumber(from, to, fromBuffer, toBuffer);
        }
    }

    /**
     * 根据数字单位初始化NumberText
     *
     */
    private void initNumberWithUnit(String from, String to, StringBuilder fromBuffer, StringBuilder toBuffer) {
        String units[] = new String[]{"兆", "亿", "万"};
        int indexFrom = 0;
        int indexTo = 0;
        int nextFrom = 0;
        int nextTo = 0;
        int lenFrom = 0;
        int lenTo = 0;
        int groupLen = 0;
        if(!TextUtils.isEmpty(from) && !TextUtils.isEmpty(to)) {
            for(int i = 0; i < units.length; i++) {
                indexFrom = from.indexOf(units[i], nextFrom);
                indexTo = to.indexOf(units[i], nextTo);
                if(indexFrom < 0 && indexTo < 0) {
                    //双方都不存在此单位的数字
                    continue;
                }

                //起始数字格式化
                lenFrom = indexFrom > 0 ? indexFrom - nextFrom : 0;
                lenTo = indexTo > 0 ? indexTo - nextTo : 0;
                groupLen = Math.max(lenFrom, lenTo);
                initNumberGroup(from, to, fromBuffer, toBuffer, nextFrom, nextTo, indexFrom, indexTo);
                if(indexFrom < 0) {
                    for(int j = 0; j < groupLen; j++) {
                        //补空字符占位
                        fromBuffer.append(DEFAULT_NUMBER_CHAR);
                    }
                    fromBuffer.append(units[i]);
                } else {
                    for(int j = 0; j < groupLen - lenFrom; j++) {
                        //补空字符占位
                        fromBuffer.append(DEFAULT_NUMBER_CHAR);
                    }
                    fromBuffer.append(from.substring(nextFrom, indexFrom));
                    fromBuffer.append(units[i]);
                    //移动到下一个位置
                    nextFrom = indexFrom + units[i].length();
                }
                //目标数字格式化
                if(indexTo < 0) {
                    //如果目标数字对应的Text不存在数字和单位全部移除
                    for(int j = 0; j < groupLen; j++) {
                        //补空字符占位
                        toBuffer.append(" ");
                    }
                    //当前这一分组部分不存在数字需要移除
                    toBuffer.append(" ");
                } else {
                    for(int j = 0; j < groupLen - lenTo; j++) {
                        //补空字符占位
                        toBuffer.append(" ");
                    }
                    toBuffer.append(to.substring(nextTo, indexTo));
                    toBuffer.append(units[i]);
                    //移动到下一个位置
                    nextTo = indexTo + units[i].length();
                }
            }//for-unit
            //追加万以内的数字
            initNumberGroup(from, to, fromBuffer, toBuffer, nextFrom, nextTo);
        }
    }

    /**
     * 空Number初始化
     * 说明: 高位补空
     */
    private void initEmptyNumber(String from, String to,
                                 StringBuilder fromBuffer, StringBuilder toBuffer) {
        int lenFrom = 0;
        int lenTo = 0;
        //有一个字符串为空
        if (TextUtils.isEmpty(from) && !TextUtils.isEmpty(to)) {
            //起始字串为空
            lenTo = to.length();
            for (int i = 0; i < lenTo; i++) {
                if(ScrollNumber.isNumeric(to.substring(i, i + 1))) {
                    fromBuffer.append(DEFAULT_NUMBER_CHAR);
                } else {
                    fromBuffer.append(DEFAULT_TEXT_CHAR);
                }
            }
            toBuffer.append(to);
        } else if (!TextUtils.isEmpty(from) && TextUtils.isEmpty(to)) {
            //结束字串为空
            lenFrom = from.length();
            for (int i = 0; i < lenFrom; i++) {
                toBuffer.append(DEFAULT_EMPTY_CHAR);
            }
            fromBuffer.append(from);
        }
    }

    /**
     * 数字Text格式化
     * 说明: 这是数字格式化的核心算法。
     * 2)文字分组格式化补位：按照单位格式化，按照文字进行分组，从高位开始分组比较
     * 例如初始数字10万，结束数字1亿300，
     * 格式化结果为：初始数字：10万空空空 结束数字：空1亿300
     */
    private void formatNumberGroupByChar(String from, String to,
                                         StringBuilder fromBuffer, StringBuilder toBuffer) {
        if (TextUtils.isEmpty(from) && TextUtils.isEmpty(to)) {
            return;
        }
        if(!TextUtils.isEmpty(from) && !TextUtils.isEmpty(to)) {
            //其实数字和目标数字都不为空根据非数字字符分组
            initNumberWithChar(from, to, fromBuffer, toBuffer);
        } else {
            //有一个字符串为空
            initEmptyNumber(from, to, fromBuffer, toBuffer);
        }
    }

    /**
     * 根据非数字字符进行分组
     */
    private void initNumberWithChar(String from, String to,
                                    StringBuilder fromBuffer, StringBuilder toBuffer) {
        int indexFrom = 0;
        int indexTo = 0;
        int nextFrom = 0;
        int nextTo = 0;
        int lenWordFrom = 0;
        int lenWordTo = 0;
        int groupFromCnt = 0;
        int groupToCnt = 0;
        if(!TextUtils.isEmpty(from) && !TextUtils.isEmpty(to)) {
            //确定分组数目
            groupFromCnt = calculateGroupCount(from);
            groupToCnt = calculateGroupCount(to);
            int groupCnt = Math.min(groupFromCnt, groupToCnt);
            for(int i = 0; i < groupCnt; i++) {
                //查找当前分组非数字Text位置
                indexFrom = findWordCharFromString(from, nextFrom);
                indexTo = findWordCharFromString(to, nextTo);
                if(indexFrom < 0 && indexTo < 0) {
                    //1)双方都不存在非数字的Text
                    initNumberGroup(from, to, fromBuffer, toBuffer, nextFrom, nextTo);
                    break;
                } else {
                    //2)至少一方存在非数字
                    //初始化当前分组的数字Text
                    if(indexFrom < 0) {
                        //不存在对应非数字的Text默认数字Text一直到字符串结尾
                        indexFrom = from.length();
                    }
                    if(indexTo < 0) {
                        indexTo = to.length();
                    }
                    initNumberGroup(from, to, fromBuffer, toBuffer, nextFrom, nextTo, indexFrom, indexTo);
                    //初始化当前分组的文字Text
                    lenWordFrom = calculateWordCharLength(from, indexFrom);
                    lenWordTo = calculateWordCharLength(to, indexTo);
                    initGroupWordChar(from, to, fromBuffer, toBuffer, indexFrom, indexTo, lenWordFrom, lenWordTo);
                    //计算下一个分组的偏移位置
                    nextFrom = indexFrom + lenWordFrom;
                    nextTo = indexTo + lenWordTo;
                }
            }//for-groupCnt
            //追加剩余分组的内容部分
            initGroupWithEmptyNumber(from, to, fromBuffer, toBuffer, nextFrom, nextTo, from.length() - nextFrom, to.length() - nextTo);
        }
    }

    /** 计算文字Text长度 */
    private int calculateWordCharLength(String from, int indexFrom) {
        int lenWordFrom = 0;
        int nextWordCharFrom = 0;
        if(!TextUtils.isEmpty(from) && indexFrom < from.length()) {
            if(indexFrom > 0) {
                nextWordCharFrom = findNumericCharFromString(from, indexFrom);
                lenWordFrom = nextWordCharFrom < indexFrom ? from.length() - indexFrom : nextWordCharFrom - indexFrom;
            }
        }
        return lenWordFrom;
    }

    /**
     * 初始化对应的空白分组
     *
     * @param from : 起始数字字符串
     * @param to : 目标数字字符串
     * @param fromBuffer : 缓存起始数字分组格式化Buffer
     * @param toBuffer : 缓存目标数字分组格式化Buffer
     * @param indexFrom : 起始数字字符串开始计算的index
     * @param indexTo : 目标数字字符串开始计算的index
     * @param lenFrom : 起始数字当前分组的需要格式化部分长度
     * @param lenTo : 目标数字当前分组的需要格式化部分长度
     */
    private void initGroupWithEmptyNumber(String from, String to, StringBuilder fromBuffer,
                                          StringBuilder toBuffer, int indexFrom, int indexTo,
                                          int lenFrom, int lenTo) {
        if(lenFrom <= 0 && lenTo <= 0) {
            return;
        }
        String subFrom = "";
        String subTo = "";
        if(!TextUtils.isEmpty(from) && indexFrom >= 0 && indexFrom + lenFrom <= from.length()) {
            subFrom = from.substring(indexFrom, indexFrom + lenFrom);
        }
        if(!TextUtils.isEmpty(to) && indexTo >= 0 && indexTo + lenTo <= to.length()) {
            subTo = to.substring(indexTo, indexTo + lenTo);
        }
        initEmptyNumber(subFrom, subTo, fromBuffer, toBuffer);
    }

    /**
     * 初始化对应的分组非数字字符Text
     * 说明: 高位补空
     *
     * @param from : 起始数字字符串
     * @param to : 目标数字字符串
     * @param fromBuffer : 缓存起始数字分组格式化Buffer
     * @param toBuffer : 缓存目标数字分组格式化Buffer
     * @param indexFrom : 起始数字字符串文字Text开始计算的index
     * @param indexTo : 目标数字字符串文字Text开始计算的index
     * @param lenWordFrom : 起始数字当前分组非数字Text的需要格式化部分长度
     * @param lenWordTo : 目标数字当前分组非数字Text的需要格式化部分长度
     *
     */
    private void initGroupWordChar(String from, String to, StringBuilder fromBuffer,
                                   StringBuilder toBuffer, int indexFrom, int indexTo,
                                   int lenWordFrom, int lenWordTo) {
        if(TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
            return;
        }
        if((indexFrom < 0 && indexTo < 0)) {
            return;
        }
        if((indexFrom > 0 && indexFrom + lenWordFrom > from.length())
                || (indexTo > 0 && indexTo + lenWordTo > to.length())) {
            return;
        }
        //当前分组文字Text长度
        int groupWordLen = Math.max(lenWordFrom, lenWordTo);
        if(groupWordLen <= 0) {
            return;
        }
        for(int i = 0; i < groupWordLen - lenWordFrom; i++) {
            fromBuffer.append(DEFAULT_TEXT_CHAR);
        }
        if(indexFrom >= 0 && lenWordFrom > 0) {
            fromBuffer.append(from.substring(indexFrom, indexFrom + lenWordFrom));
        }
        for(int i = 0; i < groupWordLen - lenWordTo; i++) {
            toBuffer.append(DEFAULT_EMPTY_CHAR);
        }
        if(indexTo >= 0 && lenWordTo > 0) {
            toBuffer.append(to.substring(indexTo, indexTo + lenWordTo));
        }
    }

    /** 初始化全数字分组 */
    private void initNumberGroup(String from, String to, StringBuilder fromBuffer,
                                 StringBuilder toBuffer, int nextFrom, int nextTo) {
        int lenFrom = TextUtils.isEmpty(from) ? 0 : from.length();
        int lenTo = TextUtils.isEmpty(to) ? 0 : to.length();
        initNumberGroup(from, to, fromBuffer, toBuffer, nextFrom, nextTo, lenFrom, lenTo);
    }

    /**
     * 初始化全数字分组
     * 说明: 数字前补位
     */
    private void initNumberGroup(String from, String to,
                                 StringBuilder fromBuffer, StringBuilder toBuffer,
                                 int nextFrom, int nextTo, int indexFrom, int indexTo) {
        if(TextUtils.isEmpty(from) && TextUtils.isEmpty(to)) {
            return;
        }
        if(nextFrom < 0 || nextTo < 0) {
            return;
        }
        int lenFrom = 0;
        int lenTo = 0;
        //当前分组长度
        int groupLen = 0;
        //追加万以内的数字
        lenFrom = nextFrom >= 0 ? indexFrom - nextFrom : indexTo;
        lenTo = nextTo >= 0 ? indexTo - nextTo : indexTo;
        groupLen = Math.max(lenFrom, lenTo);
        if(nextFrom < 0) {
            nextFrom = 0;
        }
        if(nextTo < 0) {
            nextTo = 0;
        }
        //起始数字
        for(int j = 0; j < groupLen - lenFrom; j++) {
            //补空字符占位
            fromBuffer.append(DEFAULT_NUMBER_CHAR);
        }
        if(nextFrom + lenFrom <= from.length()) {
            //追加当前分组起始数字Text 容错：字符位置有效时追加数字
            fromBuffer.append(from.substring(nextFrom, nextFrom + lenFrom));
        }
        //目标数字
        for(int j = 0; j < groupLen - lenTo; j++) {
            //补空字符占位(当前为不存在需要移除)
            toBuffer.append(DEFAULT_EMPTY_CHAR);
        }
        if(nextTo + lenTo <= to.length()) {
            //追加当前分组目标数字Text 容错：字符位置有效时追加数字
            toBuffer.append(to.substring(nextTo, nextTo + lenTo));
        }
    }

    /**
     * 计算数字分组个数
     * 说明: 优先计算数字和文字混排的，然后全数字或者全文字字符串当做一个分组
     *
     * @param from : 需要处理的字符串
     */
    private int calculateGroupCount(String from) {
        int groupCnt = 0;
        if (!TextUtils.isEmpty(from)) {
            if (1 == from.length()) {
                return 1;
            }
            String curText = "";
            String nextText = "";
            curText = from.substring(0, 1);
            for (int i = 0; i < from.length(); i++) {
                nextText = from.substring(i + 1, i + 2);
                if (ScrollNumber.isNumeric(curText) ^ ScrollNumber.isNumeric(nextText)) {
                    //当前项与下一个项不都是数字或者不都是文字
                    groupCnt++;
                }
                curText = nextText;
                if (i + 2 >= from.length()) {
                    break;
                }
            }
            //特殊情况只有文字或者只有数字
            if (0 == groupCnt) {
                groupCnt = 1;
            }
        }
        return groupCnt;
    }

    /** 查找下一个数字字符位置 */
    private int findNumericCharFromString(String from, int start) {
        int index = -1;
        if(!TextUtils.isEmpty(from) && start >= 0 && start < from.length()) {
            String subStr = "";
            for(int i = start; i < from.length(); i++) {
                subStr = from.substring(i, i+1);
                if(ScrollNumber.isNumeric(subStr)) {
                    //查找到一个非数字字符
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    /** 查找字符串中文字（非数字）Text */
    private int findWordCharFromString(String from, int start) {
        int index = -1;
        if(!TextUtils.isEmpty(from) && start >= 0 && start < from.length()) {
            String subStr = "";
            for(int i = start; i < from.length(); i++) {
                subStr = from.substring(i, i+1);
                if(!ScrollNumber.isEmptyChar(subStr) && !ScrollNumber.isNumeric(subStr)) {
                    //查找到一个非数字字符
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    /** 重置控件视图 */
    private void resetView() {
        mTargetNumbers.clear();
        mScrollNumbers.clear();
        removeAllViews();
    }

    private void removeScrollNumber(ScrollNumber scrollNumber) {
        if(null != scrollNumber) {
            if(null != mScrollNumbers) {
                mScrollNumbers.remove(scrollNumber);
            }
            removeView(scrollNumber);
        }
    }

    /**
     * 初始化数字字符列表
     *
     * @param val : 目标数字字符列表(逆序)
     */
    private void setNumber(List<String> val) {
        setNumber(null, val, NUMBER_INIT_DELAY, false);
    }

    /**
     * 设置数字的值
     * 说明: 初始化顺序从低位开始滚动，越高位延时越多。
     *
     * @param from : 起始数字字符列表(逆序)
     * @param to : 目标数字字符列表(逆序)
     * @param delay : 依次延时
     */
    private void setNumber(List<String> from, List<String> to, int delay, boolean isAnimation) {
        if(null == from && null == to) {
            return;
        }
        if(delay <= 0) {
            delay = NUMBER_INIT_DELAY;
        }
        //数字Text列表初始化
        int lenFrom = null != from ? from.size() : 0;
        int lenTo = null != to ? to.size() : 0;
        int lenMax = Math.max(lenFrom, lenTo);
        if(lenFrom != lenTo) {
            //起始数字个数和目标数字个数不一致的情况下
            mPrimaryNumbers.clear();
            mTargetNumbers.clear();
            //列表长度不一致的时候(先初始化低位后初始化高位)
            //初始化起始数字Text列表
            if(lenFrom > 0) {
                mPrimaryNumbers.addAll(from);
            }
            for(int i = lenFrom; i < lenMax; i++) {
                //通过空白字符补位
                mPrimaryNumbers.add(" ");
            }
            //初始化目标数字Text列表
            if(lenTo > 0) {
                mTargetNumbers.addAll(to);
            }
            for(int i = lenTo; i < lenMax; i++) {
                mTargetNumbers.add(" ");
            }
        } else {
            //两个列表长度一致
            if(lenFrom > 0) {
                mPrimaryNumbers.clear();
                mTargetNumbers.clear();
                mPrimaryNumbers.addAll(from);
                mTargetNumbers.addAll(to);
            }
        }

        int lastCount = mScrollNumbers.size();
        ScrollNumber scrollNumber = null;
        if(lenMax > lastCount) {
            //需要添加滚动数字项(从高位开始添加)
            for (int i = lastCount; i < lenMax; i++) {
                scrollNumber = new ScrollNumber(mContext);
                LinearLayout.LayoutParams params = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.leftMargin = 5;
                params.rightMargin = 5;
                scrollNumber.setLayoutParams(params);
                scrollNumber.setScollAnimationMode(mAnimMode);
                scrollNumber.setTextSize(mTextSize);
                scrollNumber.setUnitTextSize(mUnitTextSize);
                if (!TextUtils.isEmpty(mFontFileName))
                    scrollNumber.setTextFont(mFontFileName);
                if (!TextUtils.isEmpty(mUnitFontFileName))
                    scrollNumber.setUnitTextFont(mUnitFontFileName);
                if(mTextColors != null && mTextColors.length > 0) {
                    if(mTextColors.length > 1) {
                        scrollNumber.setUnitTextColor(getResources().getColor(mTextColors[1]));
                    } else {
                        scrollNumber.setUnitTextColor(getResources().getColor(mTextColors[0]));
                    }
                    scrollNumber.setTextColor(getResources().getColor(mTextColors[0]));
                }
                mScrollNumbers.add(scrollNumber);
                addView(scrollNumber, 0);
            }
        } else if(lenMax < lastCount) {
            //需要移除滚动数字项(从高位开始移除)
            if(0 == lenMax) {
                removeAllViews();
            } else {
                for (int i = lastCount; i > lenMax; i--) {
                    scrollNumber = mScrollNumbers.get(i-1);
                    removeScrollNumber(scrollNumber);
                    Log.d(TAG, "lenMax: " + lenMax + " ScrollNumbers: " + mScrollNumbers.size() + " childCount: " + getChildCount());
                }
            }
        }

        int animStep = 0;
        long animDelay = 0L;
        for (int i = 0; i < lenMax; i++) {
            //从低位开始初始化
            //滚动轮数(默认播放一轮遇到相同数字增加一轮)
            animStep = calAnimStepMax(isAnimation, i, mTargetNumbers.get(i).equals(mPrimaryNumbers.get(i)));
            //动画启动延时
            animDelay = calAnimDelay(isAnimation, i, lenMax, delay);
            scrollNumber = mScrollNumbers.get(i);
            scrollNumber.setNumberChar(mPrimaryNumbers.get(i), mTargetNumbers.get(i), animDelay, mAnimLoop, animStep, NUMBER_ANIM_DURATION);
            scrollNumber.setScrollNumberCallback(callback);
            if (ScrollNumber.isNumeric(mTargetNumbers.get(i))) {
                scrollNumber.setBackgroundResource(numberResId);
            } else if (!ScrollNumber.isEmptyChar(mTargetNumbers.get(i))) {
                scrollNumber.setBackgroundResource(numberUnitResId);
            } else {
                scrollNumber.setBackgroundResource(0);
            }
        }
        if(null != from) {
            from.clear();
        }
        if(null != to) {
            to.clear();
        }
    }

    /** 计算当前模式的数字播放策略 */
    private ScrollNumber.Strategy calStrategy() {
        ScrollNumber.Strategy strategy = ScrollNumber.Strategy.NATURAL;
        if(Mode.SCOREBOARD == mAnimMode) {
            //如果记分牌模式动画播放采用最短距离策略
            strategy = ScrollNumber.Strategy.BIGGER_OR_SMALLER;
        }
        return strategy;
    }

    /**
     * 计算动画播放延时
     *
     * @param isAnimation : 是否播放动画(初始化不需要播放动画)
     * @param index : 当前初始化的索引位置(从低位开始计数)
     * @param lenMax : 数字位数总长度
     * @param delayDuration : 每一个延时的长短
     */
    private long calAnimDelay(boolean isAnimation, int index, int lenMax, long delayDuration) {
        long delay = 0L;
        if (!isAnimation || Mode.START_ARRIVAL_SAME_TIME == mAnimMode || index > mAnimStartPosition) {
            //高位没有变化不需要播放动画
            return 0;
        }
        if (Mode.START_FIRST_ARRIVAL_LAST == mAnimMode) {
            //低位先启动低位后到达(逐个启动播放个数不同，低位个数多)
            delay = index * delayDuration;
        } else if (Mode.START_FIRST_ARRIVAL_FIRST == mAnimMode) {
            //低位先启动低位先到达(逐个启动播放个数相同)
            delay = index * delayDuration;
        } else if (Mode.CALENDAR == mAnimMode || Mode.SCOREBOARD == mAnimMode) {
            //台历模式和记分牌模式都从地位开始(逐个启动播放)
            delay = index * delayDuration;
        } else {
            delay = index * delayDuration;
        }
        return delay;
    }

    /** 计算动画播放最大步长数 */
    private int calAnimStepMax(boolean isAnimation, int index, boolean isEqualsNumber) {
        int animStep = 0;
        if(!isAnimation || index > mAnimStartPosition) {
            //高位没有变化不需要播放动画
            return 0;
        }

        //以10个数字为基准
        int animStepBase = ScrollNumber.NUMBER_CHARS_TOTAL;
        if(Mode.START_FIRST_ARRIVAL_LAST == mAnimMode) {
            //低位先启动低位后到达(逐个启动播放个数不同，低位个数多)
            animStep = animStepBase + mAnimStartPosition - index;
        } else if(Mode.START_FIRST_ARRIVAL_FIRST == mAnimMode) {
            //低位先启动低位先到达(逐个启动播放个数相同)
            animStep = animStepBase;
        } else if(Mode.START_ARRIVAL_SAME_TIME == mAnimMode) {
            //同时启动同时到达
            animStep = animStepBase;
        } else if (Mode.CALENDAR == mAnimMode || Mode.SCOREBOARD == mAnimMode) {
            //台历模式和记分牌模式都从地位开始(逐个启动播放不增加偏移量)
            animStep = 0;
        } else {
            animStep = 0;
        }
        return animStep;
    }

    /**
     * 设置字体颜色
     * @param textColors
     * <ul>         推荐：第一个参数为数字颜色，第二个参数为文字颜色
     */
    public void setTextColors(@ColorRes int... textColors) {
        if (textColors == null || textColors.length == 0)
            throw new IllegalArgumentException("color array couldn't be empty!");
        mTextColors = textColors;
        for (int i = mScrollNumbers.size() - 1; i >= 0; i--) {
            ScrollNumber scrollNumber = mScrollNumbers.get(i);
            if(mTextColors.length > 1) {
                scrollNumber.setUnitTextColor(getResources().getColor(mTextColors[1]));
            } else {
                scrollNumber.setUnitTextColor(getResources().getColor(mTextColors[0]));
            }
            scrollNumber.setTextColor(getResources().getColor(mTextColors[0]));
        }
    }

    /** 设置数字Text字体大小 */
    public void setTextSize(int textSize) {
        if (textSize <= 0) throw new IllegalArgumentException("text size must > 0!");
        mTextSize = textSize;
        for (ScrollNumber s : mScrollNumbers) {
            s.setTextSize(textSize);
        }
    }

    /** 设置文字Text字体大小(数字和文字混排) */
    public void setUnitTextSize(int numberUnitSize) {
        if (numberUnitSize <= 0) throw new IllegalArgumentException("number unit text size must > 0!");
        mUnitTextSize = numberUnitSize;
        for (ScrollNumber s : mScrollNumbers) {
            s.setUnitTextSize(mUnitTextSize);
        }
    }

    /** 设置数字Text背景 */
    public void setScrollNumberBackgroundResource(int resId) {
        if(resId < 0) {
            return;
        }
        this.numberResId = resId;
        if(null != mTargetNumbers && mScrollNumbers.size() > 0) {
            for (int i = 0; i < mTargetNumbers.size(); i++) {
                if(ScrollNumber.isNumeric(mTargetNumbers.get(i))) {
                    if(mScrollNumbers.size() > i) {
                        mScrollNumbers.get(i).setBackgroundResource(resId);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /** 设置文字Text背景资源 */
    public void setScrollNumberUnitBackgroundResource(int resId) {
        if(resId < 0) {
            return;
        }
        this.numberUnitResId = resId;
        if(null != mTargetNumbers && mScrollNumbers.size() > 0) {
            for (int i = 0; i < mTargetNumbers.size(); i++) {
                if(ScrollNumber.isEmptyChar(mTargetNumbers.get(i))
                        && !ScrollNumber.isNumeric(mTargetNumbers.get(i))) {
                    if(mScrollNumbers.size() > i) {
                        mScrollNumbers.get(i).setBackgroundResource(resId);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    public void setInterpolator(Interpolator interpolator) {
        if (interpolator == null)
            throw new IllegalArgumentException("interpolator couldn't be null");
        mInterpolator = interpolator;
        for (ScrollNumber s : mScrollNumbers) {
            s.setInterpolator(interpolator);
        }
    }

    /** 设置数字Text字体 */
    public void setTextFont(String fileName) {
        if (TextUtils.isEmpty(fileName)) throw new IllegalArgumentException("file name is null");
        mFontFileName = fileName;
        for (ScrollNumber s : mScrollNumbers) {
            s.setTextFont(fileName);
        }
    }

    /** 设置文字Text字体 */
    public void setUnitTextFont(String fileName) {
        if (TextUtils.isEmpty(fileName)) throw new IllegalArgumentException("file name is null");
        mUnitFontFileName = fileName;
        for (ScrollNumber s : mScrollNumbers) {
            s.setUnitTextFont(fileName);
        }
    }
}
