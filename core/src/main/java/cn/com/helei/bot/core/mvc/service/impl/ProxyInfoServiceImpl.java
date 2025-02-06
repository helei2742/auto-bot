package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.com.helei.bot.core.mvc.mapper.ProxyInfoMapper;
import cn.com.helei.bot.core.mvc.service.IProxyInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Slf4j
@Service
public class ProxyInfoServiceImpl extends ServiceImpl<ProxyInfoMapper, ProxyInfo> implements IProxyInfoService {
    @Override
    public Integer insertOrUpdate(ProxyInfo proxyInfo) {
        proxyInfo.setInsertDatetime(LocalDateTime.now());
        proxyInfo.setUpdateDatetime(LocalDateTime.now());
        proxyInfo.setIsValid(1);

        return baseMapper.insertOrUpdate(proxyInfo);
    }

    @Override
    public Integer insertOrUpdateBatch(List<ProxyInfo> browserEnvs) {
        int successCount = 0;
        for (ProxyInfo proxyInfo : browserEnvs) {
            try {
                Integer count = insertOrUpdate(proxyInfo);
                successCount += count == null ? 0 : count;;
            } catch (Exception e) {
                log.error("insert or update [{}] error", proxyInfo, e);
            }
        }

        return successCount;
    }
}
