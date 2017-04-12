package com.baidu.fis.velocity.tools;

import com.baidu.fis.util.Settings;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig; 
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;
import org.apache.velocity.tools.config.DefaultKey;
import java.util.Date;

@DefaultKey("jello")
public class JelloTool {
    public static String jsonEncode(Object obj) {
    	String dateFormat = Settings.getString("jsonDateFormat");

    	if(dateFormat == null) {
    		return JSON.toJSONString(obj);
    	} else {
    		return jsonEncode(obj, dateFormat);
    	}
    }
    public static String jsonEncode(Object obj, String dateFormat) {
    	SerializeConfig mapping = new SerializeConfig();
    	mapping.put(Date.class, new SimpleDateFormatSerializer(dateFormat)); 
        return JSON.toJSONString(obj, mapping);
    }
}
