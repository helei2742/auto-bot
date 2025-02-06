package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.entity.AccountBaseInfo;
import cn.com.helei.bot.core.mvc.mapper.AccountBaseInfoMapper;
import cn.com.helei.bot.core.mvc.service.IAccountBaseInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Slf4j
@Service
public class AccountBaseInfoServiceImpl extends ServiceImpl<AccountBaseInfoMapper, AccountBaseInfo> implements IAccountBaseInfoService {

    @Override
    public Integer insertOrUpdate(AccountBaseInfo accountBaseInfo) {
        accountBaseInfo.setInsertDatetime(LocalDateTime.now());
        accountBaseInfo.setUpdateDatetime(LocalDateTime.now());
        accountBaseInfo.setIsValid(1);

        return baseMapper.insertOrUpdate(accountBaseInfo);
    }

    @Override
    public Integer insertOrUpdateBatch(List<AccountBaseInfo> accountBaseInfos) {
        int successCount = 0;
        for (AccountBaseInfo accountBaseInfo : accountBaseInfos) {
            try {
                Integer count = insertOrUpdate(accountBaseInfo);
                successCount += count == null ? 0 : count;;
            } catch (Exception e) {
                log.error("insert or update [{}] error", accountBaseInfo, e);
            }
        }

        return successCount;
    }
}
