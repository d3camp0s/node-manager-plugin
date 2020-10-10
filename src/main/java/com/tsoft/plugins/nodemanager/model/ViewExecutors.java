package com.tsoft.plugins.nodemanager.model;

import com.tsoft.plugins.nodemanager.config.NodeGroupsManagement;
import hudson.model.Computer;
import jenkins.model.Jenkins;

import java.util.*;

public class ViewExecutors {

    public ViewExecutors() {}

    public static HashMap<String, Set<Computer> > getGroups() {

        HashMap<String, Set<Computer>> grupos = new HashMap<>();
        Set<Computer> computers = new HashSet(Arrays.asList(Jenkins.getInstanceOrNull().getComputers()));
        grupos.put("principal", computers);

        // Instancia singleton de las configuraciones de grupos de nodos
        NodeGroupsManagement.get().getJenkinsNodeGroups().forEach((name, nodeGroup) -> {
            String n = name.split("-")[0];
            if( grupos.get(n) == null )
                grupos.put(n, _getComputers(nodeGroup.getAgentsAssigned()));
            else
                grupos.get(n).addAll(_getComputers(nodeGroup.getAgentsAssigned()));
            // remove all assigned agents
            computers.removeAll(grupos.get(n));
        });

        return grupos;
    }

    private static Set<Computer> _getComputers(Set<String> agents_names){
        Set<Computer> agents = new HashSet<>();
        agents_names.forEach(name -> {
            agents.add(Jenkins.getInstanceOrNull().getComputer(name));
        });
        return agents;
    }



}
