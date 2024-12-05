
package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.apache.jena.base.Sys;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
    boolean wasOpened = true;
    boolean visible = true;
    boolean wasVisible = true;
    boolean selected = false;
    boolean moved = false;

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

    private final Set<Shape> countedChildren = new HashSet<>();
    private final Set<Shape> countedParents = new HashSet<>();
    public boolean hiddenChildren = false;
    public boolean hiddenParents = false;


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
        hideSubLevels(this, countedChildren);
    }

    public void closeLeft() {
        setLeftState(LEFTCLOSED);
        hideParents(this, countedParents);
    }

    public void resetHiddenChildrenCount() {
        hiddenChildren = false;
        countedChildren.clear();
    }

    public void resetHiddenParentsCount() {
        hiddenParents = false;
        countedParents.clear();
    }

    public int getHiddenChildrenCount() {
        hiddenChildren = true;
        System.out.println(this.getLabel() + " " + countedChildren.size() + "................");
        for (Shape c : countedChildren) {
            System.out.println(c.getLabel());
        }
        return countedChildren.size();
    }

    public int getHiddenParentsCount() {
        hiddenParents = true;
        System.out.println(this.getLabel() + " " + countedParents.size() + "................");
        for (Shape c : countedParents) {
            System.out.println(c.getLabel());
        }
        return countedParents.size();
    }

    public void setHiddenChildren() {
        hiddenChildren = !countedChildren.isEmpty();
    }

    public void setHiddenParents() {
        hiddenParents = !countedParents.isEmpty();
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
            child = connector.to;

            if (child.childHasOtherParents()) {
                connector.hide();
                continue;
            }

            connector.hide();
            if (countedChildren.add(child))
                child.checkAndHide(closedShape, countedChildren);
        }

        System.out.println("final " + getLabel());
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
        updateHiddenParentsForChildren();
        System.out.println("final " + getLabel() + "\n");
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

    public void show(Shape parent) {
        //System.out.println(this.getLabel() + " " + getState());
        this.visible = true;

        switch (getState()) {
            case CLOSED:
                break;

            case OPEN, PARTIALLY_CLOSED:
                for (VisConnector connector : outConnectors) {
                    if (connector.isVisible()) continue;
                    connector.show();
                    connector.to.show(this);
                    connector.to.checkLastNode();
                }
                break;//if its not a previously hidden node we'll show it
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
                }
                break;
        }
    }

    public void showSubLevels() {
        for (VisConnector c : outConnectors) {
            c.to.show(this);
            c.show();
            c.to.checkLastNode();
        }
    }

    public void checkLastNode() {
        if (outConnectors.isEmpty()) {
            checkAndUpdateState();
        }
    }

    public void showParentLevels() {
        for (VisConnector c : inConnectors) {
            c.from.showLeft(this);
            c.show();
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
     * Check and update the state of each node based on the visibility of
     * its connectors and children
     */
    private void checkAndUpdateState() {
        //System.out.println("checkAndUpdateState " + getLabel());
        int inConnectorsHidden = 0;
        for (VisConnector connector : inConnectors) {

            if (!connector.isVisible()) inConnectorsHidden++;
        }
        if (inConnectorsHidden == inConnectors.size()) {
            setLeftState(LEFTCLOSED);
        } else if (inConnectorsHidden == 0) {
            setLeftState(LEFTOPEN);
        } else {
            setLeftState(LEFT_PARTIALLY_CLOSED);
        }
        //System.out.println(inConnectorsHidden);
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
        collectHiddenChildren(countedChildren);
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
     * Updates the hidden parents count for children of nodes in countedParents.
     */
    private void updateHiddenParentsForChildren() {
        Set<Shape> parentsToProcess = new HashSet<>(countedParents);

        for (Shape parent : parentsToProcess) {
            for (VisConnector connector : parent.outConnectors) {
                Shape child = connector.to;

                if (!child.isVisible()) {
                    continue;
                }

                child.countedParents.clear();

                for (VisConnector inConnector : child.inConnectors) {
                    Shape grandParent = inConnector.from;
                    if (!grandParent.isVisible()) {
                        addHiddenAncestors(grandParent, child.countedParents);
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
