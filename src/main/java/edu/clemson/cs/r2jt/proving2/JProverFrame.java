package edu.clemson.cs.r2jt.proving2;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.LayoutManager;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.clemson.cs.r2jt.analysis.MathExpTypeResolver;
import edu.clemson.cs.r2jt.init.CompileEnvironment;
import edu.clemson.cs.r2jt.proving.absyn.NodeIdentifier;
import edu.clemson.cs.r2jt.proving.absyn.PExp;
import edu.clemson.cs.r2jt.proving.absyn.PSymbol;
import edu.clemson.cs.r2jt.type.BooleanType;
import edu.clemson.cs.r2jt.utilities.FlagDependencies;
import edu.clemson.cs.r2jt.utilities.FlagDependencyException;

public class JProverFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String CONTROLS_HIDDEN = "hidden";
    private static final String CONTROLS_VISIBLE = "visible";

    private final JProverFrame PARENT_THIS = this;

    private final JVCDisplay myVCDisplay = new JVCDisplay();

    private final JCheckBox myDetailsCheckBox = new JCheckBox("Details");
    private final JComponent myDetailsArea = buildDetailsArea();

    private final CardLayout myOptionalTransportLayout = new CardLayout();
    private final JPanel myOptionalTransportPanel =
            new JPanel(myOptionalTransportLayout);

    private final JComponent myBasicArea = buildBasicPanel();

    public static void main(String[] args) throws FlagDependencyException {
        JProverFrame p = new JProverFrame();
        p.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        p.setVisible(true);

        FlagDependencies.seal();
        MathExpTypeResolver metr =
                new MathExpTypeResolver(null, null,
                        new CompileEnvironment(args));

        List<PExp> conjuncts = new LinkedList<PExp>();

        List<PExp> expArgs = new LinkedList<PExp>();
        expArgs.add(new PSymbol(BooleanType.INSTANCE, "x", metr));
        expArgs.add(new PSymbol(BooleanType.INSTANCE, "y", metr));

        conjuncts.add(new PSymbol(BooleanType.INSTANCE, "=", expArgs,
                PSymbol.DisplayType.INFIX, metr));

        expArgs.clear();
        expArgs.add(new PSymbol(BooleanType.INSTANCE, "z", metr));

        conjuncts.add(new PSymbol(BooleanType.INSTANCE, "not", expArgs,
                PSymbol.DisplayType.PREFIX, metr));

        Antecedent a = new Antecedent(conjuncts);

        conjuncts.clear();
        expArgs.clear();
        expArgs.add(new PSymbol(BooleanType.INSTANCE, "b", metr));
        expArgs.add(new PSymbol(BooleanType.INSTANCE, "c", metr));

        PExp first =
                new PSymbol(BooleanType.INSTANCE, "+", expArgs,
                        PSymbol.DisplayType.INFIX, metr);

        expArgs.clear();
        expArgs.add(new PSymbol(BooleanType.INSTANCE, "a", metr));
        expArgs.add(first);

        PExp cc =
                new PSymbol(BooleanType.INSTANCE, "*", expArgs,
                        PSymbol.DisplayType.INFIX, metr);
        conjuncts.add(cc);

        Consequent c = new Consequent(conjuncts);

        VC vc = new VC("0_1", a, c);
        p.setVC(vc);

        int[] path = { 1 };
        NodeIdentifier nid = new NodeIdentifier(cc, path);

        p.highlightPExp(nid, new Color(200, 200, 200));
    }

    public JProverFrame() {
        setLayout(new BorderLayout());

        JPanel topLevel = new JPanel();
        LayoutManager topLevelLayout = new BorderLayout();
        topLevel.setLayout(topLevelLayout);

        topLevel.add(myBasicArea, BorderLayout.NORTH);
        topLevel.add(myDetailsArea, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(topLevel);
        add(scroll, BorderLayout.CENTER);

        myDetailsArea.setVisible(false);
        myDetailsCheckBox.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent arg0) {
                boolean checked = myDetailsCheckBox.isSelected();

                if (checked != myDetailsArea.isVisible()) {
                    myDetailsArea.setVisible(checked);

                    String state;
                    if (checked) {
                        state = CONTROLS_VISIBLE;
                    }
                    else {
                        myDetailsArea.setPreferredSize(myDetailsArea.getSize());
                        state = CONTROLS_HIDDEN;
                    }
                    myOptionalTransportLayout.show(myOptionalTransportPanel,
                            state);

                    myBasicArea.setPreferredSize(myBasicArea.getSize());
                    PARENT_THIS.pack();
                    myBasicArea.setPreferredSize(null);

                    if (checked) {
                        myDetailsArea.setPreferredSize(null);
                    }
                }
            }
        });

        pack();
    }

    public void setVC(VC vc) {
        myVCDisplay.setVC(vc);
    }

    public void highlightPExp(NodeIdentifier id, Color c) {
        myVCDisplay.highlightPExp(id, c);
    }

    private JComponent buildDetailsArea() {
        JPanel panel = new JPanel(new BorderLayout());

        JSplitPane split =
                new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                        buildProofStatusArea(), buildTheoremListPanel());
        split.setResizeWeight(1);

        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private JComponent buildProofStatusArea() {
        return new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
                buildVCDisplayArea(), buildProofArea());
    }

    private JComponent buildVCDisplayArea() {
        return myVCDisplay;
    }

    private JComponent buildProofArea() {
        return new JScrollPane(new JList());
    }

    private JPanel buildTheoremListPanel() {
        JPanel theoremListPanel = new JPanel(new BorderLayout());

        JList theoremList = new JList();
        JScrollPane theoremView = new JScrollPane(theoremList);
        theoremView
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        theoremView
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        theoremListPanel.add(new JTextField(), BorderLayout.NORTH);
        theoremListPanel.add(theoremView, BorderLayout.CENTER);

        JMenuBar filterListBar = new JMenuBar();
        JMenu filterList = new JMenu("Filters");

        JMenuItem showAll = new JMenuItem("Clear All Filters");
        filterList.add(showAll);
        filterList.add(new JSeparator());

        filterList.add(new JCheckBoxMenuItem("Filter Unapplicable"));
        filterList.add(new JCheckBoxMenuItem("Filter Complexifying"));

        filterListBar.add(filterList);

        theoremListPanel.add(filterListBar, BorderLayout.SOUTH);

        return theoremListPanel;
    }

    private JPanel buildBasicPanel() {
        JPanel basicPanel = new JPanel(new BorderLayout());
        basicPanel.add(new JLabel("Proving VC 0_1..."), BorderLayout.NORTH);
        basicPanel.add(buildProgressPanel(), BorderLayout.CENTER);
        basicPanel.add(buildButtonPanel(), BorderLayout.SOUTH);

        return basicPanel;
    }

    private JPanel buildProgressPanel() {
        JPanel verticalProgressPanel = new JPanel();
        BoxLayout verticalProgressPanelLayout =
                new BoxLayout(verticalProgressPanel, BoxLayout.Y_AXIS);
        verticalProgressPanel.setLayout(verticalProgressPanelLayout);

        verticalProgressPanel.add(Box.createVerticalGlue());

        JPanel horizontalProgressPanel = new JPanel();
        BoxLayout horizontalProgressPanelLayout =
                new BoxLayout(horizontalProgressPanel, BoxLayout.X_AXIS);
        horizontalProgressPanel.setLayout(horizontalProgressPanelLayout);

        horizontalProgressPanel.add(Box.createHorizontalStrut(20));

        JProgressBar progress = new JProgressBar();
        progress = new JProgressBar(0, 100);
        progress.setValue(33);
        progress.setStringPainted(true);

        horizontalProgressPanel.add(progress);
        horizontalProgressPanel.add(Box.createHorizontalStrut(20));

        verticalProgressPanel.add(horizontalProgressPanel);
        verticalProgressPanel.add(Box.createVerticalGlue());

        return verticalProgressPanel;
    }

    private JPanel buildButtonPanel() {
        JPanel buttonPanel = new JPanel();
        BoxLayout buttonPanelLayout =
                new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        buttonPanel.setLayout(buttonPanelLayout);

        buttonPanel.add(myDetailsCheckBox);
        buttonPanel.add(Box.createHorizontalStrut(10));

        JPanel blankPanel = new JPanel();
        myOptionalTransportPanel.add(blankPanel, CONTROLS_HIDDEN);
        myOptionalTransportPanel.add(buildTransportControlPanel(),
                CONTROLS_VISIBLE);

        //buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(myOptionalTransportPanel);
        //buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(new JButton("Cancel"));
        buttonPanel.add(new JButton("Skip >>>"));

        return buttonPanel;
    }

    private JPanel buildTransportControlPanel() {
        JPanel transportControlPanel = new JPanel();
        BoxLayout transportControlPanelLayout =
                new BoxLayout(transportControlPanel, BoxLayout.X_AXIS);
        transportControlPanel.setLayout(transportControlPanelLayout);
        transportControlPanel.add(new JButton(">"));
        transportControlPanel.add(new JButton("||"));
        transportControlPanel.add(new JButton("@"));
        transportControlPanel.add(Box.createHorizontalStrut(4));
        transportControlPanel.add(new JButton("<VC"));
        transportControlPanel.add(new JButton("VC>"));

        return transportControlPanel;
    }
}
