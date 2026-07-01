package com.campushub.mapper;

import com.campushub.entity.CreditRecord;
import com.campushub.vo.CreditRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CreditMapper {

    int saveCreditRecord(CreditRecord creditRecord);

    List<CreditRecordVO> listUserCreditRecords(@Param("userId") Long userId);

    Integer sumUserDeductScore(@Param("userId") Long userId);

    Integer countUserBookingBreaches(@Param("userId") Long userId);

    int updateUserCreditScore(@Param("userId") Long userId, @Param("creditScore") Integer creditScore);
}
