package me.dingtou.options.manager;

import me.dingtou.options.client.NasdaqClient;
import me.dingtou.options.dao.EarningsCalendarDAO;
import me.dingtou.options.model.EarningsCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 财报日历管理器实现
 */
@Component
public class EarningsCalendarManager {

    @Autowired
    private EarningsCalendarDAO earningsCalendarDAO;

    @Autowired
    private NasdaqClient nasdaqClient;

    /**
     * 同步指定日期的财报日历数据
     * 
     * @param date 指定日期
     */
    public void syncEarningsCalendarForDate(Date date) {
        // 调用NASDAQ API获取指定日期的财报数据
        List<EarningsCalendar> earningsCalendars = nasdaqClient.getEarningsCalendarByDate(date);

        // 设置财报日期
        for (EarningsCalendar calendar : earningsCalendars) {
            calendar.setEarningsDate(date);
        }

        // 保存数据
        batchSaveEarningsCalendar(earningsCalendars);
    }

    /**
     * 批量保存财报日历数据
     * 
     * @param earningsCalendars 财报日历列表
     */
    public void batchSaveEarningsCalendar(List<EarningsCalendar> earningsCalendars) {
        if (earningsCalendars != null && !earningsCalendars.isEmpty()) {
            Date now = new Date();
            for (EarningsCalendar earningsCalendar : earningsCalendars) {
                // 设置创建时间和更新时间
                earningsCalendar.setCreatedTime(now);
                earningsCalendar.setUpdatedTime(now);
                earningsCalendarDAO.insertReplace(earningsCalendar);
            }
        }
    }

    /**
     * 根据股票代码获取近期的财报发布信息
     * 
     * @param symbol 股票代码
     * @return 财报日历列表
     */
    public List<EarningsCalendar> getEarningsCalendarBySymbol(String symbol) {
        return earningsCalendarDAO.selectBySymbol(symbol);
    }

    /**
     * 根据日期获取财报信息
     * 
     * @param date 指定日期
     * @return 财报日历列表
     */
    public List<EarningsCalendar> getEarningsCalendarByDate(Date date) {
        return earningsCalendarDAO.selectByDate(date);
    }
}