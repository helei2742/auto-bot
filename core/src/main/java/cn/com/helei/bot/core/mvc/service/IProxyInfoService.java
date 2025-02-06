package cn.com.helei.bot.core.mvc.service;

import cn.com.helei.bot.core.entity.ProxyInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface IProxyInfoService extends IService<ProxyInfo> {

    Integer insertOrUpdate(ProxyInfo proxyInfo);

    Integer insertOrUpdateBatch(List<ProxyInfo> proxyInfo);

}
