package com.tsoft.plugins.nodemanager.steps;

import jenkins.model.CauseOfInterruption;
import java.util.Set;

public class GroupCauseOfInterruption extends CauseOfInterruption {
    private String label;
    private Set<String> nodos_offline;
    private int motivo_number = 0;

    public GroupCauseOfInterruption(String label, Set<String> nodos, int motivo){
        this.label = label;
        this.nodos_offline = nodos;
        this.motivo_number = motivo;
    }

    @Override
    public String getShortDescription() {
        StringBuilder message = new StringBuilder();
        switch (this.motivo_number) {
            case 0:
                message.append("No se encuentraron nodos online en el grupo: ")
                        .append(label);

                break;
            case 1:
                message.append("Se interrumpe la ejecucion del pipeline debido a que los siguientes nodos del grupo: ")
                        .append(label)
                        .append(" se encuentran offline: ")
                        .append(nodos_offline.toString());
                break;
            case 2:
                message.append("The function's body should not be empty!");
                break;
            default:
                message.append("No es posible realizar la operacion por un error desconocido.");
        }
        return message.toString();
    }
}