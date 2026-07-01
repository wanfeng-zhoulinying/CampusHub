package com.campushub.service.credit;

import com.campushub.dto.AdminCreditBreachDTO;
import com.campushub.vo.CreditOverviewVO;
import com.campushub.vo.CreditRecordVO;

import java.util.List;

public interface CreditService {

    CreditOverviewVO getMyCreditOverview();

    List<CreditRecordVO> listMyCreditRecords();

    void markBookingBreach(Long bookingId, AdminCreditBreachDTO breachDTO);
}
