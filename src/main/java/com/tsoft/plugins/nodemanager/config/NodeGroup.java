package com.tsoft.plugins.nodemanager.config;

import hudson.model.Computer;
import jenkins.model.Jenkins;
import org.apache.commons.lang.time.DateFormatUtils;

import java.util.*;

public class NodeGroup {

    private String name;
    private String user;
    private String date;
    private boolean ignoreOffline;
    private Set<String> agentsAssigned;

    public NodeGroup() {

    }

    public NodeGroup(String user, String name, Date date, boolean ignoreOffline) {
        this(user, name, DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:SS"), ignoreOffline);
    }

    public NodeGroup(String user, String name, String date, boolean ignoreOffline) {
        this.name = name;
        this.user = user;
        this.date = date;
        this.ignoreOffline = ignoreOffline;
        this.agentsAssigned = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isIgnoreOffline() {
        return ignoreOffline;
    }

    public void setIgnoreOffline(boolean ignoreOffline) {
        this.ignoreOffline = ignoreOffline;
    }

    public boolean addAgent(String agent_name){
        return this.agentsAssigned.add(agent_name);
    }

    public boolean removeAgent(String agent_name){
        if( hasAgent(agent_name) )
            return this.agentsAssigned.remove(agent_name);
        return false;
    }

    public boolean hasAgent(String agent_name){
        return this.agentsAssigned.contains(agent_name);
    }

    public Set<String> getAgentsAssigned() {
        return agentsAssigned;
    }

    public boolean agentsIsEmpty(){
        return (getAgentsAssigned().size()>0? false: true);
    }

    public NodeGroup refreshAssignedAgents(){
        List<String> agents_names = new ArrayList<>();
        for (Computer c : Jenkins.getInstanceOrNull().getComputers()){
            agents_names.add(c.getName());
        }

        getAgentsAssigned().forEach(s -> {
            if( !agents_names.contains(s) )
                removeAgent(s);
        });

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeGroup nodeGroup = (NodeGroup) o;
        return getName().equals(nodeGroup.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", user='" + user + '\'' +
                ", date='" + date + '\'' +
                ", ignoreOffline=" + ignoreOffline +
                '}';
    }
}
