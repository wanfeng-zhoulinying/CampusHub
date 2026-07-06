package com.campushub.service.credit;

import com.campushub.constant.BookingAppealStatusConstant;
import com.campushub.constant.BookingBreachFlagConstant;
import com.campushub.constant.BookingStatusConstant;
import com.campushub.constant.CreditBusinessTypeConstant;
import com.campushub.constant.CreditChangeTypeConstant;
import com.campushub.constant.CreditRuleConstant;
import com.campushub.constant.DeleteStatusConstant;
import com.campushub.constant.MessageTypeConstant;
import com.campushub.dto.AdminBookingBreachAppealAuditDTO;
import com.campushub.dto.AdminCreditBreachDTO;
import com.campushub.dto.BookingBreachAppealCreateDTO;
import com.campushub.entity.Booking;
import com.campushub.entity.BookingBreachAppeal;
import com.campushub.entity.CreditRecord;
import com.campushub.entity.SysUser;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.BookingMapper;
import com.campushub.mapper.CreditMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.service.message.MessageService;
import com.campushub.utils.UserContext;
import com.campushub.vo.AdminBookingBreachAppealVO;
import com.campushub.vo.BookingBreachAppealVO;
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
        Integer totalDeductScore = creditMapper.sumUserDeductScore(currentUserId);
        Integer totalRestoreScore = creditMapper.sumUserRestoreScore(currentUserId);

        CreditOverviewVO overviewVO = new CreditOverviewVO();
        overviewVO.setUserId(currentUserId);
        overviewVO.setCreditScore(user.getCreditScore());
        overviewVO.setTotalDeductScore(totalDeductScore);
        overviewVO.setTotalRestoreScore(totalRestoreScore);
        overviewVO.setNetDeductScore(totalDeductScore - totalRestoreScore);
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

        Booking booking = getRequiredBooking(bookingId);
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

        updateUserCreditScore(user.getId(), newScore);
        saveCreditRecord(
                user.getId(),
                CreditChangeTypeConstant.DECREASE,
                actualDeductScore,
                newScore,
                breachDTO.getReason(),
                CreditBusinessTypeConstant.BOOKING_BREACH,
                bookingId,
                getCurrentUserId()
        );

        messageService.createMessage(
                user.getId(),
                "信用分变动通知",
                "你的场地预约已被登记违约，信用分扣减 " + actualDeductScore + " 分，当前信用分为 " + newScore + " 分。预约单号：" + booking.getBookingNo(),
                MessageTypeConstant.CREDIT,
                bookingId
        );
    }

    /**
     * 提交预约违约申诉。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBookingBreachAppeal(Long bookingId, BookingBreachAppealCreateDTO createDTO) {
        Long currentUserId = getCurrentUserId();
        if (bookingId == null) {
            throw new BusinessException("预约ID不能为空");
        }
        if (createDTO == null) {
            throw new BusinessException("申诉信息不能为空");
        }
        if (createDTO.getReason() == null || createDTO.getReason().isBlank()) {
            throw new BusinessException("申诉原因不能为空");
        }

        Booking booking = getRequiredBooking(bookingId);
        if (!booking.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权申诉他人的预约违约");
        }
        if (!BookingBreachFlagConstant.BREACHED.equals(booking.getBreachFlag())
                || !BookingStatusConstant.BREACHED.equals(booking.getStatus())) {
            throw new BusinessException("当前预约未处于可申诉的违约状态");
        }
        if (creditMapper.getBookingBreachAppealByBookingId(bookingId) != null) {
            throw new BusinessException("该预约已提交过违约申诉");
        }

        Integer deductScore = creditMapper.getUserDeductScoreByBusiness(
                currentUserId,
                CreditBusinessTypeConstant.BOOKING_BREACH,
                bookingId
        );
        if (deductScore == null || deductScore <= 0) {
            throw new BusinessException("未找到该预约对应的违约扣分记录");
        }

        BookingBreachAppeal appeal = new BookingBreachAppeal();
        appeal.setBookingId(bookingId);
        appeal.setUserId(currentUserId);
        appeal.setReason(createDTO.getReason());
        appeal.setAppealStatus(BookingAppealStatusConstant.PENDING);
        appeal.setDeductScore(deductScore);
        appeal.setIsDeleted(DeleteStatusConstant.NOT_DELETED);
        creditMapper.saveBookingBreachAppeal(appeal);

        messageService.createMessage(
                currentUserId,
                "违约申诉提交成功",
                "你已提交预约违约申诉，请等待管理员审核。预约单号：" + booking.getBookingNo(),
                MessageTypeConstant.AUDIT,
                appeal.getId()
        );
        return appeal.getId();
    }

    /**
     * 查询我的违约申诉列表。
     */
    @Override
    public List<BookingBreachAppealVO> listMyBookingBreachAppeals(Integer appealStatus) {
        return creditMapper.listUserBookingBreachAppeals(getCurrentUserId(), appealStatus);
    }

    /**
     * 后台查询违约申诉列表。
     */
    @Override
    public List<AdminBookingBreachAppealVO> listAdminBookingBreachAppeals(Integer appealStatus) {
        return creditMapper.listAdminBookingBreachAppeals(appealStatus);
    }

    /**
     * 后台审核违约申诉。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditBookingBreachAppeal(Long appealId, AdminBookingBreachAppealAuditDTO auditDTO) {
        if (appealId == null) {
            throw new BusinessException("申诉ID不能为空");
        }
        if (auditDTO == null) {
            throw new BusinessException("审核信息不能为空");
        }
        if (!isValidAppealAuditStatus(auditDTO.getAuditStatus())) {
            throw new BusinessException("审核状态不合法");
        }
        if (BookingAppealStatusConstant.REJECTED.equals(auditDTO.getAuditStatus())
                && (auditDTO.getAuditRemark() == null || auditDTO.getAuditRemark().isBlank())) {
            throw new BusinessException("驳回申诉时审核备注不能为空");
        }

        BookingBreachAppeal appeal = creditMapper.getBookingBreachAppealById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉记录不存在");
        }
        if (!BookingAppealStatusConstant.PENDING.equals(appeal.getAppealStatus())) {
            throw new BusinessException("当前申诉状态不允许重复审核");
        }

        Booking booking = getRequiredBooking(appeal.getBookingId());
        SysUser user = getValidUser(appeal.getUserId());

        int affectedRows = creditMapper.auditBookingBreachAppeal(
                appealId,
                auditDTO.getAuditStatus(),
                auditDTO.getAuditRemark(),
                getCurrentUserId()
        );
        if (affectedRows == 0) {
            throw new BusinessException("审核违约申诉失败");
        }

        if (BookingAppealStatusConstant.APPROVED.equals(auditDTO.getAuditStatus())) {
            if (!BookingBreachFlagConstant.BREACHED.equals(booking.getBreachFlag())
                    || !BookingStatusConstant.BREACHED.equals(booking.getStatus())) {
                throw new BusinessException("当前预约状态不允许通过申诉恢复");
            }

            int newScore = (user.getCreditScore() == null ? CreditRuleConstant.DEFAULT_SCORE : user.getCreditScore())
                    + appeal.getDeductScore();

            int revertRows = bookingMapper.revertBookingBreach(booking.getId());
            if (revertRows == 0) {
                throw new BusinessException("撤销预约违约标记失败");
            }

            updateUserCreditScore(user.getId(), newScore);
            saveCreditRecord(
                    user.getId(),
                    CreditChangeTypeConstant.INCREASE,
                    appeal.getDeductScore(),
                    newScore,
                    buildAppealApprovedReason(appeal, auditDTO.getAuditRemark()),
                    CreditBusinessTypeConstant.BOOKING_BREACH_APPEAL,
                    booking.getId(),
                    getCurrentUserId()
            );

            messageService.createMessage(
                    user.getId(),
                    "违约申诉审核通过",
                    "你的预约违约申诉已审核通过，已恢复 " + appeal.getDeductScore() + " 分信用分。预约单号：" + booking.getBookingNo(),
                    MessageTypeConstant.AUDIT,
                    appealId
            );
            return;
        }

        messageService.createMessage(
                user.getId(),
                "违约申诉审核未通过",
                "你的预约违约申诉未通过审核。预约单号：" + booking.getBookingNo() + "。审核备注：" + auditDTO.getAuditRemark(),
                MessageTypeConstant.AUDIT,
                appealId
        );
    }

    private void saveCreditRecord(Long userId,
                                  Integer changeType,
                                  Integer changeScore,
                                  Integer currentScore,
                                  String reason,
                                  Integer businessType,
                                  Long businessId,
                                  Long operatorId) {
        CreditRecord creditRecord = new CreditRecord();
        creditRecord.setUserId(userId);
        creditRecord.setChangeType(changeType);
        creditRecord.setChangeScore(changeScore);
        creditRecord.setCurrentScore(currentScore);
        creditRecord.setReason(reason);
        creditRecord.setBusinessType(businessType);
        creditRecord.setBusinessId(businessId);
        creditRecord.setOperatorId(operatorId);
        creditMapper.saveCreditRecord(creditRecord);
    }

    private void updateUserCreditScore(Long userId, Integer creditScore) {
        int affectedRows = creditMapper.updateUserCreditScore(userId, creditScore);
        if (affectedRows == 0) {
            throw new BusinessException("更新用户信用分失败");
        }
    }

    private boolean isValidAppealAuditStatus(Integer auditStatus) {
        return BookingAppealStatusConstant.APPROVED.equals(auditStatus)
                || BookingAppealStatusConstant.REJECTED.equals(auditStatus);
    }

    private String buildAppealApprovedReason(BookingBreachAppeal appeal, String auditRemark) {
        if (auditRemark == null || auditRemark.isBlank()) {
            return "预约违约申诉通过：" + appeal.getReason();
        }
        return "预约违约申诉通过：" + auditRemark;
    }

    private Booking getRequiredBooking(Long bookingId) {
        Booking booking = bookingMapper.getBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException("预约记录不存在");
        }
        return booking;
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
