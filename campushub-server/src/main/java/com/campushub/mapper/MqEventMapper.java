package com.campushub.mapper;

import com.campushub.entity.MqEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MqEventMapper {

    int saveMqEvent(MqEvent mqEvent);

    List<MqEvent> listPendingEvents(@Param("limit") int limit);

    int markSent(@Param("id") Long id);

    int markDead(@Param("id") Long id);

    int increaseRetryCount(@Param("id") Long id);
}
