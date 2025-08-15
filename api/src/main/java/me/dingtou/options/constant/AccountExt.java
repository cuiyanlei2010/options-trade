package me.dingtou.options.constant;

/**
 * 账户扩展字段枚举
 * 用于管理OwnerAccount的ext字段中的键名
 *
 * @author qiyan
 */
public enum AccountExt {
    // 长桥平台配置
    LONGPORT_APP_KEY("longport_app_key", "长桥平台App Key", "text", 100),
    LONGPORT_APP_SECRET("longport_app_secret", "长桥平台App Secret", "text", 110),
    LONGPORT_ACCESS_TOKEN("longport_access_token", "长桥平台Access Token", "text", 120),
    
    // AI配置
    AI_BASE_URL("ai_base_url", "AI接口Base URL", "text", 200),
    AI_API_MODEL("ai_api_model", "AI模型名称", "text", 210),
    AI_API_KEY("ai_api_key", "AI接口Key", "text", 220),
    AI_API_TEMPERATURE("ai_api_temperature", "AI温度参数", "text", 230),
    AI_MCP_SETTINGS("ai_mcp_settings", "MCP服务器配置", "textarea", 240),
    
    // 分析配置
    KLINE_PERIOD("kline_period", "K线周期DAY/WEEK", "text", 300),
    
    // 账户资金配置
    ACCOUNT_SIZE("account_size", "账户资金规模", "text", 400),
    MARGIN_RATIO("margin_ratio", "保证金比例", "text", 410),
    POSITION_RATIO("position_ratio", "头寸比例", "text", 420);
    
    private final String key;
    private final String desc;
    private final String type;  // 字段类型：text, textarea, select, number
    private final int sort;     // 排序字段
    
    
    AccountExt(String key, String desc, String type, int sort) {
        this.key = key;
        this.desc = desc;
        this.type = type;
        this.sort = sort;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public String getType() {
        return type;
    }
    
    public int getSort() {
        return sort;
    }
    
    /**
     * 获取所有账户扩展字段的元数据
     * @return 所有字段的元数据数组
     */
    public static AccountExt[] getAllFields() {
        return AccountExt.values();
    }
}