package com.campushub.mapper;

import com.campushub.entity.BookingBreachAppeal;
import com.campushub.entity.CreditRecord;
import com.campushub.vo.AdminBookingBreachAppealVO;
import com.campushub.vo.BookingBreachAppealVO;
import com.campushub.vo.CreditRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CreditMapper {

    int saveCreditRecord(CreditRecord creditRecord);

    List<CreditRecordVO> listUserCreditRecords(@Param("userId") Long userId);

    Integer sumUserDeductScore(@Param("userId") Long userId);

    Integer sumUserRestoreScore(@Param("userId") Long userId);

    Integer countUserBookingBreaches(@Param("userId") Long userId);

    int updateUserCreditScore(@Param("userId") Long userId, @Param("creditScore") Integer creditScore);

    Integer getUserDeductScoreByBusiness(@Param("userId") Long userId,
                                         @Param("businessType") Integer businessType,
                                         @Param("businessId") Long businessId);

    int saveBookingBreachAppeal(BookingBreachAppeal appeal);

    BookingBreachAppeal getBookingBreachAppealById(@Param("id") Long id);

    BookingBreachAppeal getBookingBreachAppealByBookingId(@Param("bookingId") Long bookingId);

    List<BookingBreachAppealVO> listUserBookingBreachAppeals(@Param("userId") Long userId,
                                                             @Param("appealStatus") Integer appealStatus);

    List<AdminBookingBreachAppealVO> listAdminBookingBreachAppeals(@Param("appealStatus") Integer appealStatus);

    int auditBookingBreachAppeal(@Param("id") Long id,
                                 @Param("appealStatus") Integer appealStatus,
                                 @Param("auditRemark") String auditRemark,
                                 @Param("auditUserId") Long auditUserId);
}
