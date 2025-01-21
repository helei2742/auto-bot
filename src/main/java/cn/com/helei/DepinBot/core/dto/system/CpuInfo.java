package cn.com.helei.DepinBot.core.dto.system;

import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Builder
public class CpuInfo {

    public String archName;

    public List<String> features;

    public String modelName;

    public int numOfProcessors;

    public List<CpuProcessor> cpuProcessors;

    public List<JSONObject> temperatures;

    @Getter
    public static class CpuProcessor {

        private final JSONObject usage;

        public CpuProcessor() {
            this.usage = new JSONObject();
        }

        public void setUsage(Double idle, Double kernel, Double total, Double user) {
            this.usage.put("idle", idle);
            this.usage.put("kernel", kernel);
            this.usage.put("total", total);
            this.usage.put("user", user);
        }
    }
}
