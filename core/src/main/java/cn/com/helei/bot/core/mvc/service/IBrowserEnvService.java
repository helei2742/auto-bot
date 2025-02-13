package cn.com.helei.bot.core.mvc.service;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.BrowserEnv;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface IBrowserEnvService extends IService<BrowserEnv> {

    Integer insertOrUpdate(BrowserEnv browserEnv);

    Integer insertOrUpdateBatch(List<BrowserEnv> browserEnvs);

    Result saveBrowserEnvs(List<Map<String, Object>> rawLines);
}
