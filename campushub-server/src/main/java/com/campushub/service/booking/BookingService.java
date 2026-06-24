package com.campushub.service.booking;

import com.campushub.dto.BookingCancelDTO;
import com.campushub.dto.BookingCreateDTO;
import com.campushub.dto.BookingQueryDTO;
import com.campushub.vo.BookingListVO;

import java.util.List;

public interface BookingService {

    Long createBooking(BookingCreateDTO createDTO);

    List<BookingListVO> listMyBookings(BookingQueryDTO queryDTO);

    void cancelBooking(Long bookingId, BookingCancelDTO cancelDTO);

    void checkinBooking(Long bookingId);
}
