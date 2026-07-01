package com.campushub.service.credit;

import com.campushub.constant.BookingBreachFlagConstant;
import com.campushub.constant.BookingStatusConstant;
import com.campushub.constant.CreditBusinessTypeConstant;
import com.campushub.constant.CreditChangeTypeConstant;
import com.campushub.constant.CreditRuleConstant;
import com.campushub.constant.DeleteStatusConstant;
import com.campushub.constant.MessageTypeConstant;
import com.campushub.dto.AdminCreditBreachDTO;
import com.campushub.entity.Booking;
import com.campushub.entity.CreditRecord;
import com.campushub.entity.SysUser;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.BookingMapper;
import com.campushub.mapper.CreditMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.service.message.MessageService;
import com.campushub.utils.UserContext;
import com.campushub.vo.CreditOverviewVO;
import com.campushub.vo.CreditRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CreditMapper creditMapper;
    private final UserMapper userMapper;
    private final BookingMapper bookingMapper;
    private final MessageService messageService;

    /**
     * 查询我的信用分概览。
     */
    @Override
    public CreditOverviewVO getMyCreditOverview() {
        Long currentUserId = getCurrentUserId();
        SysUser user = getValidUser(currentUserId);

        CreditOverviewVO overviewVO = new CreditOverviewVO();
        overviewVO.setUserId(currentUserId);
        overviewVO.setCreditScore(user.getCreditScore());
        overviewVO.setTotalDeductScore(creditMapper.sumUserDeductScore(currentUserId));
        overviewVO.setBreachCount(creditMapper.countUserBookingBreaches(currentUserId));
        return overviewVO;
    }

    /**
     * 查询我的信用分变动记录。
     */
    @Override
    public List<CreditRecordVO> listMyCreditRecords() {
        return creditMapper.listUserCreditRecords(getCurrentUserId());
    }

    /**
     * 管理员登记预约违约并扣减信用分。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markBookingBreach(Long bookingId, AdminCreditBreachDTO breachDTO) {
        if (bookingId == null) {
            throw new BusinessException("预约ID不能为空");
        }
        if (breachDTO == null) {
            throw new BusinessException("违约信息不能为空");
        }
        if (breachDTO.getReason() == null || breachDTO.getReason().isBlank()) {
            throw new BusinessException("违约原因不能为空");
        }

        Integer deductScore = breachDTO.getDeductScore() == null
                ? CreditRuleConstant.BOOKING_BREACH_DEDUCT_SCORE
                : breachDTO.getDeductScore();
        if (deductScore <= 0) {
            throw new BusinessException("扣减分值必须大于0");
        }

        Booking booking = bookingMapper.getBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (BookingBreachFlagConstant.BREACHED.equals(booking.getBreachFlag())) {
            throw new BusinessException("该预约已登记违约");
        }
        if (!BookingStatusConstant.BOOKED.equals(booking.getStatus())) {
            throw new BusinessException("当前预约状态不允许登记违约");
        }

        SysUser user = getValidUser(booking.getUserId());
        int currentScore = user.getCreditScore() == null ? CreditRuleConstant.DEFAULT_SCORE : user.getCreditScore();
        int newScore = Math.max(CreditRuleConstant.MIN_SCORE, currentScore - deductScore);
        int actualDeductScore = currentScore - newScore;

        int affectedRows = bookingMapper.markBookingBreach(bookingId);
        if (affectedRows == 0) {
            throw new BusinessException("登记预约违约失败");
        }

        int userAffectedRows = creditMapper.updateUserCreditScore(user.getId(), newScore);
        if (userAffectedRows == 0) {
            throw new BusinessException("更新用户信用分失败");
        }

        CreditRecord creditRecord = new CreditRecord();
        creditRecord.setUserId(user.getId());
        creditRecord.setChangeType(CreditChangeTypeConstant.DECREASE);
        creditRecord.setChangeScore(actualDeductScore);
        creditRecord.setCurrentScore(newScore);
        creditRecord.setReason(breachDTO.getReason());
        creditRecord.setBusinessType(CreditBusinessTypeConstant.BOOKING_BREACH);
        creditRecord.setBusinessId(bookingId);
        creditRecord.setOperatorId(getCurrentUserId());
        creditMapper.saveCreditRecord(creditRecord);

        messageService.createMessage(
                user.getId(),
                "信用分变动通知",
                "你的场地预约已被登记违约，信用分扣减 " + actualDeductScore + " 分，当前信用分为 " + newScore + " 分。预约单号：" + booking.getBookingNo(),
                MessageTypeConstant.CREDIT,
                bookingId
        );
    }

    private SysUser getValidUser(Long userId) {
        SysUser user = userMapper.getById(userId);
        if (user == null || !DeleteStatusConstant.NOT_DELETED.equals(user.getIsDeleted())) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        return currentUserId;
    }
}
