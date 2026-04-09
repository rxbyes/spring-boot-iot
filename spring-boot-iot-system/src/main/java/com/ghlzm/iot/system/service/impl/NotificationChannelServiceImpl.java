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
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
public class NotificationChannelServiceImpl extends ServiceImpl<NotificationChannelMapper, NotificationChannel>
        implements NotificationChannelService {

      private static final String CHANNEL_TYPE_DICT_CODE = "notification_channel_type";
      private static final Set<String> DEFAULT_CHANNEL_TYPES =
              Set.of("email", "sms", "webhook", "wechat", "feishu", "dingtalk");

      private final PermissionService permissionService;
      private final SystemDictValueSupport systemDictValueSupport;

      public NotificationChannelServiceImpl(PermissionService permissionService,
                                            SystemDictValueSupport systemDictValueSupport) {
            this.permissionService = permissionService;
            this.systemDictValueSupport = systemDictValueSupport;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public NotificationChannel addChannel(NotificationChannel channel) {
            return addChannel(null, channel);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public NotificationChannel addChannel(Long currentUserId, NotificationChannel channel) {
            Long tenantId = resolveTenantId(currentUserId, channel.getTenantId());
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationChannel::getTenantId, tenantId)
                    .eq(NotificationChannel::getChannelCode, channel.getChannelCode())
                    .eq(NotificationChannel::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("渠道编码已存在");
            }

            channel.setChannelType(systemDictValueSupport.normalizeRequiredLowerCase(
                    currentUserId,
                    CHANNEL_TYPE_DICT_CODE,
                    channel.getChannelType(),
                    "渠道类型",
                    DEFAULT_CHANNEL_TYPES
            ));
            if (channel.getSortNo() == null) {
                  channel.setSortNo(0);
            }
            if (channel.getStatus() == null) {
                  channel.setStatus(1);
            }
            channel.setTenantId(tenantId == null ? 1L : tenantId);
            if (channel.getCreateBy() == null) {
                  channel.setCreateBy(currentUserId == null ? 1L : currentUserId);
            }

            this.save(channel);
            return channel;
      }

      @Override
      public List<NotificationChannel> listChannels(String channelName, String channelCode, String channelType) {
            return listChannels(null, channelName, channelCode, channelType);
      }

      @Override
      public List<NotificationChannel> listChannels(Long currentUserId,
                                                    String channelName,
                                                    String channelCode,
                                                    String channelType) {
            return this.list(buildChannelQueryWrapper(currentUserId, channelName, channelCode, channelType));
      }

      @Override
      public PageResult<NotificationChannel> pageChannels(String channelName, String channelCode, String channelType, Long pageNum, Long pageSize) {
            return pageChannels(null, channelName, channelCode, channelType, pageNum, pageSize);
      }

      @Override
      public PageResult<NotificationChannel> pageChannels(Long currentUserId,
                                                          String channelName,
                                                          String channelCode,
                                                          String channelType,
                                                          Long pageNum,
                                                          Long pageSize) {
            Page<NotificationChannel> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<NotificationChannel> result = page(page, buildChannelQueryWrapper(currentUserId, channelName, channelCode, channelType));
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public NotificationChannel getByCode(String channelCode) {
            return getByCode(null, channelCode);
      }

      @Override
      public NotificationChannel getByCode(Long currentUserId, String channelCode) {
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            Long tenantId = resolveTenantId(currentUserId, null);
            queryWrapper.eq(NotificationChannel::getChannelCode, channelCode)
                    .eq(tenantId != null, NotificationChannel::getTenantId, tenantId)
                    .eq(NotificationChannel::getDeleted, 0);
            NotificationChannel channel = this.getOne(queryWrapper);
            ensureChannelAccessible(currentUserId, channel);
            return channel;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateChannel(NotificationChannel channel) {
            updateChannel(null, channel);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateChannel(Long currentUserId, NotificationChannel channel) {
            NotificationChannel existing = super.getById(channel.getId());
            if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
                  throw new BizException("通知渠道不存在");
            }
            ensureChannelAccessible(currentUserId, existing);
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationChannel::getTenantId, existing.getTenantId())
                    .eq(NotificationChannel::getChannelCode, channel.getChannelCode())
                    .ne(NotificationChannel::getId, channel.getId())
                    .eq(NotificationChannel::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("渠道编码已存在");
            }

            String normalizedChannelType = StringUtils.hasText(channel.getChannelType())
                    ? channel.getChannelType()
                    : existing.getChannelType();
            channel.setChannelType(systemDictValueSupport.normalizeRequiredLowerCase(
                    currentUserId,
                    CHANNEL_TYPE_DICT_CODE,
                    normalizedChannelType,
                    "渠道类型",
                    DEFAULT_CHANNEL_TYPES
            ));
            channel.setTenantId(existing.getTenantId());
            if (channel.getUpdateBy() == null && currentUserId != null) {
                  channel.setUpdateBy(currentUserId);
            }
            this.updateById(channel);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteChannel(Long id) {
            deleteChannel(null, id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteChannel(Long currentUserId, Long id) {
            NotificationChannel existing = super.getById(id);
            if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
                  throw new BizException("通知渠道不存在");
            }
            ensureChannelAccessible(currentUserId, existing);
            this.removeById(id);
      }

      private LambdaQueryWrapper<NotificationChannel> buildChannelQueryWrapper(Long currentUserId,
                                                                               String channelName,
                                                                               String channelCode,
                                                                               String channelType) {
            LambdaQueryWrapper<NotificationChannel> queryWrapper = new LambdaQueryWrapper<>();
            Long tenantId = resolveTenantId(currentUserId, null);
            queryWrapper.eq(tenantId != null, NotificationChannel::getTenantId, tenantId);
            queryWrapper.eq(NotificationChannel::getDeleted, 0);
            if (StringUtils.hasText(channelName)) {
                  queryWrapper.like(NotificationChannel::getChannelName, channelName.trim());
            }
            if (StringUtils.hasText(channelCode)) {
                  queryWrapper.like(NotificationChannel::getChannelCode, channelCode.trim());
            }
            if (StringUtils.hasText(channelType)) {
                  queryWrapper.eq(NotificationChannel::getChannelType, channelType.trim().toLowerCase(Locale.ROOT));
            }
            queryWrapper.orderByAsc(NotificationChannel::getSortNo).orderByAsc(NotificationChannel::getId);
            return queryWrapper;
      }

      private Long resolveTenantId(Long currentUserId, Long fallbackTenantId) {
            if (currentUserId == null) {
                  return fallbackTenantId;
            }
            return permissionService.getDataPermissionContext(currentUserId).tenantId();
      }

      private void ensureChannelAccessible(Long currentUserId, NotificationChannel channel) {
            if (currentUserId == null || channel == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(channel.getTenantId())) {
                  throw new BizException("通知渠道不存在或无权访问");
            }
      }
}
