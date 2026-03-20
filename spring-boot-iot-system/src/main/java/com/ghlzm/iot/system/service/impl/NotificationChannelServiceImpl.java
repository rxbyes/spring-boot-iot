package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.mapper.NotificationChannelMapper;
import com.ghlzm.iot.system.service.NotificationChannelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class NotificationChannelServiceImpl extends ServiceImpl<NotificationChannelMapper, NotificationChannel>
        implements NotificationChannelService {

      @Override
      @Transactional(rollbackFor = Exception.class)
      public NotificationChannel addChannel(NotificationChannel channel) {
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationChannel::getTenantId, channel.getTenantId())
                    .eq(NotificationChannel::getChannelCode, channel.getChannelCode())
                    .eq(NotificationChannel::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("渠道编码已存在");
            }

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
      public List<NotificationChannel> listChannels(String channelName, String channelCode, String channelType) {
            return this.list(buildChannelQueryWrapper(channelName, channelCode, channelType));
      }

      @Override
      public PageResult<NotificationChannel> pageChannels(String channelName, String channelCode, String channelType, Long pageNum, Long pageSize) {
            Page<NotificationChannel> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<NotificationChannel> result = page(page, buildChannelQueryWrapper(channelName, channelCode, channelType));
            return PageQueryUtils.toPageResult(result);
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

      private LambdaQueryWrapper<NotificationChannel> buildChannelQueryWrapper(String channelName, String channelCode, String channelType) {
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationChannel::getDeleted, 0);
            if (StringUtils.hasText(channelName)) {
                  queryWrapper.like(NotificationChannel::getChannelName, channelName.trim());
            }
            if (StringUtils.hasText(channelCode)) {
                  queryWrapper.like(NotificationChannel::getChannelCode, channelCode.trim());
            }
            if (StringUtils.hasText(channelType)) {
                  queryWrapper.eq(NotificationChannel::getChannelType, channelType.trim());
            }
            queryWrapper.orderByAsc(NotificationChannel::getSortNo).orderByAsc(NotificationChannel::getId);
            return queryWrapper;
      }
}
