/*
 * {{{ header & license
 * Copyright (c) 2004 Joshua Marinacci
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.layout;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xhtmlrenderer.css.Border;
import org.xhtmlrenderer.css.FontResolver;
import org.xhtmlrenderer.css.StyleReference;
import org.xhtmlrenderer.css.newmatch.CascadedStyle;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.EmptyStyle;
import org.xhtmlrenderer.extend.RenderingContext;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.InlineBox;
import org.xhtmlrenderer.render.Renderer;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.util.Uu;
import org.xhtmlrenderer.util.XRLog;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;
import java.util.logging.Level;

/**
 * Description of the Class
 *
 * @author empty
 */
public class Context {

    /**
     * The media for this context
     */
    public String getMedia() {
        return getCtx().getMedia();
    }

    /**
     * Description of the Field
     */
    private Graphics2D graphics;

    /**
     * Description of the Field
     */
    private StyleReference css;

    /**
     * Description of the Field
     */
    private boolean debug_draw_boxes;

    /**
     * Description of the Field
     */
    private boolean debug_draw_line_boxes;
    private boolean debug_draw_inline_boxes;
    private boolean debug_draw_font_metrics;

    /**
     * Description of the Field
     */
    private BasicPanel canvas;

    //public Graphics canvas_graphics;

    /**
     * Description of the Field
     */
    //public JComponent viewport;

    /**
     * Description of the Field
     */
    public void setGraphics(Graphics2D graphics) {
        this.graphics = graphics;
    }

    /*
     * selection management code
     */
    /**
     * Description of the Field
     */
    private Box selection_start, selection_end;

    /**
     * Description of the Field
     */
    private int selection_end_x, selection_start_x;


    /**
     * Description of the Field
     */
    private boolean in_selection = false;


    /**
     * Description of the Field
     */
    private int list_counter;

    /*
     * =========== form access code =============
     */
    /**
     * Description of the Field
     */
    private String form_name = null;
    /**
     * Description of the Field
     */
    private Map forms = new HashMap();
    /**
     * Description of the Field
     */
    private Map actions = new HashMap();
    /**
     * the current block formatting context
     */
    private BlockFormattingContext bfc;
    protected Stack bfc_stack;

    /**
     * Description of the Field
     */
    private Stack extents_stack = new Stack();

    /**
     * Description of the Field
     */
    private FontResolver font_resolver;

    /**
     * Description of the Field
     */
    private Rectangle extents;

    /**
     * Description of the Field
     */
    private int xoff = 0;

    /**
     * Description of the Field
     */
    private int yoff = 0;

    /**
     * Description of the Field
     */
    private int max_width;
    /**
     * Description of the Field
     */
    private boolean sub_block = false;

    private RenderingContext ctx;

    public RenderingContext getRenderingContext() {
        return getCtx();
    }

    public TextRenderer getTextRenderer() {
        return getCtx().getTextRenderer();
    }

    /**
     * Constructor for the Context object
     */
    public Context() {
        font_resolver = new FontResolver();
        bfc_stack = new Stack();
    }

    //Style-handling stuff
    private Stack styleStack;

    public void initializeStyles(EmptyStyle c) {
        styleStack = new Stack();
        styleStack.push(c);
    }

    public void pushStyle(CascadedStyle s) {
        CalculatedStyle parent = (CalculatedStyle) styleStack.peek();
        CalculatedStyle derived = getCss().getDerivedStyle(parent, s);
        styleStack.push(derived);
    }

    public void popStyle() {
        styleStack.pop();
    }

    public CalculatedStyle getCurrentStyle() {
        return (CalculatedStyle) styleStack.peek();
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawBoxes() {
        return isDebug_draw_boxes();
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawLineBoxes() {
        return isDebug_draw_line_boxes();
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawInlineBoxes() {
        return isDebug_draw_inline_boxes();
    }

    public boolean debugDrawFontMetrics() {
        return isDebug_draw_font_metrics();
    }

    /**
     * Description of the Method
     *
     * @param x PARAM
     * @param y PARAM
     */
    public void translate(int x, int y) {
        //Uu.p("trans: " + x + "," + y);
        this.graphics.translate(x, y);
        if (getBfc() != null) {
            getBfc().translate(x, y);
        }
        xoff += x;
        yoff += y;
    }

    public Point getOriginOffset() {
        return new Point(xoff, yoff);
    }

    /**
     * Adds a feature to the MaxWidth attribute of the Context object
     *
     * @param max_width The feature to be added to the MaxWidth attribute
     */
    public void addMaxWidth(int max_width) {
        if (max_width > this.max_width) {
            this.max_width = max_width;
        }
    }

    /**
     * Description of the Method
     *
     * @param block PARAM
     */
    public void shrinkExtents(Box block) {

        extents_stack.push(getExtents());

        
        //Border border = block.border;
        //Border padding = block.padding;
        //Border margin = block.margin;

        Rectangle rect = new Rectangle(0, 0,
                getExtents().width - block.totalHorizontalPadding(getCurrentStyle()),
                getExtents().height - block.totalVerticalPadding(getCurrentStyle()));

        setExtents(rect);
    }


    /**
     * Description of the Method
     *
     * @param block PARAM
     */
    public void unshrinkExtents(Box block) {
        setExtents((Rectangle) extents_stack.pop());
    }


    /**
     * Description of the Method
     *
     * @param box PARAM
     */
    public void translateInsets(Box box) {
        Border border = LayoutUtil.getBorder(box, getCurrentStyle());
        Border margin = getCurrentStyle().getMarginWidth();
        Border padding = getCurrentStyle().getPaddingWidth();
        if (box == null) {
            XRLog.render(Level.WARNING, "null box");
            return;//TODO: why?
        }
        if (margin == null) {
            XRLog.render(Level.WARNING, "translate insets: null margin on box of type " + box.getClass().getName() +
                    " content " + (box.content == null ? "null" : box.content.getClass().getName()));
            return;
        }
        if (border == null) {
            XRLog.render(Level.WARNING, "translate insets: null border on box of type " + box.getClass().getName() +
                    " content " + (box.content == null ? "null" : box.content.getClass().getName()));
            return;
        }
        if (padding == null) {
            XRLog.render(Level.WARNING, "translate insets: null padding on box of type " + box.getClass().getName() +
                    " content " + (box.content == null ? "null" : box.content.getClass().getName()));
            return;
        }
        translate(margin.left + border.left + padding.left,
                margin.top + border.top + padding.top);
    }

    /**
     * Description of the Method
     *
     * @param box PARAM
     */
    public void untranslateInsets(Box box) {
        Border border = LayoutUtil.getBorder(box, getCurrentStyle());
        Border margin = getCurrentStyle().getMarginWidth();
        Border padding = getCurrentStyle().getPaddingWidth();
        if (margin == null) {
            XRLog.render(Level.WARNING, "translate insets: null margin on box of type " + box.getClass().getName() +
                    " content " + (box.content == null ? "null" : box.content.getClass().getName()));
            return;
        }
        if (border == null) {
            XRLog.render(Level.WARNING, "translate insets: null border on box of type " + box.getClass().getName() +
                    " content " + (box.content == null ? "null" : box.content.getClass().getName()));
            return;
        }
        if (padding == null) {
            XRLog.render(Level.WARNING, "translate insets: null padding on box of type " + box.getClass().getName() +
                    " content " + (box.content == null ? "null" : box.content.getClass().getName()));
            return;
        }
        translate(-(margin.left + border.left + padding.left),
                -(margin.top + border.top + padding.top));
    }


    /**
     * Converts to a String representation of the object.
     *
     * @return A string representation of the object.
     */
    public String toString() {
        return "Context: extents = " +
                "(" + extents.x + "," + extents.y + ") -> (" + extents.width + "x" + extents.height + ")"
                //" cursor = " + cursor +
                //"\n color = " + color + " background color = " + background_color;
                + " offset = " + xoff + "," + yoff
                ;
    }


    /**
     * Description of the Method
     */
    public void clearSelection() {
        setSelection_end(null);
        setSelection_start(null);
        setSelection_start_x(-1);
        setSelection_end_x(-1);
    }

    /**
     * Description of the Method
     *
     * @param box PARAM
     */
    public void updateSelection(Box box) {
        if (box == getSelection_end()) {
            setIn_selection(false);
        }
        if (box == getSelection_start()) {
            setIn_selection(true);
        }
        if (box == getSelection_end() && box == getSelection_start()) {
            setIn_selection(false);
        }
    }

    /**
     * Description of the Method
     *
     * @param box PARAM
     * @return Returns
     */
    public boolean inSelection(Box box) {
        if (box == getSelection_end() ||
                box == getSelection_start()) {
            return true;
        }
        return isIn_selection();
    }

    /**
     * Adds a feature to the InputField attribute of the Context object
     *
     * @param name    The feature to be added to the InputField attribute
     * @param element The feature to be added to the InputField attribute
     * @param comp    The feature to be added to the InputField attribute
     * @return Returns
     */
    public FormComponent addInputField(String name, Element element, JComponent comp) {
        if (getForm() == null) {
            Uu.p("warning! attempted to add input field: '" + name + "' to a form without a 'name' attribute");
            return null;
        }
        Map fields = (Map) getForms().get(getForm());
        List field_list = new ArrayList();
        if (fields.containsKey(name)) {
            field_list = (List) fields.get(name);
        }
        FormComponent fc = new FormComponent();
        fc.name = element.getAttribute("name");
        fc.element = element;
        fc.component = comp;
        field_list.add(fc);
        fields.put(name, field_list);
        return fc;
    }

    /**
     * Sets the extents attribute of the Context object
     *
     * @param rect The new extents value
     */
    public void setExtents(Rectangle rect) {
        this.extents = rect;
        if (extents.width < 1) {
            XRLog.exception("width < 1");
            extents.width = 1;
        }
    }

    /**
     * Sets the maxWidth attribute of the Context object
     *
     * @param max_width The new maxWidth value
     */
    public void setMaxWidth(int max_width) {
        this.max_width = max_width;
    }


    /**
     * Sets the selectionStart attribute of the Context object
     *
     * @param box The new selectionStart value
     */
    public void setSelectionStart(Box box, int x) {
        setSelection_start(box);
        setSelection_start_x(x);
        if (box instanceof InlineBox) {
            InlineBox ib = (InlineBox) box;
            int i = ib.getTextIndex(this, x);
        }
    }

    /**
     * Sets the selectionEnd attribute of the Context object
     *
     * @param box The new selectionEnd value
     */
    public void setSelectionEnd(Box box, int x) {
        setSelection_end(box);
        setSelection_end_x(x);
        if (box instanceof InlineBox) {
            InlineBox ib = (InlineBox) box;
            int i = ib.getTextIndex(this, x);
            setSelection_end_x(ib.getAdvance(this, i));
        }
    }


    /**
     * Sets the listCounter attribute of the Context object
     *
     * @param counter The new listCounter value
     */
    public void setListCounter(int counter) {
        this.setList_counter(counter);
    }

    /**
     * Sets the form attribute of the Context object
     *
     * @param form_name The new form value
     * @param action    The new form value
     */
    public void setForm(String form_name, String action) {
        this.setForm_name(form_name);
        if (form_name != null) {
            getForms().put(form_name, new HashMap());
            getActions().put(form_name, action);
        }
    }


    /*
     * notes to help manage inline sub blocks (like table cells)
     */
    /**
     * Sets the subBlock attribute of the Context object
     *
     * @param sub_block The new subBlock value
     */
    public void setSubBlock(boolean sub_block) {
        this.sub_block = sub_block;
    }

    /**
     * Gets the graphics attribute of the Context object
     *
     * @return The graphics value
     */
    public Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * Gets the extents attribute of the Context object
     *
     * @return The extents value
     */
    public Rectangle getExtents() {
        return this.extents;
    }

    /**
     * Gets the viewport attribute of the Context object
     *
     * @return The viewport value
     */
    /*public JComponent getViewport() {
        return this.viewport;
    }*/

    /**
     * Gets the xoff attribute of the Context object
     *
     * @return The xoff value
     */
    /*public int getXoff() {
        return this.xoff;
    }*/

    /**
     * Gets the yoff attribute of the Context object
     *
     * @return The yoff value
     */
    /*public int getYoff() {
        return this.yoff;
    }*/

    /**
     * Gets the maxWidth attribute of the Context object
     *
     * @return The maxWidth value
     */
    public int getMaxWidth() {
        return this.max_width;
    }

    /* =========== Font stuff ============== */

    /**
     * Gets the fontResolver attribute of the Context object
     *
     * @return The fontResolver value
     */
    public FontResolver getFontResolver() {
        return font_resolver;
    }

    public void flushFonts() {
        font_resolver = new FontResolver();
    }


    
    
    /* =========== Selection Management ============== */
    
    
    /**
     * Gets the selectionStart attribute of the Context object
     *
     * @return The selectionStart value
     */
    public Box getSelectionStart() {
        return getSelection_start();
    }

    /**
     * Gets the selectionEnd attribute of the Context object
     *
     * @return The selectionEnd value
     */
    public Box getSelectionEnd() {
        return getSelection_end();
    }

    /**
     * Gets the selectionStartX attribute of the Context object
     *
     * @return The selectionStartX value
     */
    public int getSelectionStartX() {
        return getSelection_start_x();
    }

    /**
     * Gets the selectionEndX attribute of the Context object
     *
     * @return The selectionEndX value
     */
    public int getSelectionEndX() {
        return getSelection_end_x();
    }

    
    
    /* =========== List stuff ============== */

    /**
     * Gets the listCounter attribute of the Context object
     *
     * @return The listCounter value
     */
    public int getListCounter() {
        return getList_counter();
    }

    
    
    /* =========== Form Stuff ============== */

    /**
     * Gets the form attribute of the Context object
     *
     * @return The form value
     */
    public String getForm() {
        return this.getForm_name();
    }

    /**
     * Gets the inputFieldComponents attribute of the Context object
     *
     * @param form_name PARAM
     * @return The inputFieldComponents value
     */
    public Iterator getInputFieldComponents(String form_name) {
        Map fields = (Map) getForms().get(form_name);
        return fields.values().iterator();
    }

    /**
     * Gets the inputFieldComponents attribute of the Context object
     *
     * @param form_name  PARAM
     * @param field_name PARAM
     * @return The inputFieldComponents value
     */
    public List getInputFieldComponents(String form_name, String field_name) {
        Map fields = (Map) getForms().get(form_name);
        List field_list = (List) fields.get(field_name);
        if (field_list == null) {
            return new ArrayList();
        }
        return field_list;
    }

    /**
     * Gets the formAction attribute of the Context object
     *
     * @param form_name PARAM
     * @return The formAction value
     */
    public String getFormAction(String form_name) {
        return (String) getActions().get(form_name);
    }

    /**
     * Gets the forms attribute of the Context object
     *
     * @return The forms value
     */
    public Map getForms() {
        return forms;
    }

    public StyleReference getCss() {
        return css;
    }

    public void setCss(StyleReference css) {
        this.css = css;
    }

    public boolean isDebug_draw_boxes() {
        return debug_draw_boxes;
    }

    public void setDebug_draw_boxes(boolean debug_draw_boxes) {
        this.debug_draw_boxes = debug_draw_boxes;
    }

    public boolean isDebug_draw_line_boxes() {
        return debug_draw_line_boxes;
    }

    public void setDebug_draw_line_boxes(boolean debug_draw_line_boxes) {
        this.debug_draw_line_boxes = debug_draw_line_boxes;
    }

    public boolean isDebug_draw_inline_boxes() {
        return debug_draw_inline_boxes;
    }

    public void setDebug_draw_inline_boxes(boolean debug_draw_inline_boxes) {
        this.debug_draw_inline_boxes = debug_draw_inline_boxes;
    }

    public boolean isDebug_draw_font_metrics() {
        return debug_draw_font_metrics;
    }

    public void setDebug_draw_font_metrics(boolean debug_draw_font_metrics) {
        this.debug_draw_font_metrics = debug_draw_font_metrics;
    }

    public BasicPanel getCanvas() {
        return canvas;
    }

    public void setCanvas(BasicPanel canvas) {
        this.canvas = canvas;
    }

    public Box getSelection_start() {
        return selection_start;
    }

    public void setSelection_start(Box selection_start) {
        this.selection_start = selection_start;
    }

    public Box getSelection_end() {
        return selection_end;
    }

    public void setSelection_end(Box selection_end) {
        this.selection_end = selection_end;
    }

    public int getSelection_end_x() {
        return selection_end_x;
    }

    public void setSelection_end_x(int selection_end_x) {
        this.selection_end_x = selection_end_x;
    }

    public int getSelection_start_x() {
        return selection_start_x;
    }

    public void setSelection_start_x(int selection_start_x) {
        this.selection_start_x = selection_start_x;
    }

    public boolean isIn_selection() {
        return in_selection;
    }

    public void setIn_selection(boolean in_selection) {
        this.in_selection = in_selection;
    }

    public int getList_counter() {
        return list_counter;
    }

    public void setList_counter(int list_counter) {
        this.list_counter = list_counter;
    }

    public String getForm_name() {
        return form_name;
    }

    public void setForm_name(String form_name) {
        this.form_name = form_name;
    }

    public void setForms(Map forms) {
        this.forms = forms;
    }

    public Map getActions() {
        return actions;
    }

    public void setActions(Map actions) {
        this.actions = actions;
    }

    public BlockFormattingContext getBfc() {
        return bfc;
    }

    public void setBfc(BlockFormattingContext bfc) {
        this.bfc = bfc;
    }

    public RenderingContext getCtx() {
        return ctx;
    }

    public void setCtx(RenderingContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    public class FormComponent {
        /**
         * Description of the Field
         */
        public String name;
        /**
         * Description of the Field
         */
        public JComponent component;
        /**
         * Description of the Field
         */
        public Element element;
        /**
         * Description of the Field
         */
        public ButtonGroup group;

        /**
         * Description of the Method
         */
        public void reset() {
            Uu.p("resetting");
            if (component instanceof JTextField) {
                Uu.p("it's a text field");
                if (element.hasAttribute("value")) {
                    Uu.p("setting to : " + element.getAttribute("value"));
                    ((JTextField) component).setText(element.getAttribute("value"));
                } else {
                    ((JTextField) component).setText("");
                }
            }
        }
    }


    public BlockFormattingContext getBlockFormattingContext() {
        return getBfc();
    }

    public void pushBFC(BlockFormattingContext bfc) {
        bfc_stack.push(this.getBfc());
        this.setBfc(bfc);
    }

    public void popBFC() {
        this.setBfc((BlockFormattingContext) bfc_stack.pop());
    }

    public void setBlockFormattingContext(BlockFormattingContext bfc) {
        this.setBfc(bfc);
    }

    
    
    /* ================== Extra Utility Funtions ============== */
    /**
     * Gets the subBlock attribute of the Context object
     *
     * @return The subBlock value
     */
    public boolean isSubBlock() {
        return sub_block;
    }

    private boolean first_line = false;

    public boolean isFirstLine() {
        return first_line;
    }

    public void setFirstLine(boolean first_line) {
        this.first_line = first_line;
    }


    public Rectangle getFixedRectangle() {
        //Uu.p("this = " + canvas);
        Rectangle rect = getCanvas().getFixedRectangle();
        rect.translate(getCanvas().getX(), getCanvas().getY());
        return rect;
    }

    public Layout getLayout(Node node) {
        return getCtx().getLayoutFactory().getLayout(this, node);
    }

    public Renderer getRenderer(Node node) {
        return getCtx().getLayoutFactory().getRenderer(this, node);
    }

}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.40  2004/12/29 07:35:38  tobega
 * Prepared for cloned Context instances by encapsulating fields
 *
 * Revision 1.39  2004/12/28 01:48:23  tobega
 * More cleaning. Magically, the financial report demo is starting to look reasonable, without any effort being put on it.
 *
 * Revision 1.38  2004/12/27 09:40:47  tobega
 * Moved more styling to render stage. Now inlines have backgrounds and borders again.
 *
 * Revision 1.37  2004/12/27 07:43:31  tobega
 * Cleaned out border from box, it can be gotten from current style. Is it maybe needed for dynamic stuff?
 *
 * Revision 1.36  2004/12/16 17:22:25  joshy
 * minor code cleanup
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.35  2004/12/16 17:10:41  joshy
 * fixed box bug
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.34  2004/12/14 02:28:48  joshy
 * removed some comments
 * some bugs with the backgrounds still
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.33  2004/12/14 01:56:23  joshy
 * fixed layout width bugs
 * fixed extra border on document bug
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.32  2004/12/13 15:15:57  joshy
 * fixed bug where inlines would pick up parent styles when they aren't supposed to
 * fixed extra Xx's in printed text
 * added conf boolean to turn on box outlines
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.31  2004/12/12 03:32:58  tobega
 * Renamed x and u to avoid confusing IDE. But that got cvs in a twist. See if this does it
 *
 * Revision 1.30  2004/12/11 23:36:48  tobega
 * Progressing on cleaning up layout and boxes. Still broken, won't even compile at the moment. Working hard to fix it, though.
 *
 * Revision 1.29  2004/12/11 18:18:10  tobega
 * Still broken, won't even compile at the moment. Working hard to fix it, though. Replace the StyleReference interface with our only concrete implementation, it was a bother changing in two places all the time.
 *
 * Revision 1.28  2004/12/10 06:51:02  tobega
 * Shamefully, I must now check in painfully broken code. Good news is that Layout is much nicer, and we also handle :before and :after, and do :first-line better than before. Table stuff must be brought into line, but most needed is to fix Render. IMO Render should work with Boxes and Content. If Render goes for a node, that is wrong.
 *
 * Revision 1.27  2004/12/05 05:22:35  joshy
 * fixed NPEs in selection listener
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.26  2004/12/02 15:50:58  joshy
 * added debugging
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.25  2004/12/01 14:02:52  joshy
 * modified media to use the value from the rendering context
 * added the inline-block box
 * - j
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.24  2004/11/30 20:28:27  joshy
 * support for multiple floats on a single line.
 *
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.23  2004/11/28 23:29:02  tobega
 * Now handles media on Stylesheets, still need to handle at-media-rules. The media-type should be set in Context.media (set by default to "screen") before calling setContext on StyleReference.
 *
 * Revision 1.22  2004/11/18 14:12:44  joshy
 * added whitespace test
 * cleaned up some code, spacing, and comments
 *
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.21  2004/11/18 02:58:06  joshy
 * collapsed the font resolver and font resolver test into one class, and removed
 * the other
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.20  2004/11/17 14:58:18  joshy
 * added actions for font resizing
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.19  2004/11/16 07:25:12  tobega
 * Renamed HTMLPanel to BasicPanel
 *
 * Revision 1.18  2004/11/14 21:33:47  joshy
 * new font rendering interface support
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.17  2004/11/14 16:40:58  joshy
 * refactored layout factory
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.16  2004/11/14 06:26:39  joshy
 * added better detection for width problems. should avoid most
 * crashes
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.15  2004/11/12 22:02:00  joshy
 * initial support for mouse copy selection
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.14  2004/11/12 17:05:24  joshy
 * support for fixed positioning
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.13  2004/11/12 02:54:38  joshy
 * removed more dead code
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.11  2004/11/12 02:47:33  joshy
 * moved baseurl to rendering context
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.9  2004/11/10 17:28:54  joshy
 * initial support for anti-aliased text w/ minium
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.8  2004/11/09 00:36:08  joshy
 * fixed more text alignment
 * added menu item to show font metrics
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.7  2004/11/08 16:56:51  joshy
 * added first-line pseudo-class support
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.6  2004/11/03 23:54:33  joshy
 * added hamlet and tables to the browser
 * more support for absolute layout
 * added absolute layout unit tests
 * removed more dead code and moved code into layout factory
 *
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.5  2004/11/03 15:17:04  joshy
 * added intial support for absolute positioning
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.4  2004/11/02 20:44:55  joshy
 * put in some prep work for float support
 * removed some dead debugging code
 * moved isBlock code to LayoutFactory
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.3  2004/10/23 13:46:46  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc).
 * Added CVS log comments at bottom.
 *
 *
 */

