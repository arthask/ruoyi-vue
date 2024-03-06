package com.ruoyi.system.service.statistics.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.vo.StatisticsCountVo;
import com.ruoyi.system.domain.vo.UserWordPeriodVo;
import com.ruoyi.system.mapper.UserStudyRecordMapper;
import com.ruoyi.system.mapper.UserWordMapper;
import com.ruoyi.system.mapper.WordMapper;
import com.ruoyi.system.service.statistics.IStatisticsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatisticsServiceImpl implements IStatisticsService {

    public static final String COUNT = "count";

    public static final String EXCEPT = "except";
    public static final String ACTUAL = "actual";

    public static final String TOTAL = "total";
    public static final String NOT_STUDY = "notStudy";

    public static final String NEED_REVIEW = "needReview";
    public static final String HAVE_REVIEW = "haveReview";
    public static final String TOTAL_REVIEW_NUM = "totalReviewNum";
    @Autowired
    private WordMapper wordMapper;

    @Autowired
    private UserWordMapper userWordMapper;

    @Autowired
    private UserStudyRecordMapper userStudyRecordMapper;

    private static final int MONTH_NUM = 12;

    @Override
    public StatisticsCountVo getStatisticsOfCount(Long userId) {
        StatisticsCountVo result = new StatisticsCountVo();
        // 目前是1，只有程序员词汇库
        result.setWordDataBaseNum(1L);
        result.setWordCount(wordMapper.getWordCount(userId));
        result.setMyWordCount(userWordMapper.getWordCount(userId));
        result.setStudyRecordCount(userStudyRecordMapper.getStudyRecordCount(userId));
        return result;
    }

    @Override
    public Long[] getStudyRecordOfMonth(Long userId) {
        Long[] result = initMonthDataArray(MONTH_NUM);
        Date now = new Date();
        DateTime startDate = DateUtil.beginOfYear(now);
        DateTime endDate = DateUtil.endOfYear(now);
        String beginStr = DateFormatUtils.format(startDate, DateUtils.YYYY_MM_DD_HH_MM_SS);
        String endStr = DateFormatUtils.format(endDate, DateUtils.YYYY_MM_DD_HH_MM_SS);
        List<Long> studyRecordOfMonthList = userStudyRecordMapper.getStudyRecordOfMonth(userId, beginStr, endStr);
        if (CollectionUtil.isEmpty(studyRecordOfMonthList)) {
            return result;
        }
        for (int i = 0; i < studyRecordOfMonthList.size(); i++) {
            result[i] = studyRecordOfMonthList.get(i);
        }
        return result;
    }

    @Override
    public Long[] getUserWordOfMonth(Long userId) {
        Long[] result = initMonthDataArray(MONTH_NUM);
        Date now = new Date();
        DateTime startDate = DateUtil.beginOfYear(now);
        DateTime endDate = DateUtil.endOfYear(now);
        String beginStr = DateFormatUtils.format(startDate, DateUtils.YYYY_MM_DD_HH_MM_SS);
        String endStr = DateFormatUtils.format(endDate, DateUtils.YYYY_MM_DD_HH_MM_SS);
        List<Long> userWordOfMonthList = userWordMapper.getUserWordOfMonth(userId, beginStr, endStr);
        if (CollectionUtil.isEmpty(userWordOfMonthList)) {
            return result;
        }
        for (int i = 0; i < userWordOfMonthList.size(); i++) {
            result[i] = userWordOfMonthList.get(i);
        }
        return result;
    }

    @Override
    public List<UserWordPeriodVo> getUserWordPeriodCount(Long userId) {

        List<UserWordPeriodVo> userWordPeriodCount = userWordMapper.getUserWordPeriodCount(userId);
        if (CollectionUtil.isEmpty(userWordPeriodCount)) {
            return Collections.emptyList();
        }
        Map<String, Long> periodMap = new HashMap<>();
        userWordPeriodCount.forEach(e -> {
            if (e.getPeriod() == 0) {
                setPeriodName(e, periodMap, "新学习");
            } else if (e.getPeriod() >= 1 && e.getPeriod() < 3) {
                setPeriodName(e, periodMap, "了解");
            } else if (e.getPeriod() >= 3 && e.getPeriod() < 6) {
                setPeriodName(e, periodMap, "熟悉");
            } else if (e.getPeriod() >= 6 && e.getPeriod() < 9) {
                setPeriodName(e, periodMap, "掌握");
            } else {
                setPeriodName(e, periodMap, "应用");
            }
        });
        List<UserWordPeriodVo> translateList = new ArrayList<>();
        periodMap.forEach((key, value) -> {
            UserWordPeriodVo userWordPeriodVo = new UserWordPeriodVo();
            userWordPeriodVo.setName(key);
            userWordPeriodVo.setValue(value);
            translateList.add(userWordPeriodVo);
        });

        return translateList;
    }

    @Override
    public Map<String, Long[]> getExceptAndActualValueOfDay(Long userId) {
        // 查询期望值数组
        boolean isLeapYear = DateUtil.isLeapYear(DateUtil.thisYear());
        // 当前月份, 从0开始
        int month = DateUtil.thisMonth();
        DateTime beginOfMonth = DateUtil.beginOfMonth(DateUtil.date());
        DateTime endOfMonth = DateUtil.endOfMonth(DateUtil.date());
        // 用于横坐标的展示,
        int lengthOfMonth = DateUtil.lengthOfMonth(month + 1, isLeapYear);
        Map<String, Long[]> result = new HashMap<>();
        Long[] exceptArray = initMonthDataArray(lengthOfMonth);
        Long[] actualArray = initMonthDataArray(lengthOfMonth);
        // 当天有期望学习时间才会有值
        Map<String, Map<String, Long>> exceptValueOfDay = userWordMapper.getExceptValueOfDay(
                userId,
                DateUtil.format(beginOfMonth, DateUtils.YYYY_MM_DD_HH_MM_SS),
                DateUtil.format(endOfMonth, DateUtils.YYYY_MM_DD_HH_MM_SS));
        exceptValueOfDay.forEach((k, v) -> {
            // 截取对应的天数
            String day = StringUtils.substring(k, 6);
            int index = Integer.parseInt(day) - 1;
            exceptArray[index] = v.get(COUNT);
        });
        result.put(EXCEPT, exceptArray);
        // 当天单词学习记录
        Map<String, Map<String, Long>> actualValueOfDay = userStudyRecordMapper.getActualValueOfDay(userId, DateUtil.format(beginOfMonth, DateUtils.YYYY_MM_DD_HH_MM_SS),
                DateUtil.format(endOfMonth, DateUtils.YYYY_MM_DD_HH_MM_SS));
        actualValueOfDay.forEach((k, v) -> {
            // 截取对应的天数
            String day = StringUtils.substring(k, 6);
            int index = Integer.parseInt(day) - 1;
            actualArray[index] = v.get(COUNT);
        });
        result.put(ACTUAL, actualArray);
        return result;
    }

    @Override
    public Map<String, Long> getTotalAndNotStudyNum(Long userId) {
        Map<String, Long> result = new HashMap<>();
        Long wordCount = wordMapper.getWordCount(userId);
        Long notStudyNum = wordMapper.getNewWordCountOfUser(userId);
        result.put(TOTAL, wordCount);
        result.put(NOT_STUDY, notStudyNum);
        return result;
    }

    @Override
    public Map<String, Long> getNeedReviewAnHaveReviewNum(Long userId) {
        Map<String, Long> result = new HashMap<>();
        DateTime now = DateUtil.date();
        String beginStr = DateUtil.format(DateUtil.beginOfDay(now), DateUtils.YYYY_MM_DD_HH_MM_SS);
        String endStr = DateUtil.format(DateUtil.endOfDay(now), DateUtils.YYYY_MM_DD_HH_MM_SS);
        Long needReviewNumOfDay = userWordMapper.getNeedReviewNumOfDay(userId,
                beginStr,
                endStr);
        Long haveReviewNumOfDay = userWordMapper.getHaveReviewNumOfDay(userId,
                beginStr,
                endStr);
        Long totalReviewNum = userWordMapper.getTotalReviewNum(userId);
        result.put(NEED_REVIEW, needReviewNumOfDay);
        result.put(HAVE_REVIEW, haveReviewNumOfDay);
        result.put(TOTAL_REVIEW_NUM, totalReviewNum);
        return result;
    }

    private static void setPeriodName(UserWordPeriodVo e, Map<String, Long> periodMap, String key) {
        if (periodMap.containsKey(key)) {
            Long oldValue = periodMap.get(key);
            periodMap.put(key, e.getValue() + oldValue);
        } else {
            periodMap.put(key, e.getValue());
        }
        e.setName(key);
    }

    private Long[] initMonthDataArray(int size) {
        Long[] result = new Long[size];
        Arrays.fill(result, 0L);
        return result;
    }
}
