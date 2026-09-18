package com.campushub.mapper;

import com.campushub.entity.Message;
import com.campushub.vo.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    int saveMessage(Message message);

    Integer countByBusiness(@Param("userId") Long userId,
                            @Param("type") Integer type,
                            @Param("businessId") Long businessId);

    List<MessageVO> listUserMessages(@Param("userId") Long userId, @Param("readStatus") Integer readStatus);

    int markAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId);

    int countUnread(@Param("userId") Long userId);
}
