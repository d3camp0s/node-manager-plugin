package com.tsoft.plugins.nodemanager.steps;

import com.tsoft.plugins.nodemanager.config.NodeGroupsManagement;
import hudson.FilePath;
import hudson.model.*;
import hudson.slaves.DumbSlave;
import jenkins.model.CauseOfInterruption;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.cps.CpsStepContext;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn;
import org.jenkinsci.plugins.workflow.steps.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import static org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext.FLOW_NODE;


class MultiNodeStepExecution extends StepExecution {

    private static final long serialVersionUID = 1L;
    private transient MultiNodeStep multinodeStep;
    private final List<BodyExecution> bodies = new ArrayList<>();

    public MultiNodeStepExecution(MultiNodeStep multinodeStep, StepContext context) {
        super(context);
        this.multinodeStep = multinodeStep;
    }

    @Override
    public boolean start() throws Exception {
        CpsStepContext cps = (CpsStepContext) getContext();
        Run<?,?> run = getContext().get(Run.class);
        Job<?,?> job = run.getParent();
        String jobName = job.getFullName();
        String buildUrl = job.getUrl();

        if (!cps.hasBody()) {
            cps.get(TaskListener.class).getLogger().println("[ ... ] The function's body should not be empty.... pls check and continue");

            throw new FlowInterruptedException(Result.UNSTABLE,
                    new GroupCauseOfInterruption(multinodeStep.getLabelGroupName(), Collections.EMPTY_SET, 2));
        }

        // Iteracion sobre los nodos del grupo asociado al label indicado en la creacion del step
        nodesByLabel(multinodeStep.getLabelGroupName()).forEach( nodeName -> {
            DumbSlave node = (DumbSlave)Jenkins.getInstanceOrNull().getNode(nodeName);
            Computer computer = node.toComputer();

            // Creamos el workspace en el nodo de destino
            FilePath ws = null;
            try {
                ws = node.createPath(node.getRemoteFS() + "/workspace" + "/" + jobName +"/"+ job.getLastBuild().getNumber());
            } catch (Exception e) {
                e.printStackTrace();
            }
            BodyExecution exec = cps.newBodyInvoker()
                    .withDisplayName(new MultiNodeLabelAction(nodeName).getDisplayName())
                    .withCallback(BodyExecutionCallback.wrap(cps))
                    .withContexts(computer, ws)
                    .start(); // when the body is done, the step is done

            /*
            CpsThread t = CpsThread.current();
            BodyExecution exec = cps.newBodyInvoker(t.getGroup().export(multinodeStep.closure), true)
                    .withDisplayName(new MultiNodeLabelAction(nodeName).getDisplayName())
                    .withCallback(BodyExecutionCallback.wrap(cps))
                    .withContexts(computer, ws)
                    .start(); // when the body is done, the step is done
            */


            bodies.add(exec);
        });

        return false;

    }

    @Override
    public void stop(Throwable cause) throws Exception {
        for (BodyExecution body : bodies) {
            body.cancel(cause);
        }
    }

    void stop(CauseOfInterruption... causes) {
        for (BodyExecution body : bodies) {
            body.cancel(causes);
        }
    }

    private Set<String> nodesByLabel(String label) throws IOException, InterruptedException {
        Set<String> all_nodes = NodeGroupsManagement.get()
                .getJenkinsNodeGroups()
                .get(label)
                .getAgentsAssigned();

        Jenkins instance = Jenkins.getInstanceOrNull();
        Set<String> nodes_online = new HashSet<>();
        Set<String> nodes_offline = new HashSet<>();
        boolean isIgnoreOffline = multinodeStep.isIgnoreOffline();

        if( instance!=null ) {

            all_nodes.forEach(s -> {
                if (instance.getNode(s).toComputer().isOnline()) {
                    nodes_online.add(s);
                } else {
                    nodes_offline.add(s);
                }
            });

            getContext().get(TaskListener.class).getLogger()
                    .printf("Buscando nodos en el grupo: '%s'\n", multinodeStep.getLabelGroupName())
                    .printf("[ ... ] Agents online: %s\n", nodes_online.toString())
                    .printf("[ ... ] Agents offline: %s\n", nodes_offline.toString())
                    .printf("[ ... ] Ignore nodes Offline? : %s\n", isIgnoreOffline)
                    .println();
        }

        // Verificamos si existen nodos en online y/o offline
        if( nodes_online.size() == 0 ){
            throw new FlowInterruptedException(Result.FAILURE, true,
                    new GroupCauseOfInterruption(multinodeStep.getLabelGroupName(), nodes_offline, 0));
        }
        else if(!isIgnoreOffline && nodes_offline.size()>0) {
            throw new FlowInterruptedException(Result.ABORTED, true,
                    new GroupCauseOfInterruption(multinodeStep.getLabelGroupName(), nodes_offline, 1));
        }

        return nodes_online;
    }


    @PersistIn(FLOW_NODE)
    private static class MultiNodeLabelAction extends LabelAction implements ThreadNameAction {
        private final String nodehName;

        MultiNodeLabelAction(String branchName) {
            super(null);
            this.nodehName = branchName;
        }

        @Override
        public String getDisplayName() {
            return "MultiNode: " + nodehName;
        }

        @Nonnull
        @Override
        public String getThreadName() {
            return nodehName;
        }
    }
}