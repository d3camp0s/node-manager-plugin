package com.tsoft.plugins.nodemanager.config;

import com.tsoft.plugins.nodemanager.utils.JenkinsUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.*;
import hudson.model.listeners.SaveableListener;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.acegisecurity.AccessDeniedException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.tsoft.plugins.Messages;

@Extension
public class NodeGroupsManagement extends ManagementLink implements StaplerProxy, Describable<NodeGroupsManagement>, Saveable {

    private ConcurrentHashMap<String, NodeGroup> jenkinsNodeGroups = new ConcurrentHashMap<>();
    private static final Logger log = Logger.getLogger(NodeGroupsManagement.class.getName());

    public static final String PLUGIN_URL = "/plugin/node-manager";
    public static final String PLUGIN_IMAGES_URL = PLUGIN_URL + "/images";
    private int i=0;

    public NodeGroupsManagement() throws IOException {
        load();
    }

    @Override
    public Descriptor<NodeGroupsManagement> getDescriptor() {
        return Jenkins.get().getDescriptorByType(DescriptorImpl.class);
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return PLUGIN_IMAGES_URL + "/48x48/nodes_management.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return Messages.PluginDisplayName();
    }

    @Override
    public String getDescription() {
        return Messages.PluginDescription();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "node-groups-management";
    }

    @NonNull
    @Override
    public Category getCategory() {
        return Category.TOOLS;
    }

    @Override
    public synchronized void save() throws IOException {
        if (!BulkChange.contains(this)) {
            try {
                this.getConfigFile().write(this);
                SaveableListener.fireOnChange(this, this.getConfigFile());
            } catch (IOException var2) {
                log.warning("Failed to save " + this.getConfigFile());
            }

        }
    }

    public synchronized void load() throws IOException {
        XmlFile file = this.getConfigFile();
        if (file.exists()) {
            try {
                file.unmarshal(this);
            } catch (IOException var3) {
                log.warning("Failed to load " + file);
            }

        }
    }

    protected XmlFile getConfigFile() {
        return new XmlFile(new File(Jenkins.get().getRootDir(), this.getId() + ".xml"));
    }

    @Override
    public Object getTarget() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        return this;
    }

    public static NodeGroupsManagement get() {
        return ManagementLink.all().get(NodeGroupsManagement.class);
    }


    /**
     * Descriptor is only used for UI form bindings.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<NodeGroupsManagement> {
        @Override
        public String getDisplayName() {
            return null; // unused
        }

        @RequirePOST
        @Restricted(NoExternalUse.class)
        public void doDeleteLabel(StaplerRequest req, StaplerResponse rsp) throws IOException {
            synchronized (this) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                req.setCharacterEncoding("UTF-8");
                try {
                    String value = req.getParameter("value");
                    if (value != null) {
                        if (canDeleteLabel(value)) {
                            NodeGroupsManagement.get()
                                    .getJenkinsNodeGroups()
                                    .remove(value);

                            NodeGroupsManagement.get().save();

                            rsp.setStatus(200);
                            rsp.getWriter().println("deletion completed successfully!");
                        } else {
                            rsp.setStatus(500);
                            rsp.getWriter().println("No es posible eliminar el label " + value + "<br>");
                            rsp.getWriter().println("ya que aun posee agentes asignados:<br>");
                            rsp.getWriter().println(NodeGroupsManagement.get()
                                    .getJenkinsNodeGroups()
                                    .get(value)
                                    .getAgentsAssigned());
                        }
                    } else {
                        rsp.setStatus(404);
                        rsp.getWriter().println("Label can not be empty!");
                    }
                } catch (Exception e) {
                    rsp.setStatus(500);
                    rsp.getWriter().println(e.toString());
                }
            }
        }

        private boolean canDeleteLabel(String label){
            return NodeGroupsManagement.get()
                    .getJenkinsNodeGroups()
                    .get(label)
                    .refreshAssignedAgents()
                    .agentsIsEmpty();
        }

    }

    /**
     * Called on roles management form submission.
     */
    @RequirePOST
    public void doGroupsSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        synchronized (this) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            // Let the strategy descriptor handle the form
            JSONObject json = req.getSubmittedForm();
            JenkinsUtils.copyOrUpdateGroups(json, jenkinsNodeGroups);
            save();
            // Redirect to the plugin index page
            FormApply.success(".").generateResponse(req, rsp, this);
        }
    }

    @RequirePOST
    public void doAsignarAgenteToLabel(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        synchronized (this) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            // Let the strategy descriptor handle the form
            JSONObject json = req.getSubmittedForm();
            JenkinsUtils.copyOrUpdateAssignedAgents(json, jenkinsNodeGroups);
            save();
            // Redirect to the plugin index page
            FormApply.success(".").generateResponse(req, rsp, this);
        }
    }

    @RequirePOST
    public FormValidation doAdd(@QueryParameter String label, @QueryParameter boolean ignoreOffline) {
        try {
            synchronized (this) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                User user = Jenkins.get().getMe();
                if (Util.fixEmptyAndTrim(label) == null) {
                    return FormValidation.error("value can not be empty");
                } else {
                    if (!jenkinsNodeGroups.keySet().contains(label)) {
                        NodeGroup newGroup = new NodeGroup(user.getId(), label, new Date(), ignoreOffline);
                        jenkinsNodeGroups.put(label, newGroup);
                        save();
                        return FormValidation.ok("Grupo creado con exito, label: " + label);
                    } else {
                        return FormValidation.error("Ya existe un grupo con la etiqueta: " + label);
                    }
                }
            }
        }
        catch(AccessDeniedException | IOException ex){
            return FormValidation.warning("User is not available when not logged in");
        }
    }

    public String getId() {
        return this.getClass().getName();
    }


    public Map<String,NodeGroup> getFilteredNodeGroups() {
        String filter = Stapler.getCurrentRequest().getParameter("filter");
        if( filter==null || filter.equals("") )
            return jenkinsNodeGroups;

        return jenkinsNodeGroups.entrySet()
                .stream()
                .filter(node -> node.getKey().contains(filter))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<Computer> getFilteredAgents(){
        String filter = Stapler.getCurrentRequest().getParameter("filter");
        List<Computer> agents = JenkinsUtils.getAllAgents();
        if( filter!=null ) {
            agents = JenkinsUtils.getAllAgents()
                    .stream()
                    .filter(computer -> computer.getName().contains(filter))
                    .collect(Collectors.toList());
        }

        return agents;
    }

    public int countComputers(){
        return JenkinsUtils.countComputers();
    }

    public void setJenkinsNodeGroups(ConcurrentHashMap<String,NodeGroup> jenkinsNodeGroups) {
        this.jenkinsNodeGroups = jenkinsNodeGroups;
    }

    public Map<String,NodeGroup> getJenkinsNodeGroups() {
        return jenkinsNodeGroups;
    }

}
