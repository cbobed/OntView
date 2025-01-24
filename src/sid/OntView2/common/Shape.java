
package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.*;

public abstract class Shape {

    public static final int CLOSED = 0;
    public static final int OPEN = 1;
    public static final int PARTIALLY_CLOSED = 2;
    public static final int LEFTCLOSED = 3;
    public static final int LEFTOPEN = 4;
    public static final int LEFT_PARTIALLY_CLOSED = 5;


    public int posx, posy;
    private int height, width;
    int depthlevel;
    VisLevel vdepthlevel;
    VisGraph graph;
    Point2D connectionPointsL;
    Point2D connectionPointsR;
    ArrayList<VisConnector> inConnectors,
            outConnectors,
            inDashedConnectors,
            outDashedConnectors;

    //when showing i need to keep track of those that were closed

    int state = OPEN;
    int leftState = LEFTOPEN;
    boolean visible = true;

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

    public void setHeight(int x) {
        height = x;
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

    public void setState(int pstate) {
        state = pstate;
    }

    public int getState() {
        return state;
    }

    public void setLeftState(int pstate) {
        leftState = pstate;
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
        setState(CLOSED);
        hideSubLevels(this, hiddenDescendantsSet);
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
     * Check if childres has other visible parents
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
     * hides outconnectors and checks if children need to be hidden
     *
     * @param closedShape
     */
    private void hideSubLevels(Shape closedShape, Set<Shape> countedChildren) {
        // hides outconnectors and
        // checks if children need to be hidden
        // if so, it hides it

        Shape child;
        for (VisConnector connector : outConnectors) {
            if (!connector.isVisible()) continue;
            child = connector.to;

            if (child.childHasOtherParents()) {
                connector.hide();
                child.checkAndUpdateParentVisibilityStates();
                continue;
            }

            connector.hide();
            if (countedChildren.add(child)) {
                child.checkAndHide(closedShape, countedChildren);
            }
        }

    }

    /**
     * hides inconnectors and checks if parents need to be hidden
     */
    private void hideParents(Shape closedShape, Set<Shape> countedParents) {
        Shape parent;
        for (VisConnector connector : inConnectors) {
            parent = connector.from;

            if (parent.getLabel().matches("Thing")) break;

            connector.hide();

            if (parent.hasOtherVisibleChildren(this)) {
                parent.setState(PARTIALLY_CLOSED);
                continue;
            }

            if (countedParents.add(parent)) {
                parent.checkAndHideParents(closedShape, countedParents);
            }
        }

    }

    /**
     * Checks references
     * Before setting invisible a shape we need to check if there's still
     * any reference ( an in Connector)
     */
    public void checkAndHide(Shape closedShape, Set<Shape> countedChildren) {
        if (getVisibleInReferences() == 0) {
            this.visible = false;
            countedChildren.add(this);
            hideSubLevels(closedShape, countedChildren);
            return;
        }
        hideSubLevels(closedShape, countedChildren);
    }

    /**
     * Checks references
     * Before setting invisible a shape we need to check if there's still
     * any reference ( an out Connector)
     */
    public void checkAndHideParents(Shape closedShape, Set<Shape> countedParents) {
        if (getVisibleOutReferences() == 0) {
            this.visible = false;
            countedParents.add(this);
            for (VisConnector connector : inConnectors) {
                connector.hide();
            }
            hideParents(closedShape, countedParents);
        }
    }

    /**
     * hides shape, connector and notifies parents
     */
    public void hide() {
        this.visible = false;
        for (VisConnector c : inConnectors) {
            c.hide();
            c.from.hiddenDescendantsSet.add(this);
            c.from.notifyHidden(this);
        }

        for (VisConnector c : outConnectors) {
            c.hide();
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
     * this is called by a hidden node
     * it notifies parents that it's hidden
     */
    private void notifyHidden(Shape s) {

        if (!(this instanceof VisConstraint)) {
            if (allSubHidden()) {
                setState(CLOSED);
            } else {
                setState(PARTIALLY_CLOSED);
            }
        } else {
            for (VisConnector c : inConnectors) {
                if (allSubHidden()) {
                    this.visible = false;
                }
                c.hide();
                //notify Parent
                c.from.notifyHidden(this);
            }
        }
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

    /**************************************************************/

    public void openRight() {
        setState(OPEN);
        showSubLevels();
    }

    public void openLeft() {
        setLeftState(LEFTOPEN);
        showParentLevels();
    }

    public void show(Shape parent, Set<Shape> openDescendantsSet) {
        if (!this.isVisible()) openDescendantsSet.add(this);
        this.visible = true;

        switch (getState()) {
            case CLOSED:
                break;

            case OPEN, PARTIALLY_CLOSED:
                for (VisConnector connector : outConnectors) {
                    if (connector.isVisible()) continue;
                    connector.show();
                    connector.to.show(this, openDescendantsSet);
                    connector.to.checkAndUpdateParentVisibilityStates();
                }
                break; //if its not a previously hidden node we'll show it
        }
    }

    public void showLeft(Shape parent) {
        this.visible = true;

        switch (getLeftState()) {
            case LEFTCLOSED:
                break;

            case LEFTOPEN, LEFT_PARTIALLY_CLOSED:
                for (VisConnector connector : inConnectors) {
                    connector.show();
                    connector.from.showLeft(this);
                    connector.from.checkAndUpdateChildrenVisibilityStates();
                }
                break;
        }
    }

    public void showSubLevels() {
        for (VisConnector c : outConnectors) {
            if (c.isVisible()) continue;
            c.to.show(this, visibleDescendantsSet);
            c.show();
            c.to.checkAndUpdateParentVisibilityStates();
        }
        hiddenDescendantsSet.removeAll(visibleDescendantsSet);

    }

    public void showParentLevels() {
        for (VisConnector c : inConnectors) {
            c.from.showLeft(this);
            c.show();
            c.from.checkAndUpdateChildrenVisibilityStates();
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
     * Inverts lookup in the shapeMap by returning the key out of an owlclassexpression
     *
     * @param e
     * @return
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
     * Recursively collects the count of hidden children in the hierarchy.
     * Traverses the entire list of connectors until it encounters a child
     * with no outgoing connectors and accumulates the count of hidden nodes.
     */
    public void collectHiddenChildren() {
        collectHiddenChildren(hiddenDescendantsSet);
    }

    public void collectHiddenChildren(Set<Shape> countedChildren) {
        for (VisConnector connector : outConnectors) {
            Shape child = connector.to;

            if (!child.isVisible()) {
                countedChildren.add(child);
            }

            if (!child.outConnectors.isEmpty()) {
                child.collectHiddenChildren(countedChildren);
            }
        }
    }

    /**
     * Updates the state of the shape based on the visibility of its children.
     */
    public void checkAndUpdateChildrenVisibilityStates() {
        // Check children visibility
        boolean allChildrenHidden = true;
        boolean allChildrenVisible = true;

        for (VisConnector connector : outConnectors) {
            if (connector.isVisible()) {
                allChildrenHidden = false;
            } else {
                allChildrenVisible = false;
            }

            if (!allChildrenHidden && !allChildrenVisible) {
                break;
            }
        }

        if (allChildrenHidden) {
            setState(CLOSED);
        } else if (allChildrenVisible) {
            resetHiddenChildrenCount();
            setState(OPEN);
        } else {
            setState(PARTIALLY_CLOSED);
        }
    }

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

        updateAncestorsForHiddenDescendants(this, visitedNodes, descendantsToProcess);

        for (Shape child : descendantsToProcess) {
            updateSameLevelParentsHiddenDescendants(child, visitedNodes);
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
     * Updates the hidden descendants count for the first visible parent of node.
     */
    private void updateSameLevelParentsHiddenDescendants(Shape currentNode, Set<Shape> visitedNodes) {
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
            } else {
                updateSameLevelParentsHiddenDescendants(parent, visitedNodes);
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
            updateSameLevelParentsVisibleDescendants(child, visitedNodes);
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
     * Updates the hidden descendants count for the first visible parent of node.
     */
    private void updateSameLevelParentsVisibleDescendants(Shape currentNode, Set<Shape> visitedNodes) {
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
            } else {
                updateSameLevelParentsHiddenDescendants(parent, visitedNodes);
            }
        }
    }

    // NOT USED
    /**
     * Updates the hidden parents count for children of nodes in countedParents.
     */
    private void updateHiddenParentsForChildren() {
        Set<Shape> parentsToProcess = new HashSet<>(hiddenParentsSet);

        for (Shape parent : parentsToProcess) {
            for (VisConnector connector : parent.outConnectors) {
                Shape child = connector.to;

                if (!child.isVisible()) {
                    continue;
                }

                child.hiddenParentsSet.clear();

                for (VisConnector inConnector : child.inConnectors) {
                    Shape grandParent = inConnector.from;
                    if (!grandParent.isVisible()) {
                        addHiddenAncestors(grandParent, child.hiddenParentsSet);
                    }
                }
            }
        }
    }

    /**
     * Adds all hidden ancestors of a node to the provided set until a visible ancestor is encountered.
     */
    private void addHiddenAncestors(Shape s, Set<Shape> countedParents) {
        if (!s.isVisible()) {
            if (countedParents.add(s)) {
                for (VisConnector inConnector : s.inConnectors) {
                    addHiddenAncestors(inConnector.from, countedParents);
                }
            }
        }
    }
}
