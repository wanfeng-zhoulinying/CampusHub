package com.campushub.mapper;

import com.campushub.entity.Booking;
import com.campushub.entity.VenueSlot;
import com.campushub.vo.BookingListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookingMapper {

    VenueSlot getVenueSlotById(@Param("slotId") Long slotId);

    int decreaseSlotCapacity(@Param("slotId") Long slotId, @Param("personCount") Integer personCount);

    int restoreSlotCapacity(@Param("slotId") Long slotId, @Param("personCount") Integer personCount);

    int saveBooking(Booking booking);

    Booking getBookingById(@Param("id") Long id);

    List<BookingListVO> listUserBookings(@Param("userId") Long userId, @Param("status") Integer status);

    int cancelBooking(@Param("id") Long id, @Param("cancelReason") String cancelReason);

    int checkinBooking(@Param("id") Long id);

    int markBookingBreach(@Param("id") Long id);
}
