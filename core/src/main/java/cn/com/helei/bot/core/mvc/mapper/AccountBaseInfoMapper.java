package cn.com.helei.bot.core.mvc.mapper;

import cn.com.helei.bot.core.entity.AccountBaseInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface AccountBaseInfoMapper extends BaseMapper<AccountBaseInfo> {

    Integer insertOrUpdate(AccountBaseInfo accountBaseInfo);

}
