package cn.com.helei.bot.core.mvc.mapper;

import cn.com.helei.bot.core.entity.ProxyInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface ProxyInfoMapper extends BaseMapper<ProxyInfo> {

    Integer insertOrUpdate(ProxyInfo proxyInfo);

}
