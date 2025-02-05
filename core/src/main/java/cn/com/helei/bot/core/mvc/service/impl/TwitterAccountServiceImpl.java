package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.entity.TwitterAccount;
import cn.com.helei.bot.core.mvc.mapper.TwitterAccountMapper;
import cn.com.helei.bot.core.mvc.service.ITwitterAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Service
public class TwitterAccountServiceImpl extends ServiceImpl<TwitterAccountMapper, TwitterAccount> implements ITwitterAccountService {

}
