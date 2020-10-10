package com.tsoft.plugins.nodemanager.utils;

import com.tsoft.plugins.nodemanager.config.NodeGroup;
import hudson.Util;
import hudson.model.Computer;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Utilities to fetch things out of jenkins environment.
 */
public class JenkinsUtils {
    private static final Logger log = LoggerFactory.getLogger(JenkinsUtils.class);
    private static String _id;

    private JenkinsUtils(){}

    public static List<Computer> getAllAgents() {
        Computer[] agents = Jenkins.get().getComputers();
        return Arrays.asList(agents)
                .stream()
                .filter(computer -> Util.fixEmptyAndTrim(computer.getName())!=null)
                .collect(Collectors.toList());
    }

    public static int countComputers() {
        return getAllAgents().size();
    }

    public static void copyOrUpdateGroups(JSONObject json, Map<String, NodeGroup> jenkinsNodeGroups) {
        JSONObject data = json.getJSONObject("data");
        if(data!=null){
            // recorremos la lista de nodos
            data.forEach((key, object) -> {
                key = key.trim();
                log.debug("Procesando label: "+key);
                Map group = (Map) object;

                NodeGroup ng;
                if(jenkinsNodeGroups.keySet().contains(key))
                    ng = jenkinsNodeGroups.get(key);
                else
                    ng = new NodeGroup();

                ng.setUser((String) group.get("user"));
                ng.setName((String) group.get("label"));
                ng.setDate((String) group.get("date"));
                ng.setIgnoreOffline((boolean) group.get("ignoreOffline"));

                jenkinsNodeGroups.put(key,ng);
                log.debug(ng.toString());
            });
        }
        else{
            log.error("No se puede guardar data null en la lista de grupos");
        }
    }

    public static void copyOrUpdateAssignedAgents(JSONObject json, Map<String, NodeGroup> jenkinsNodeGroups) {
        JSONObject data = json.getJSONObject("data");
        if(data!=null) {
            data.forEach((key, object) -> {
                // Por cada label se actualizan los nodos activos
                log.debug("Procesando label: "+key);
                NodeGroup ng = jenkinsNodeGroups.get(key);
                Map<String, Boolean> agents = (Map<String, Boolean>) object;
                agents.forEach((k, v) -> {
                    boolean r = v? ng.addAgent(k) : ng.removeAgent(k);
                    log.debug(k +" was added: "+r);
                });
                jenkinsNodeGroups.put(key,ng);
                log.debug(key +" : "+object.toString());
            });
        }
    }
}
