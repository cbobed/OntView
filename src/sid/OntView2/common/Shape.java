
package sid.OntView2.common;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.util.Duration;
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
    public VisGraph getGraph() {
        return graph;
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

    public abstract void drawShape(GraphicsContext g);
    public abstract void paintFocus(GraphicsContext g);

    public abstract Point2D getConnectionPoint(Point2D point, boolean b);

    private final Set<Shape> hiddenDescendantsSet = new HashSet<>();
    private final Set<Shape> visibleDescendantsSet = new HashSet<>();

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
        hideSubLevels(getShapesFromStrategySteps(true, graph.paintframe.getPercentageShown()));
    }

    public void closeLeft() {
        setLeftState(LEFTCLOSED);
        hideParents(this, new HashSet<>());
    }

    public void resetHiddenChildrenCount() {
        hiddenDescendantsSet.clear();
    }

    public int getHiddenDescendantsSet() {
        return hiddenDescendantsSet.size();
    }

    public Set<Shape> get1(){
        return hiddenDescendantsSet;
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
        checkAndUpdateParentVisibilityStates();

    }

    /**
     * hides inConnectors and checks if parents need to be hidden
     */
    private void hideParents(Shape closedShape, Set<Shape> countedParents) {
        Shape parent;
        for (VisConnector connector : inConnectors) {
            parent = connector.from;

            if (parent.getLabel().matches("Thing")) {
                parent.checkAndUpdateChildrenVisibilityStates();
                break;
            }

            if (parent.hasOtherVisibleChildren(this)) {
                continue;
            }
            connector.hide();

            if (countedParents.add(parent)) {
                parent.checkAndHideParents(closedShape, countedParents);
            }
        }
        if (countedParents.isEmpty()) {
            Cursor prohibitedCursor = new ImageCursor(graph.paintframe.prohibitedImage);
            graph.paintframe.setCursor(prohibitedCursor);
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> graph.paintframe.setCursor(Cursor.DEFAULT));
            pause.play();
        }

        for (Shape s: countedParents) {
            s.addHiddenDescendantsToAncestors(countedParents, new HashSet<>());
        }
        checkAndUpdateChildrenVisibilityStates();
        checkAndUpdateParentVisibilityStates();

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
        }

        for (VisConnector c : outConnectors) {
            c.hide();
        }

        // Wake observer thread on hide event
        graph.getDashedConnectorList().clear();
        graph.addDashedConnectors();
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
            if (c.visible && !c.to.asVisClass().isBottom)
                return false;
        }
        return true;
    }

    public void notEnoughLimit(){
        int requiredValue = (int)  Math.min(Math.ceil(100.0 / asVisClass().orderedDescendants.size()),100);
        graph.paintframe.nTopPanel.changeLimitValue(requiredValue);
        graph.paintframe.showAlertDialog("Warning",
            "The selected visibility percentage is insufficient to display or hide at least one node.",
            "The minimum required value is " + requiredValue + "%. The percentage has been adjusted accordingly.",
            Alert.AlertType.INFORMATION);


    }

    public Set<Shape> getShapesFromStrategySteps(boolean toHide, int limit){
        switch (graph.paintframe.getStrategyOptionStep()) {
            case VisConstants.STEPSTRATEGY_RDFRANK -> {
                RDFRankSelectionStrategySteps RDFStrategy = new RDFRankSelectionStrategySteps(limit,
                    asVisClass().orderedDescendants, this);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            case VisConstants.STEPSTRATEGY_RDF_LEVEL_LR -> {
                RDFRankSelectionStrategyStepsLevel RDFStrategy = new RDFRankSelectionStrategyStepsLevel(limit,
                    asVisClass().orderedDescendantsByLevel, asVisClass().orderedDescendantsByLevelLeastImportant,
                    asVisClass().orderedDescendantsByLevelBottomTop, this, true);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            case VisConstants.STEPSTRATEGY_RDF_LEVEL_RL -> {
                RDFRankSelectionStrategyStepsLevel RDFStrategy = new RDFRankSelectionStrategyStepsLevel(limit,
                    asVisClass().orderedDescendantsByLevel, asVisClass().orderedDescendantsByLevelLeastImportant,
                    asVisClass().orderedDescendantsByLevelBottomTop, this, false);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            default -> {
                return null;
            }
        }
    }

    public Set<Shape> getShapesFromStrategyGlobal(boolean toHide, int limit){
        switch (graph.paintframe.getStrategyOptionGlobal()) {
            case VisConstants.GLOBALSTRATEGY_RDFRANK -> {
                RDFRankSelectionStrategyGlobal RDFStrategy = new RDFRankSelectionStrategyGlobal(limit,
                    asVisClass().orderedDescendants);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            case VisConstants.GLOBALSTRATEGY_RDF_LEVEL_LR -> {
                RDFRankSelectionStrategyGlobalLevel RDFStrategy = new RDFRankSelectionStrategyGlobalLevel(limit,
                    asVisClass().orderedDescendantsByLevel, asVisClass().orderedDescendantsByLevelLeastImportant,
                    asVisClass().orderedDescendantsByLevelBottomTop, this, true);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            case VisConstants.GLOBALSTRATEGY_RDF_LEVEL_RL -> {
                RDFRankSelectionStrategyGlobalLevel RDFStrategy = new RDFRankSelectionStrategyGlobalLevel(limit,
                    asVisClass().orderedDescendantsByLevel, asVisClass().orderedDescendantsByLevelLeastImportant,
                    asVisClass().orderedDescendantsByLevelBottomTop, this, false);
                return toHide ? RDFStrategy.getShapesToHide() : RDFStrategy.getShapesToVisualize();
            }
            default -> {
                return null;
            }
        }
    }

    /**************************************************************/

    public void openRight() {
        showSubLevels(getShapesFromStrategySteps(false, graph.paintframe.getPercentageShown()));
        graph.paintframe.setStateChanged(true);
        Platform.runLater(graph.paintframe.relaxerRunnable);
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
            }
            for (VisConnector c : outConnectors) {
                if (c.to.isVisible() && c instanceof VisConnectorIsA) c.show();
                else if (!c.to.isVisible() && c instanceof VisConnectorDashed) c.show();
            }
            checkAndUpdateParentVisibilityStates();
        }

        for (VisConnector connector : outConnectors) {
            connector.to.show(visibleDescendantsSet);
        }
    }

    public void showLeft(Set<Shape> visibleDescendantsSet) {
        if (!this.isVisible()) visibleDescendantsSet.add(this);
        this.visible = true;

        switch (getLeftState()) {
            case LEFTCLOSED:
                break;

            case LEFTOPEN, LEFT_PARTIALLY_CLOSED:
                for (VisConnector connector : inConnectors) {
                    connector.show();
                    connector.from.showLeft(visibleDescendantsSet);
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
            s.removeHiddenDescendantsFromAncestors(visibleDescendants, visited);
            for (VisConnector c : s.outConnectors) { // update direct children
                c.to.checkAndUpdateParentVisibilityStates();
            }
        }
        hiddenDescendantsSet.removeAll(visibleDescendants);
        checkAndUpdateChildrenVisibilityStates();
        checkAndUpdateParentVisibilityStates();
    }

    public void showParentLevels() {
        for (VisConnector c : inConnectors) {
            c.from.showLeft(visibleDescendantsSet);
            c.show();
            c.from.checkAndUpdateChildrenVisibilityStates();
            c.from.checkAndUpdateParentVisibilityStates();
        }
        for (Shape s: visibleDescendantsSet){
            s.updateParentsShowLeft(visibleDescendantsSet);
        }
        checkAndUpdateChildrenVisibilityStates();
        checkAndUpdateParentVisibilityStates();
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

    public static final Comparator<Shape> POSY_ORDER = Comparator.comparingInt(Shape::getTopCorner);

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

            if (childNode.asVisClass().isBottom) continue;

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
                parent.checkAndUpdateChildrenVisibilityStates();
                parent.checkAndUpdateParentVisibilityStates();
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
                Set<Shape> relevantDescendants = new HashSet<>(visibleDescendantsSet);
                relevantDescendants.retainAll(parent.asVisClass().orderedDescendants);
                parent.hiddenDescendantsSet.addAll(relevantDescendants);
                parent.checkAndUpdateChildrenVisibilityStates();
                parent.checkAndUpdateParentVisibilityStates();
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
        Set<Shape> collected = new HashSet<>();
        collectHiddenDescendants(this, collected, new HashSet<>());
        this.hiddenDescendantsSet.clear();
        this.hiddenDescendantsSet.addAll(collected);
    }

    private void collectHiddenDescendants(Shape current, Set<Shape> collected, Set<Shape> visited) {
        if (!visited.add(current)) return;
        if (current.outConnectors.isEmpty()) return;

        for (VisConnector c : current.outConnectors) {
            Shape child = c.to;
            if (!child.isVisible()) collected.add(child);
            checkAndUpdateChildrenVisibilityStates();
            checkAndUpdateParentVisibilityStates();
            collectHiddenDescendants(child, collected, visited);
        }
    }

    /**
     * Updates the parents hidden descendants count. Node hided by double-clicked
     */
    public void updateParents() {
        Set<Shape> descendantsToProcess = new HashSet<>();
        descendantsToProcess.add(this);
        addHiddenDescendantsToAncestors(descendantsToProcess, new HashSet<>());
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
            parent.checkAndUpdateChildrenVisibilityStates();
            parent.checkAndUpdateParentVisibilityStates();
            parent.hiddenDescendantsSet.removeAll(visibleDescendantsSet);
            updateAncestorsForHiddenDescendantsShowLeft(parent, visitedNodes, visibleDescendantsSet);
        }
    }

    /**
     * Determines whether the node can use displayed or hidden functionalities on its right side.
     */
    public boolean canShowFunctionalityRight() {
        return !asVisClass().children.isEmpty() && (outConnectors != null) && (!outConnectors.isEmpty()) &&
            (!outConnectors.get(0).to.asVisClass().isBottom) && (!asVisClass().isBottom);
    }

    /**
     * Determines whether the node can use displayed or hidden functionalities on its left side.
     */
    public boolean canShowFunctionalityLeft() {
        return !asVisClass().parents.isEmpty() && (inConnectors != null) && (!inConnectors.isEmpty()) &&
            (!inConnectors.get(0).from.getLabel().matches("Thing")) && (!asVisClass().isBottom);
    }
}
