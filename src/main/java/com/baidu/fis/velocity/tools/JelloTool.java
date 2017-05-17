package com.baidu.fis.velocity.tools;

import com.baidu.fis.util.Settings;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig; 
import com.alibaba.fastjson.serializer.SerializerFeature; 
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;
import org.apache.velocity.tools.config.DefaultKey;
import java.util.Date;
import java.util.ArrayList;

@DefaultKey("jello")
public class JelloTool {
    /*

        fastjson.serializerFeatures.QuoteFieldNames
        fastjson.serializerFeatures.UseSingleQuotes     
        fastjson.serializerFeatures.WriteMapNullValue       
        fastjson.serializerFeatures.WriteEnumUsingToString      
        fastjson.serializerFeatures.WriteEnumUsingName      
        fastjson.serializerFeatures.UseISO8601DateFormat        
        fastjson.serializerFeatures.WriteNullListAsEmpty
        fastjson.serializerFeatures.fastjson.serializerFeatures.WriteNullStringAsEmpty
        fastjson.serializerFeatures.WriteNullNumberAsZero
        fastjson.serializerFeatures.WriteNullBooleanAsFalse 
        fastjson.serializerFeatures.SkipTransientField
        fastjson.serializerFeatures.SortField   
        fastjson.serializerFeatures.WriteTabAsSpecial
        fastjson.serializerFeatures.PrettyFormat    
        fastjson.serializerFeatures.WriteClassName  
        fastjson.serializerFeatures.DisableCircularReferenceDetect      
        fastjson.serializerFeatures.WriteSlashAsSpecial     
        fastjson.serializerFeatures.BrowserCompatible       
        fastjson.serializerFeatures.WriteDateUseDateFormat      
        fastjson.serializerFeatures.NotWriteRootClassName       
        fastjson.serializerFeatures.DisableCheckSpecialChar     
        fastjson.serializerFeatures.BeanToArray     
        fastjson.serializerFeatures.WriteNonStringKeyAsString       
        fastjson.serializerFeatures.NotWriteDefaultValue        
        fastjson.serializerFeatures.BrowserSecure       
        fastjson.serializerFeatures.IgnoreNonFieldGetter        
        fastjson.serializerFeatures.WriteNonStringValueAsString     
        fastjson.serializerFeatures.IgnoreErrorGetter       
        fastjson.serializerFeatures.WriteBigDecimalAsPlain      
        fastjson.serializerFeatures.MapSortField    
    */
    public static int DEFAULT_FEATURE = -1;
    public static int getDefaultFeature(){
        if(DEFAULT_FEATURE == -1) {
            DEFAULT_FEATURE = 0;

            int features = 0;

            {
                for(SerializerFeature feature: SerializerFeature.values()){
                    String featuresProperty = Settings.getString("fastjson.serializerFeatures." + feature);

                    System.out.println("[fastjson config -]"+ feature + ":" + featuresProperty);
                    int mask = feature.getMask();
                    if ("true".equals(featuresProperty)) {
                        features |= mask;
                    } else if ("false".equals(featuresProperty)) {
                        features &= ~mask;
                    }
                }

            }

            DEFAULT_FEATURE = features;
        }

        return DEFAULT_FEATURE;
    }


    public static String jsonEncode(Object obj) {
        int defaultFeature = getDefaultFeature();
        return JSON.toJSONString(obj,  defaultFeature);
    }

}
