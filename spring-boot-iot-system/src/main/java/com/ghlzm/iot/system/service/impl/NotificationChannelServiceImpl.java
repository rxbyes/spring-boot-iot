package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.mapper.NotificationChannelMapper;
import com.ghlzm.iot.system.service.NotificationChannelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知渠道 Service 实现类
 */
@Service
public class NotificationChannelServiceImpl extends ServiceImpl<NotificationChannelMapper, NotificationChannel>
            implements NotificationChannelService {

      @Override
      @Transactional(rollbackFor = Exception.class)
      public NotificationChannel addChannel(NotificationChannel channel) {
            // 验证渠道编码唯一性
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationChannel::getTenantId, channel.getTenantId())
                        .eq(NotificationChannel::getChannelCode, channel.getChannelCode())
                        .eq(NotificationChannel::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("渠道编码已存在");
            }

            // 设置默认值
            if (channel.getSortNo() == null) {
                  channel.setSortNo(0);
            }
            if (channel.getStatus() == null) {
                  channel.setStatus(1);
            }

            this.save(channel);
            return channel;
      }

      @Override
      public List<NotificationChannel> listChannels() {
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationChannel::getDeleted, 0);
            queryWrapper.orderByAsc(NotificationChannel::getSortNo);
            return this.list(queryWrapper);
      }

      @Override
      public NotificationChannel getByCode(String channelCode) {
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationChannel::getChannelCode, channelCode)
                        .eq(NotificationChannel::getDeleted, 0);
            return this.getOne(queryWrapper);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateChannel(NotificationChannel channel) {
            // 验证渠道编码唯一性
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationChannel::getTenantId, channel.getTenantId())
                        .eq(NotificationChannel::getChannelCode, channel.getChannelCode())
                        .ne(NotificationChannel::getId, channel.getId())
                        .eq(NotificationChannel::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("渠道编码已存在");
            }

            this.updateById(channel);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteChannel(Long id) {
            this.removeById(id);
      }
}
