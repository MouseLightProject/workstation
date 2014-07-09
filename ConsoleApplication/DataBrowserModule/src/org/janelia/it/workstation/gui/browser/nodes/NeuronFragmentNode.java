package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class NeuronFragmentNode extends DomainObjectNode {
    
    public NeuronFragmentNode(ChildFactory parentChildFactory, NeuronFragment neuronFragment) throws Exception {
        super(parentChildFactory, Children.LEAF, neuronFragment);
    }
    
    private NeuronFragment getNeuronFragment() {
        return (NeuronFragment)getDomainObject();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("brick.png").getImage();
    }
    
    @Override
    public String getPrimaryLabel() {
        return "Neuron Fragment "+getNeuronFragment().getNumber();
    }
}
