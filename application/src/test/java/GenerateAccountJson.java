import cn.com.helei.bot.core.pool.account.AccountPool;
import cn.com.helei.bot.core.pool.account.DepinClientAccount;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public class GenerateAccountJson {


    public static void main(String[] args) {
        List<DepinClientAccount> depinClientAccounts = AccountPool.getDefault().getAllItem();

        List<JSONObject> list = depinClientAccounts.stream().map(depinClientAccount -> {
            JSONObject jb = new JSONObject();
            jb.put("email", depinClientAccount.getEmail());
            jb.put("password", depinClientAccount.getPassword());
            return jb;
        }).toList();

        System.out.println(JSONObject.toJSONString(list));
    }
}
