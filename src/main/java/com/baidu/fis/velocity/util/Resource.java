package com.baidu.fis.velocity.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 2betop on 4/29/14.
 *
 * Fis 资源管理
 */
public class Resource {

    public static final String STYLE_PLACEHOLDER = "<!--FIS_STYLE_PLACEHOLDER-->";
    public static final String SCRIPT_PLACEHOLDER = "<!--FIS_SCRIPT_PLACEHOLDER-->";

    protected String framework = null;
    protected MapJson map = null;
    protected Map<String, Boolean> loaded;
    protected Map<String, ArrayList<String>> collection;
    protected Map<String, StringBuilder> embed;
    public int refs = 0;
    public Boolean ignorePkg = false;

    public Resource() {
        this.loaded = new HashMap<String, Boolean>();
        this.collection = new HashMap<String, ArrayList<String>>();
        this.embed = new HashMap<String, StringBuilder>();
        this.map = new MapJson();

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

            if (request.getParameter("debug") != null) {
                ignorePkg = true;
            }
        } catch (Exception err) {
            // do nothing.
            ignorePkg = Settings.getBoolean("debug", false);
        }
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public void addJS(String id) {
       if (id.contains(":") && !id.contains(":/") && !id.contains(":\\")) {
           this.addResource(id);
       } else {
           String type = "js";
           ArrayList<String> list = collection.get(type);

           if (list == null) {
               list = new ArrayList<String>();
               collection.put(type, list);
           }

           list.add(id);
       }
    }

    public void addJSEmbed(String content) {
        StringBuilder sb = embed.get("js");

        if (sb == null) {
            sb = new StringBuilder();
            embed.put("js", sb);
        }

        sb.append(content);
    }

    public void addCSS(String id) {
        if (id.contains(":") && !id.contains(":/") && !id.contains(":\\")) {
            this.addResource(id);
        } else {
            String type = "css";
            ArrayList<String> list = collection.get(type);

            if (list == null) {
                list = new ArrayList<String>();
                collection.put(type, list);
            }

            list.add(id);
        }
    }

    public void addCSSEmbed(String content) {
        StringBuilder sb = embed.get("css");

        if (sb == null) {
            sb = new StringBuilder();
            embed.put("css", sb);
        }

        sb.append(content);
    }

    public String addResource(String id){
        return this.addResource(id, false, false);
    }

    public String addResource(String id, Boolean deffer) {
        return this.addResource(id, deffer, false);
    }

    public String addResource(String id, Boolean deffer, Boolean drop) {
        JSONObject info, node;
        String uri;

        // 如果添加过了而且添加的方式也相同则不重复添加。（这里说的方式是指，同步 or 异步）
        // 如果之前是同步的这次异步添加则忽略掉。都同步添加过了，不需要异步再添加一次。
        // 注意：null 不能直接用来和 false\true 比较，否则报错。
        if ( loaded.get(id) != null && loaded.get(id) == deffer ||
                deffer && loaded.get(id) != null && !loaded.get(id) ) {
            return getUri(id, true);
        }

        info = map.getNode(id);

        if (info == null) {
            throw new IllegalArgumentException("missing resource [" + id + "]");
        }

        String pkg = (String) info.get("pkg");

        if (!ignorePkg && pkg != null) {
            info = map.getNode(pkg, "pkg");
            uri = info.getString("uri");

            if (info.containsKey("has")) {
                JSONArray has = info.getJSONArray("has");

                for (Object obj : has) {
                    loaded.put(obj.toString(), deffer);
                }
            }
        } else {
            uri = info.getString("uri");
            loaded.put(id, deffer);
        }


        try {
            // 如果有异步依赖，则添加异步依赖
            if (info.containsKey("extras")) {
                node = info.getJSONObject("extras");
                if (node.containsKey("async")) {
                    JSONArray async = node.getJSONArray("async");
                    for (Object dep : async) {
                        this.addResource(dep.toString(), true);
                    }
                }
            }

            // 如果有同步依赖，则把同步依赖也添加进来。
            if (info.containsKey("deps")) {
                JSONArray deps = info.getJSONArray("deps");
                for (Object dep : deps) {
                    this.addResource(dep.toString(), deffer);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        String type = info.get("type").toString();

        if (type.equals("js") && deffer) {
            type = "jsDeffer";

            // 如果是异步 js，用 id 代替 uri。因为还要生成依赖。
            // 注意：此处 uri 已不再是 uri。
            uri = id;
        }

        ArrayList<String> list = collection.get(type);

        if (list == null) {
            list = new ArrayList<String>();
            collection.put(type, list);
        }

        if (!drop) {
            list.add(uri);
        }

        return uri;
    }

    public String getUri(String id) {
        return getUri(id, false);
    }

    public String getUri(String id, Boolean usePkg) {
        JSONObject node = map.getNode(id);

        if (node == null) {
            return null;
        }

        if (usePkg && !ignorePkg) {
            String pkg = (String) node.get("pkg");

            if (pkg != null) {
                node = map.getNode(pkg, "pkg");
            }
        }

        return node.getString("uri");
    }

    public String renderCSS() {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> arr = collection.get("css");

        if (arr != null) {
            for (String uri : arr) {
                sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
                sb.append(uri);
                sb.append("\"/>");
            }
        }

        StringBuilder embedCSS = embed.get("css");

        if (embedCSS != null) {
            sb.append("<style type=\"text/css\">");
            sb.append(embedCSS.toString());
            sb.append("</style>");
        }

        return sb.toString();
    }

    public String renderJS() {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> arr = collection.get("js");

        Boolean needModJs = framework != null && (arr != null && !arr.isEmpty() || collection.get("jsDeffer") != null);
        String modJs = "";

        if (needModJs) {
            modJs = addResource(framework, false, true);
            sb.append("<script type=\"text/javascript\" src=\"");
            sb.append(modJs);
            sb.append("\"></script>");
        }

        if (collection.get("jsDeffer") != null) {
            Boolean useAmd = !(framework != null && framework.endsWith("mod.js"));

            if (!useAmd && Settings.getBoolean("sourceMap", true)) {
                Map<String, Map> defferMap = this.buildDefferMap();
                sb.append("<script type=\"text/javascript\">require.resourceMap(");
                sb.append(JSONObject.toJSON(defferMap));
                sb.append(");</script>");
            } else {
                // 输出 amd 方式 require.config({paths: {}});
                Map<String, String> paths = this.buildAmdPaths();
                sb.append("<script type=\"text/javascript\">require.config({paths:");
                sb.append(JSONObject.toJSON(paths));
                sb.append("});</script>");
            }
        }

        if (arr != null) {
            for (String uri : arr) {
                if (uri.equals(modJs)) {
                    continue;
                }
                sb.append("<script type=\"text/javascript\" src=\"");
                sb.append(uri);
                sb.append("\"></script>");
            }
        }

        // 输出 embed js
        StringBuilder embedJS = embed.get("js");

        if (embedJS != null) {
            sb.append("<script type=\"text/javascript\">");
            sb.append(embedJS.toString());
            sb.append("</script>");
        }

        return sb.toString();
    }

    /**
     * 生成异步JS资源表。
     * @return
     */
    protected Map<String, Map> buildDefferMap() {
        Map<String, Map> defferMap = new HashMap<String, Map>();
        Map<String, JSONObject> res = new HashMap<String, JSONObject>();
        Map<String, JSONObject> pkgMap = new HashMap<String, JSONObject>();

        ArrayList<String> list = collection.get("jsDeffer");
        JSONObject info;

        if (list != null) {

            for (String id : list) {

                // 已经同步加载，则忽略。
                if (loaded.get(id) != null && !loaded.get(id)) {
                    continue;
                }

                info = map.getNode(id);


                if (info == null) {
                    throw new IllegalArgumentException("missing resource [" + id + "]");
                }

                // 先加 res
                String pkg = info.getString("pkg");

                JSONObject infoCopy = new JSONObject();
                infoCopy.put("url", info.getString("uri"));

                // 保留 pkg 信息
                if (!ignorePkg && pkg != null) {
                    infoCopy.put("pkg", pkg);
                }

                // 过滤掉非 .js 的依赖。
                // 同时过滤掉已经同步加载的依赖。
                if (info.containsKey("deps")) {
                    JSONArray deps = info.getJSONArray("deps");
                    JSONArray depsFilter = new JSONArray();

                    for (Object dep : deps) {
                        String sDep = dep.toString();

                        if (!sDep.endsWith(".js")) {
                            continue;
                        } else if ( loaded.get(sDep) != null && !loaded.get(sDep)) {

                            // 同步中已经依赖。
                            continue;
                        }

                        depsFilter.add(sDep);
                    }

                    if (!depsFilter.isEmpty()) {
                        infoCopy.put("deps", depsFilter);
                    }
                }

                // 再把对应的 pkg 加入。
                if (!ignorePkg && pkg != null) {
                    info = map.getNode(pkg, "pkg");

                    info.put("url", info.getString("uri"));
                    info.remove("uri");

                    pkgMap.put(pkg, info);
                }

                res.put(id, infoCopy);
            }
        }

        if (!res.isEmpty()) {
            defferMap.put("res", res);
        }

        if (!pkgMap.isEmpty()) {
            defferMap.put("pkg", pkgMap);
        }

        return defferMap;
    }

    public Map<String, String> buildAmdPaths() {
        Map<String, String> paths = new HashMap<String, String>();
        JSONObject info;

        for (String id : loaded.keySet()) {
            if (loaded.get(id) == null || !loaded.get(id)) {
                continue;
            }

            // 异步依赖
            info = map.getNode(id);

            if (!info.getString("type").equals("js")) {
                continue;
            }


            if (info.containsKey("extras")) {
                String uri = info.getString("uri");

                    if (!ignorePkg && info.containsKey("pkg")) {
                        JSONObject pkg = map.getNode(info.getString("pkg"), "pkg");
                        uri = pkg.getString("uri");
                    }

                if (uri.endsWith(".js")) {
                    uri = uri.substring(0, uri.length() - 3);
                }

                if (info.getJSONObject("extras").containsKey("moduleId")) {
                    paths.put(info.getJSONObject("extras").getString("moduleId"), uri);
                }
            }

        }

        return paths;
    }

    public String filterContent(String input) {

        if (input.contains(Resource.SCRIPT_PLACEHOLDER)) {
            input = input.replace(Resource.SCRIPT_PLACEHOLDER, renderJS());
        }

        if (input.contains(Resource.STYLE_PLACEHOLDER)) {
            input = input.replace(Resource.STYLE_PLACEHOLDER, renderCSS());
        }

        return input;
    }

}
