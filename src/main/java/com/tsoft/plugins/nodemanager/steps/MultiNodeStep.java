package com.tsoft.plugins.nodemanager.steps;

import com.google.common.collect.ImmutableSet;
import com.tsoft.plugins.nodemanager.config.NodeGroupsManagement;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;;
import org.jenkinsci.plugins.workflow.cps.CpsVmThreadOnly;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.*;

import com.tsoft.plugins.Messages;

public class MultiNodeStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;
    private String labelGroupName;

    @DataBoundConstructor
    public MultiNodeStep(String label) {
        this.labelGroupName = label;
    }

    public String getLabelGroupName() {
        return labelGroupName;
    }

    public boolean isIgnoreOffline() {
        return NodeGroupsManagement.get()
                .getJenkinsNodeGroups()
                .get(this.labelGroupName)
                .isIgnoreOffline();
    }
    @Override
    @CpsVmThreadOnly("CPS program calls this, which is run by CpsVmThread")
    public StepExecution start(StepContext stepContext) {
        return new MultiNodeStepExecution(this, stepContext);
    }

    // Overridden for better type safety.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.MultiNodeStepDisplayName();
        }

        @Override
        public String getFunctionName() {
            return Messages.MultiNodeStepFunctionName();
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(FlowNode.class, Run.class, TaskListener.class);
        }
    }

}
