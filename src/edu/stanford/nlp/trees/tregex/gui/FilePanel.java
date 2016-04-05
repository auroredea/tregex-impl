// Tregex/Tsurgeon, FilePanel - a GUI for tree search and modification
// Copyright (c) 2007-2008 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// This code is a GUI interface to Tregex and Tsurgeon (which were
// written by Roger Levy and Galen Andrew).
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//    Support/Questions: parser-user@lists.stanford.edu
//    Licensing: parser-support@lists.stanford.edu
//    http://www-nlp.stanford.edu/software/tregex.shtml

package edu.stanford.nlp.trees.tregex.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;


import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreeReaderFactory;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

/**
 * Class representing the hierarchy of files in which trees may be searched and
 * allowing users to select whether to search a particular file or not
 *
 * @author Anna Rafferty
 */
public class FilePanel extends JPanel {

  private static final long serialVersionUID = -2229250395240163264L;
  private static FilePanel filePanel = null;
  private JTree tree;
  private FileTreeModel treeModel;
  private File tempParseFile;

  public static synchronized FilePanel getInstance() {
    if (filePanel == null) {
      filePanel = new FilePanel();
    }
    return filePanel;
  }

  private FilePanel() {
    //Temporary File
    try {
      tempParseFile = File.createTempFile("tregex", "t");
      tempParseFile.deleteOnExit();
    } catch (IOException e) { e.printStackTrace(); }

    //data stuff
    FileTreeNode root = new FileTreeNode();
    treeModel = new FileTreeModel(root);
    tree = new JTree(treeModel);
    tree.setCellRenderer(new FileTreeCellRenderer());
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        int nActiveTreebanks = getActiveTreebanks().size();

        //Tdiff
        boolean canActivate = (! TregexGUI.getInstance().isTdiffEnabled() || nActiveTreebanks < TregexGUI.MAX_TDIFF_TREEBANKS);
        if(path != null) {
          FileTreeNode node = (FileTreeNode) path.getLastPathComponent();
          if(canActivate || node.isActive())
            node.setActive(!node.isActive());
        }
      }
    });

    JTextField sentence = new JTextField(150);
    JButton sentenceAdd = new JButton("Add");

    sentenceAdd.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(sentence.getText().length() >= 2) {
          try {
            String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
            LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
            Tree parse = lp.parse(sentence.getText());
            TreePrint tp = new TreePrint("oneline");
            PrintWriter p = new PrintWriter(tempParseFile);
            tp.printTree(parse, p);
            p.close();
          }
          catch (IOException ex) { ex.printStackTrace(); }

          File[] sentenceList = new File[]{tempParseFile};
          loadFiles(new EnumMap<>(TregexGUI.FilterType.class), sentenceList);
          sentence.setText("");
        }
      }
    });

    //layout/panel stuff
    GridBagConstraints gbc = new GridBagConstraints();
    //card.setBorder(BorderFactory.createTitledBorder("Text auto-parsing :"));

    this.setLayout(new GridBagLayout());
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    this.add(sentence, gbc);
    sentence.setMinimumSize(new Dimension(260,24));

    gbc.gridx = GridBagConstraints.RELATIVE;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    this.add(sentenceAdd, gbc);

    gbc.gridy = 1;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    JScrollPane scroller = new JScrollPane(tree);
    scroller.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Tree files: "));
    scroller.setMinimumSize(new Dimension(300, 330));
    this.add(scroller);



    /*JScrollPane scroller = new JScrollPane(tree);
    this.add(scroller, BorderLayout.CENTER);*/
   /* this.setLayout(new BorderLayout());
    this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Tree files: "));
    JScrollPane scroller = new JScrollPane(tree);
    this.add(sentence, BorderLayout.LINE_START);
    this.add(sentenceAdd, BorderLayout.CENTER);
    this.add(scroller, BorderLayout.PAGE_END);*/

  }


  /**
   * Sets a new tree reader factory for reading trees from files in this panel.  Since this may make some files
   * with trees unable to be read, clearFiles indicates if all current files should be removed from the panel.
   */
  public void setTreeReaderFactory(TreeReaderFactory trf) {
    treeModel.setTRF(trf);
  }

  public void loadFiles(EnumMap<TregexGUI.FilterType, String> filters, File[] files) {
    treeModel.addFileFolder(filters, files);
  }

  /**
   *Returns true if no files are loaded; false otherwise
   */
  public boolean isEmpty() {
    return treeModel.isEmpty();
  }

  /**
   * Removes all files from the panel
   */
  public void clearAll() {
    TreeReaderFactory oldTrf = treeModel.getTRF();//Preserve the current TRF when we refresh the tree file list
    FileTreeNode root = new FileTreeNode();
    treeModel = new FileTreeModel(root);
    setTreeReaderFactory(oldTrf);
    tree.setModel(treeModel);
    this.revalidate();
    this.repaint();
  }

  /**
   * Returns all treebanks corresponding to the files stored in the panel that
   * are selected
   * @return active treebanks
   */
  public List<FileTreeNode> getActiveTreebanks() {
    List<FileTreeNode> active = new ArrayList<>();
    setActiveTreebanksFromParent(active, treeModel.getRoot());
    return active;
  }

  private void setActiveTreebanksFromParent(List<FileTreeNode> active, FileTreeNode parent) {
    int numChildren = treeModel.getChildCount(parent);
    for(int i = 0; i < numChildren; i++) {
      FileTreeNode child = treeModel.getChild(parent, i);
      if(!child.getAllowsChildren()) {
        if(child.isActive())
          active.add(child);
      } else {
        setActiveTreebanksFromParent(active,child);
      }
    }
  }


  @SuppressWarnings("serial")
  private static class FileTreeCellRenderer extends JCheckBox implements TreeCellRenderer {
    public FileTreeCellRenderer() {
      setOpaque(true);
    }

    public Component getTreeCellRendererComponent(JTree t, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

      return ((FileTreeNode) value).getDisplay();
    }
  }
}