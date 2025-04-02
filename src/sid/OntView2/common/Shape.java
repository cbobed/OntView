
package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import sid.OntView2.selectionStrategy.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Shape {

    public static final int CLOSED = 0;
    public static final int OPEN = 1;
    public static final int PARTIALLY_CLOSED = 2;
    public static final int LEFTCLOSED = 3;
    public static final int LEFTOPEN = 4;
    public static final int LEFT_PARTIALLY_CLOSED = 5;

    public int posx, posy;
    private int height, width;
    public int depthlevel;
    VisLevel vdepthlevel;
    VisGraph graph;
    Point2D connectionPointsL;
    Point2D connectionPointsR;
    ArrayList<VisConnector> inConnectors,
        outConnectors,
        inDashedConnectors,
        outDashedConnectors;

    //when showing I need to keep track of those that were closed

    int state = OPEN;
    int leftState = LEFTOPEN;
    boolean visible = true;

    private double indicatorSize = -1;

    public double getIndicatorSize() {
        return indicatorSize;
    }

    public void setIndicatorSize(double indicatorSize) {
        this.indicatorSize = indicatorSize;
    }

    public abstract String getLabel();

    private int getZoomLevel() {
        return 1;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean b) {
        visible = b;
    }

    public int getPosX() {
        return posx;
    }

    public int getPosY() {
        return posy;
    }

    public int getHeight() {
        return height * getZoomLevel();
    }

    public int getTotalHeight() {
        return height * getZoomLevel();
    }

    public int getWidth() {
        return width;
    }

    public void setPosX(int x) {
        posx = x;
    }

    public void setPosY(int y) {
        posy = y;
    }

    public void setHeight(int value) {
        height = value;
    }

    public void setWidth(int x) {
        width = x;
    }

    public int getTopCorner() {
        return posy - getHeight() / 2;
    }

    public int getLeftCorner() {
        return posx - getWidth() / 2;
    }

    public int getRightCorner() {
        return posx + getWidth() / 2;
    }

    public int getBottomShapeCorner() {
        return posy + getHeight() / 2;
    }


    public int getBottomCorner() {
        VisClass v_i = this.asVisClass();
        if (v_i.getPropertyBox() != null && v_i.getPropertyBox().visible) {
            return posy + getHeight() / 2 + v_i.getPropertyBox().getHeight();
        }
        return posy + getHeight() / 2;
    }

    public VisClass asVisClass() {
        return (VisClass) this;
    }

    public void setState(int pState) {
        state = pState;
    }

    public int getState() {
        return state;
    }

    public void setLeftState(int pState) {
        leftState = pState;
    }

    public int getLeftState() {
        return leftState;
    }

    public abstract OWLClassExpression getLinkedClassExpression();

    public abstract String getToolTipInfo();

    public abstract int getLevelRelativePos();

    public abstract void drawShape(GraphicsContext g);

    public abstract Point2D getConnectionPoint(Point2D point, boolean b);

    private final Set<Shape> hiddenDescendantsSet = new HashSet<>();
    private final Set<Shape> visibleDescendantsSet = new HashSet<>();
    private final Set<Shape> hiddenParentsSet = new HashSet<>();

    /**************************************************************/
    public Shape() {
        inConnectors = new ArrayList<>();
        outConnectors = new ArrayList<>();
        outDashedConnectors = new ArrayList<>();
        inDashedConnectors = new ArrayList<>();
    }

    /**
     * Marks as closed and hides subLevels
     * Then looks for those remaining visible nodes and adds a reference (dashed line)
     */
    public void closeRight() {
        showWarning();
        hideSubLevels(getShapesFromStrategySteps(true, graph.paintframe.getPercentageShown()));
    }

    public void closeLeft() {
        setLeftState(LEFTCLOSED);
        hideParents(this, hiddenParentsSet);
    }

    public void resetHiddenChildrenCount() {
        hiddenDescendantsSet.clear();
    }

    public void resetHiddenParentsCount() {
        hiddenParentsSet.clear();
    }

    public int getHiddenDescendantsSet() {
        return hiddenDescendantsSet.size();
    }

    public int getHiddenParentsSet() {
        return hiddenParentsSet.size();
    }

    /**
     * Check if children has other visible parents
     */
    boolean childHasOtherParents() {
        int visibleParentCount = 0;

        for (VisConnector inConnector : this.inConnectors) {
            if (inConnector.isVisible()) {
                visibleParentCount++;
            }
        }

        return visibleParentCount > 1;
    }

    /**
     * Checks if the shape has other visible children besides the specified child.
     *
     * @param excludingChild The child to exclude from the visibility check.
     * @return true if there are other visible children, false otherwise.
     */
    private boolean hasOtherVisibleChildren(Shape excludingChild) {
        for (VisConnector connector : outConnectors) {
            if (connector.to != excludingChild && connector.isVisible()) {
                return true;
            }
        }
        return false;
    }


    /**
     * hides outConnectors and checks if children need to be hidden
     */
    protected void hideSubLevels(Set<Shape> hiddenDescendants) {
        this.hideDescendants(hiddenDescendants);

        Set<Shape> visited = new HashSet<>();
        for (Shape s: hiddenDescendants) {
            s.addHiddenDescendantsToAncestors(hiddenDescendants, visited);
        }
        hiddenDescendantsSet.addAll(hiddenDescendants);
        checkAndUpdateChildrenVisibilityStates();

    }

    /**
     * hides inConnectors and checks if parents need to be hidden
     */
    private void hideParents(Shape closedShape, Set<Shape> countedParents) {
        Shape parent;
        for (VisConnector connector : inConnectors) {
            parent = connector.from;
            countedParents.clear();

            if (parent.getLabel().matches("Thing")) {
                parent.checkAndUpdateChildrenVisibilityStates();
                //parent.updateHiddenDescendants();
                break;
            }

            if (parent.hasOtherVisibleChildren(this)) {
                connector.hide();
                parent.setState(PARTIALLY_CLOSED);
                parent.updateHiddenDescendants(); // update hidden descendants for the parent
                parent.updateHiddenDescendantsForParents(); // update hidden descendants for the parent's parents
                continue;
            }
            connector.hide();

            if (countedParents.add(parent)) {
                parent.checkAndHideParents(closedShape, countedParents);
            }
        }
        countedParents.clear();

    }

    /**
     * Checks references
     * Before setting invisible a shape we need to check if there's still
     * any reference ( an out Connector)
     */
    public void checkAndHideParents(Shape closedShape, Set<Shape> countedParents) {
        if (getVisibleOutReferences() == 0) {
            this.visible = false;
            //countedParents.add(this);
            hideParents(closedShape, countedParents);
        }
        if (!isVisible()){ // hide connectors once all nodes are hidden
            for (VisConnector connector : inConnectors) {
                connector.hide();
            }
        }
    }

    /**
     * hides shape, connector and notifies parents
     */
    public void hideDescendants(Set<Shape> hiddenDescendantsSet) {
        if (hiddenDescendantsSet.contains(this)) {
            this.visible = false;
            for (VisConnector c : inConnectors) {
                c.hide();
                c.from.checkAndUpdateChildrenVisibilityStates();
            }
            for (VisConnector c : outConnectors) {
                c.hide();
                c.to.checkAndUpdateParentVisibilityStates();
            }
        }

        for (VisConnector connector : outConnectors) {
            connector.to.hideDescendants(hiddenDescendantsSet);
        }
    }

    public void hide() {
        this.visible = false;
        for (VisConnector c : inConnectors) {
            c.hide();
            c.from.hiddenDescendantsSet.add(this);
            c.from.checkAndUpdateChildrenVisibilityStates();
        }

        for (VisConnector c : outConnectors) {
            c.hide();
            c.to.checkAndUpdateParentVisibilityStates();
        }

        // Wake observer thread on hide event
        graph.updateObservers(VisConstants.GENERALOBSERVER);
        graph.getDashedConnectorList().clear();
        graph.addDashedConnectors();
    }

    private int getVisibleInReferences() {
        int count = 0;
        for (VisConnector c : inConnectors) {
            if (c.visible) {
                count++;
            }
        }
        return count;
    }

    private int getVisibleOutReferences() {
        int count = 0;
        for (VisConnector c : outConnectors) {
            if (c.visible) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return all subclasses hidden or not
     */
    public boolean allSubHidden() {
        //if all subclasses are hidden
        for (VisConnector c : this.outConnectors) {
            if (c.visible)
                return false;
        }
        return true;
    }

    private void showWarning() {
        if (graph.paintframe.getPercentageShown() == 0) {
            graph.paintframe.showAlertDialog("Warning",
                "Please note that the visibility percentage for showing \nor hiding nodes is currently set to 0.",
                "Consider selecting a different percentage to adjust the visibility appropriately.",
                Alert.AlertType.INFORMATION);
        }
    }

    public Set<Shape> getShapesFromStrategySteps(boolean toHide, int limit){
        switch (graph.paintframe.getStrategyOptionStep()) {
            case VisConstants.STEPSTRATEGY_RDFRANK -> {
                RDFRankSelectionStrategySteps RDFStrategy = new RDFRankSelectionStrategySteps(limit,
                    asVisClass().orderedDescendants);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            case VisConstants.STEPSTRATEGY_RDF_LEVEL_LR -> {
                RDFRankSelectionStrategyStepsLR RDFStrategy = new RDFRankSelectionStrategyStepsLR(limit,
                    asVisClass().orderedDescendantsByLevel);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            case VisConstants.STEPSTRATEGY_RDF_LEVEL_RL -> {
                RDFRankSelectionStrategyStepsRL RDFStrategy = new RDFRankSelectionStrategyStepsRL(limit,
                    asVisClass().orderedDescendantsByLevel);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            default -> {
                return null;
            }
        }
    }

    public Set<Shape> getShapesFromStrategy(boolean toHide, int limit){
        switch (graph.paintframe.getStrategyOptionGlobal()) {
            case VisConstants.GLOBALSTRATEGY_RDFRANK -> {
                RDFRankSelectionStrategyGlobal RDFStrategy = new RDFRankSelectionStrategyGlobal(limit,
                    asVisClass().orderedDescendants);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            case VisConstants.STEPSTRATEGY_RDF_LEVEL_LR -> {
                RDFRankSelectionStrategyGlobalLR RDFStrategy = new RDFRankSelectionStrategyGlobalLR(limit,
                    asVisClass().orderedDescendantsByLevel);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            case VisConstants.STEPSTRATEGY_RDF_LEVEL_RL -> {
                RDFRankSelectionStrategyGlobalRL RDFStrategy = new RDFRankSelectionStrategyGlobalRL(limit,
                    asVisClass().orderedDescendantsByLevel);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            default -> {
                return null;
            }
        }
    }

    /**************************************************************/

    public void openRight() {
        showWarning();
        showSubLevels(getShapesFromStrategySteps(false, graph.paintframe.getPercentageShown()));
    }

    public void openLeft() {
        setLeftState(LEFTOPEN);
        showParentLevels();
    }

    public void show(Set<Shape> visibleDescendantsSet) {
        if (visibleDescendantsSet.contains(this)) {
            this.visible = true;

            for (VisConnector c : inConnectors) {
                if (c.from.isVisible() && c instanceof VisConnectorIsA) c.show();
                else if (!c.from.isVisible() && c instanceof VisConnectorDashed) c.show();

                c.from.checkAndUpdateChildrenVisibilityStates();
            }
            for (VisConnector c : outConnectors) {
                if (c.to.isVisible() && c instanceof VisConnectorIsA) c.show();
                else if (!c.to.isVisible() && c instanceof VisConnectorDashed) c.show();

                c.to.checkAndUpdateParentVisibilityStates();
            }
        }

        for (VisConnector connector : outConnectors) {
            connector.to.show(visibleDescendantsSet);
        }
    }

    public void showLeft(Shape parent, Set<Shape> visibleDescendantsSet) {
        if (!this.isVisible()) visibleDescendantsSet.add(this);
        this.visible = true;

        switch (getLeftState()) {
            case LEFTCLOSED:
                break;

            case LEFTOPEN, LEFT_PARTIALLY_CLOSED:
                for (VisConnector connector : inConnectors) {
                    connector.show();
                    connector.from.showLeft(this, visibleDescendantsSet);
                    connector.from.checkAndUpdateChildrenVisibilityStates();
                    connector.from.checkAndUpdateParentVisibilityStates();
                }
                break;
        }
    }

    public void showSubLevels(Set<Shape> visibleDescendants) {
        this.show(visibleDescendants);

        Set<Shape> visited = new HashSet<>();
        for (Shape s: visibleDescendants){
            //s.checkAndUpdateChildrenVisibilityStates();
            //s.updateHiddenDescendants();
            s.removeHiddenDescendantsFromAncestors(visibleDescendants, visited);
        }
        hiddenDescendantsSet.removeAll(visibleDescendants);
        checkAndUpdateChildrenVisibilityStates();
    }

    public void showParentLevels() {
        for (VisConnector c : inConnectors) {
            c.from.showLeft(this, visibleDescendantsSet);
            c.show();
            c.from.checkAndUpdateChildrenVisibilityStates();
            c.from.checkAndUpdateParentVisibilityStates();
        }
        for (Shape s: visibleDescendantsSet){
            s.updateParentsShowLeft(visibleDescendantsSet);
        }
    }

    public void setVisLevel(VisLevel v) {
        v.addShape(this);
        if (v.width < width)
            v.width = width + VisLevel.MIN_WIDTH;
    }

    public VisLevel getVisLevel() {
        return vdepthlevel;
    }

    /**
     * Inverts lookup in the shapeMap by returning the key out of an OWLClassExpression
     */
    public static String getKey(OWLClassExpression e) {
        if (e instanceof OWLClass) {
            return e.asOWLClass().getIRI().toString();
        } else {
            return e.toString();
        }
    }

    public static final Comparator<Shape> POSY_ORDER =
        new Comparator<Shape>() {
            public int compare(Shape s1, Shape s2) {
                return s1.getTopCorner() - s2.getTopCorner();
            }
        };

    /**
     * Updates the state of the shape based on the visibility of its parents.
     */
    public void checkAndUpdateParentVisibilityStates() {
        // Check parents visibility
        boolean allParentsHidden = true;
        boolean allParentsVisible = true;

        for (VisConnector connector : inConnectors) {
            if (connector.isVisible()) {
                allParentsHidden = false;
            } else {
                allParentsVisible = false;
            }

            if (!allParentsHidden && !allParentsVisible) {
                break;
            }
        }

        if (allParentsHidden) {
            setLeftState(LEFTCLOSED);
        } else if (allParentsVisible) {
            setLeftState(LEFTOPEN);
        } else {
            setLeftState(LEFT_PARTIALLY_CLOSED);
        }
    }

    /**
     * Checks and updates the visibility state of the shape based on the visibility of its descendants.
     */
    public void checkAndUpdateChildrenVisibilityStates() {
        Set<Shape> visitedNodes = new HashSet<>();
        AtomicBoolean hasVisibleDescendants = new AtomicBoolean(false);
        AtomicBoolean hasHiddenDescendants = new AtomicBoolean(false);

        traverseAndCheckVisibility(this, visitedNodes, hasVisibleDescendants, hasHiddenDescendants);

        if (hasVisibleDescendants.get() && hasHiddenDescendants.get()) {
            setState(PARTIALLY_CLOSED);
        } else if (hasVisibleDescendants.get()) {
            setState(OPEN);
        } else {
            setState(CLOSED);
        }
    }

    private void traverseAndCheckVisibility(Shape currentNode, Set<Shape> visitedNodes,
                                            AtomicBoolean hasVisibleDescendants, AtomicBoolean hasHiddenDescendants) {
        if (visitedNodes.contains(currentNode)) {
            return;
        }
        visitedNodes.add(currentNode);

        for (VisConnector outConnector : currentNode.outConnectors) {
            Shape childNode = outConnector.to;

            if (childNode.isVisible()) {
                hasVisibleDescendants.set(true);
            } else {
                hasHiddenDescendants.set(true);
            }

            if (hasVisibleDescendants.get() && hasHiddenDescendants.get()) {
                return;
            }

            traverseAndCheckVisibility(childNode, visitedNodes, hasVisibleDescendants, hasHiddenDescendants);
        }
    }

    /**
     * Recursively traverses all ancestors of the given node and removes the nodes
     * from visibleDescendantsSet in the hiddenDescendantsSet of each ancestor.
     */
    private void removeHiddenDescendantsFromAncestors(Set<Shape> visibleDescendantsSet, Set<Shape> visited) {
        if (visited.contains(this)) return;
        visited.add(this);

        for (VisConnector connector : inConnectors) {
            Shape parent = connector.from;
            if (parent != null) {
                parent.hiddenDescendantsSet.removeAll(visibleDescendantsSet);
                //parent.checkAndUpdateChildrenVisibilityStates();
                //parent.checkAndUpdateParentVisibilityStates();
                parent.removeHiddenDescendantsFromAncestors(visibleDescendantsSet, visited);
            }
        }
    }

    /**
     * Recursively traverses all ancestors of the given node and adds the nodes
     * from visibleDescendantsSet in the hiddenDescendantsSet of each ancestor,
     * only if the node is its descendant.
     */
    private void addHiddenDescendantsToAncestors(Set<Shape> visibleDescendantsSet, Set<Shape> visited) {
        if (visited.contains(this)) return;
        visited.add(this);

        for (VisConnector connector : inConnectors) {
            Shape parent = connector.from;
            if (parent != null) {
                if (visited.contains(parent)) return;

                Set<Shape> relevantDescendants = new HashSet<>(visibleDescendantsSet);
                relevantDescendants.retainAll(parent.asVisClass().orderedDescendants);
                parent.hiddenDescendantsSet.addAll(relevantDescendants);
                //parent.checkAndUpdateChildrenVisibilityStates();
                parent.addHiddenDescendantsToAncestors(visibleDescendantsSet, visited);
            }
        }
    }


    /**
     * Checks and updates the hidden descendants of a specific shape.
     * It traverses all descendants connected to the current shape via its outConnectors
     * and collects only those that are hidden.
     */
    public void updateHiddenDescendants() {
        Set<Shape> hiddenDescendantsSet = new HashSet<>();
        collectHiddenDescendants(this, hiddenDescendantsSet);
        this.hiddenDescendantsSet.clear();
        this.hiddenDescendantsSet.addAll(hiddenDescendantsSet);
    }

    private void collectHiddenDescendants(Shape current, Set<Shape> hiddenDescendantsSet) {
        if (current.outConnectors.isEmpty()) {
            return;
        }

        for (VisConnector c : current.outConnectors) {
            Shape child = c.to;
            if (!child.isVisible()) hiddenDescendantsSet.add(child);
            collectHiddenDescendants(child, hiddenDescendantsSet);
        }
    }

    /**
     * Updates the parents hidden descendants count. Node hided by double-clicked
     */
    public void updateParents() {
        Set<Shape> visitedParents = new HashSet<>();
        for (VisConnector s: this.inConnectors){
            Shape parent =  s.from;
            updateAncestorsForHiddenDescendants(parent, visitedParents, parent.hiddenDescendantsSet);
        }
    }

    /**
     * Updates the hidden descendants count for the first visible parent of nodes in hiddenDescendantsSet.
     */
    public void updateHiddenDescendantsForParents() {
        Set<Shape> descendantsToProcess = new HashSet<>(hiddenDescendantsSet);
        Set<Shape> visitedNodes = new HashSet<>();

        updateAncestorsForHiddenDescendants(this, visitedNodes, hiddenDescendantsSet);

        visitedNodes.clear();
        for (Shape child : descendantsToProcess) {
            updateSameLevelParentsDescendants(child, visitedNodes, descendantsToProcess);
        }
    }

    /**
     * Updates the hidden descendants count for its ancestors.
     */
    private void updateAncestorsForHiddenDescendants(Shape currentNode, Set<Shape> visitedNodes, Set<Shape> hiddenChildrenSet) {
        if (visitedNodes.contains(currentNode)) return;

        visitedNodes.add(currentNode);
        for (VisConnector inConnector : currentNode.inConnectors) {
            Shape parent = inConnector.from;
            if (visitedNodes.contains(parent)) continue;

            parent.hiddenDescendantsSet.addAll(hiddenChildrenSet);
            updateAncestorsForHiddenDescendants(parent, visitedNodes, hiddenChildrenSet);
        }
    }

    /**
     * Updates the descendants count for the first visible parent of node.
     */
    private void updateSameLevelParentsDescendants(Shape currentNode, Set<Shape> visitedNodes, Set<Shape> visibleDescendantsSet) {
        if (visitedNodes.contains(currentNode)) return;
        visitedNodes.add(currentNode);

        for (VisConnector inConnector : currentNode.inConnectors) {
            Shape parent = inConnector.from;

            if (visitedNodes.contains(parent)) continue;

            if (parent.isVisible()) {
                parent.hiddenDescendantsSet.clear();
                for (VisConnector outConnector : parent.outConnectors) {
                    Shape childNode = outConnector.to;
                    addHiddenDescendants(childNode, parent.hiddenDescendantsSet);
                }
                parent.updateAncestorsForVisibleChildren(parent, new HashSet<>(), visibleDescendantsSet);
            } else {
                updateSameLevelParentsDescendants(parent, visitedNodes, visibleDescendantsSet);
            }
        }
    }

    /**
     * Adds all hidden descendants of a node to the provided set.
     */
    private void addHiddenDescendants(Shape s, Set<Shape> countedChildren) {
        if (!s.isVisible()) {
            countedChildren.add(s);
        }
        for (VisConnector outConnector : s.outConnectors) {
            addHiddenDescendants(outConnector.to, countedChildren);
        }
    }

    /**
     * Updates the hidden descendants count for the first visible parent of nodes in hiddenDescendantsSet.
     */
    public void updateVisibleDescendantsForParents() {
        Set<Shape> descendantsToProcess = new HashSet<>(visibleDescendantsSet);
        Set<Shape> visitedNodes = new HashSet<>();

        updateAncestorsForVisibleChildren(this, visitedNodes, descendantsToProcess);

        for (Shape child : descendantsToProcess) {
            updateSameLevelParentsDescendants(child, visitedNodes, visibleDescendantsSet);
        }

        visibleDescendantsSet.clear();
    }

    /**
     * Updates the hidden descendants count for its ancestors.
     */
    private void updateAncestorsForVisibleChildren(Shape currentNode, Set<Shape> visitedNodes, Set<Shape> visibleDescendantsSet) {
        if (visitedNodes.contains(currentNode)) return;

        visitedNodes.add(currentNode);
        for (VisConnector inConnector : currentNode.inConnectors) {
            Shape parent = inConnector.from;
            if (visitedNodes.contains(parent)) continue;

            parent.hiddenDescendantsSet.removeAll(visibleDescendantsSet);
            updateAncestorsForVisibleChildren(parent, visitedNodes, visibleDescendantsSet);
        }
    }

    /**
     * Updates the parents hidden descendants count by removing the hidden the visible nodes in hiddenDescendantsSet.
     */
    public void updateParentsShowLeft(Set<Shape> visibleDescendantsSet) {
        Set<Shape> visitedParents = new HashSet<>();
        updateAncestorsForHiddenDescendantsShowLeft(this, visitedParents, visibleDescendantsSet);
    }

    private void updateAncestorsForHiddenDescendantsShowLeft(Shape currentNode, Set<Shape> visitedNodes, Set<Shape> visibleDescendantsSet) {
        if (visitedNodes.contains(currentNode)) return;

        visitedNodes.add(currentNode);
        for (VisConnector inConnector : currentNode.inConnectors) {
            Shape parent = inConnector.from;
            if (visitedNodes.contains(parent)) continue;

            parent.hiddenDescendantsSet.removeAll(visibleDescendantsSet);
            updateAncestorsForHiddenDescendantsShowLeft(parent, visitedNodes, visibleDescendantsSet);
        }
    }

}
