package com.tsoft.plugins.nodemanager.utils;


import com.google.common.collect.ImmutableSet;
import com.tsoft.plugins.nodemanager.config.NodeGroupsManagement;
import com.tsoft.plugins.nodemanager.steps.GroupCauseOfInterruption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class MultiNodeUtilsStep extends Step {

    private String label;

    @DataBoundConstructor
    public MultiNodeUtilsStep(String label) {
        this.label = label;
    }


    public boolean isIgnoreOffline() {
        return NodeGroupsManagement.get()
                .getJenkinsNodeGroups()
                .get(this.label)
                .isIgnoreOffline();
    }

    public String getLabel() {
        return label;
    }

    public Set<String> getNodeNamesFromLabel(){
        return NodeGroupsManagement.get()
                .getJenkinsNodeGroups()
                .get(this.label)
                .getAgentsAssigned();
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new GroupManagerStepExecution(this, stepContext);
    }


    @SuppressFBWarnings(value="SE_TRANSIENT_FIELD_NOT_RESTORED", justification="Only used when starting.")
    private static class GroupManagerStepExecution extends SynchronousNonBlockingStepExecution<Set<String>>
    {

        private transient MultiNodeUtilsStep step;

        protected GroupManagerStepExecution(MultiNodeUtilsStep step, @Nonnull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Set<String> run() throws Exception {
            Jenkins instance = Jenkins.getInstanceOrNull();
            Set<String> nodes_online = new HashSet<>();
            Set<String> nodes_offline = new HashSet<>();
            boolean isIgnoreOffline = step.isIgnoreOffline();

            if( instance!=null ) {

                step.getNodeNamesFromLabel()
                        .forEach(s -> {
                            if (instance.getNode(s).toComputer().isOnline()) {
                                nodes_online.add(s);
                            } else {
                                nodes_offline.add(s);
                            }
                        });

                getContext().get(TaskListener.class).getLogger()
                        .printf("Buscando nodos en el grupo: '%s'\n", step.getLabel())
                        .printf("[ ... ] Agents online: %s\n", nodes_online.toString())
                        .printf("[ ... ] Agents offline: %s\n", nodes_offline.toString())
                        .printf("[ ... ] Ignore nodes Offline? : %s\n", isIgnoreOffline)
                        .println();
            }

            if(!isIgnoreOffline && nodes_offline.size()>0)
                throw new FlowInterruptedException(Result.ABORTED, true, new GroupCauseOfInterruption(step.getLabel(), nodes_offline, 1));

            return nodes_online;
        }

    }


    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        /** {@inheritDoc} */
        @Override
        public String getDisplayName()
        {
            return "Busqueda de nodos por label";
        }

        @Override
        public String getFunctionName()
        {
            return "nodeutils";
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }
    }
}
