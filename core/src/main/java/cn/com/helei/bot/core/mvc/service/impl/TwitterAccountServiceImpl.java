package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.entity.TwitterAccount;
import cn.com.helei.bot.core.mvc.mapper.TwitterAccountMapper;
import cn.com.helei.bot.core.mvc.service.ITwitterAccountService;
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
public class TwitterAccountServiceImpl extends ServiceImpl<TwitterAccountMapper, TwitterAccount> implements ITwitterAccountService {
    @Override
    public Integer insertOrUpdate(TwitterAccount twitterAccount) {
        twitterAccount.setInsertDatetime(LocalDateTime.now());
        twitterAccount.setUpdateDatetime(LocalDateTime.now());
        twitterAccount.setIsValid(1);

        return baseMapper.insertOrUpdate(twitterAccount);
    }

    @Override
    public Integer insertOrUpdateBatch(List<TwitterAccount> browserEnvs) {
        int successCount = 0;
        for (TwitterAccount twitterAccount : browserEnvs) {
            try {
                Integer count = insertOrUpdate(twitterAccount);
                successCount += count == null ? 0 : count;;
            } catch (Exception e) {
                log.error("insert or update [{}] error", twitterAccount, e);
            }
        }

        return successCount;
    }
}
