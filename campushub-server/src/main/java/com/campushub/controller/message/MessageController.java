package com.campushub.controller.message;

import com.campushub.common.Result;
import com.campushub.service.message.MessageService;
import com.campushub.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    /**
     * 我的通知列表接口。
     * 支持按已读状态筛选，不传 readStatus 时返回全部通知。
     */
    @GetMapping("/my")
    public Result<List<MessageVO>> listMyMessages(@RequestParam(value = "readStatus", required = false) Integer readStatus) {
        log.info("[Message] my messages readStatus={}", readStatus);
        return Result.success(messageService.listMyMessages(readStatus));
    }

    /**
     * 标记通知已读接口。
     * 只能操作当前登录用户自己的通知。
     */
    @PutMapping("/{messageId}/read")
    public Result<Void> markAsRead(@PathVariable("messageId") Long messageId) {
        log.info("[Message] mark read messageId={}", messageId);
        messageService.markAsRead(messageId);
        return Result.success();
    }

    /**
     * 我的未读通知数量接口。
     * 用于前端消息红点或后台顶部消息提示。
     */
    @GetMapping("/unread/count")
    public Result<Integer> countMyUnreadMessages() {
        log.info("[Message] unread count");
        return Result.success(messageService.countMyUnreadMessages());
    }
}
