package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.entity.BrowserEnv;
import cn.com.helei.bot.core.mvc.mapper.BrowserEnvMapper;
import cn.com.helei.bot.core.mvc.service.IBrowserEnvService;
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
public class BrowserEnvServiceImpl extends ServiceImpl<BrowserEnvMapper, BrowserEnv> implements IBrowserEnvService {

    @Override
    public Integer insertOrUpdate(BrowserEnv browserEnv) {
        browserEnv.setInsertDatetime(LocalDateTime.now());
        browserEnv.setUpdateDatetime(LocalDateTime.now());
        browserEnv.setIsValid(1);

        return baseMapper.insertOrUpdate(browserEnv);
    }

    @Override
    public Integer insertOrUpdateBatch(List<BrowserEnv> browserEnvs) {
        int successCount = 0;
        for (BrowserEnv browserEnv : browserEnvs) {
            try {
                Integer count = insertOrUpdate(browserEnv);
                successCount += count == null ? 0 : count;;
            } catch (Exception e) {
                log.error("insert or update [{}] error", browserEnv, e);
            }
        }

        return successCount;
    }
}
