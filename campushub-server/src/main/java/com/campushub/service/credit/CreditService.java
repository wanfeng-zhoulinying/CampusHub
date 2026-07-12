package com.campushub.service.credit;

import com.campushub.dto.AdminBookingBreachAppealAuditDTO;
import com.campushub.dto.AdminCreditBreachDTO;
import com.campushub.dto.BookingBreachAppealCreateDTO;
import com.campushub.vo.AdminBookingBreachAppealVO;
import com.campushub.vo.BookingBreachAppealVO;
import com.campushub.vo.CreditOverviewVO;
import com.campushub.vo.CreditRecordVO;

import java.util.List;

public interface CreditService {

    CreditOverviewVO getMyCreditOverview();

    List<CreditRecordVO> listMyCreditRecords();

    void markBookingBreach(Long bookingId, AdminCreditBreachDTO breachDTO);

    void markBookingBreachBySystem(Long bookingId);

    Long createBookingBreachAppeal(Long bookingId, BookingBreachAppealCreateDTO createDTO);

    List<BookingBreachAppealVO> listMyBookingBreachAppeals(Integer appealStatus);

    List<AdminBookingBreachAppealVO> listAdminBookingBreachAppeals(Integer appealStatus);

    void auditBookingBreachAppeal(Long appealId, AdminBookingBreachAppealAuditDTO auditDTO);
}
