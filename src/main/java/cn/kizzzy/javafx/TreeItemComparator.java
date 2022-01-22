package cn.kizzzy.javafx;

import cn.kizzzy.vfs.tree.Node;
import javafx.scene.control.TreeItem;

import java.util.Comparator;

public class TreeItemComparator implements Comparator<TreeItem<Node>> {
    
    @Override
    public int compare(TreeItem<Node> o1, TreeItem<Node> o2) {
        Node folder1 = o1.getValue();
        Node folder2 = o2.getValue();
        if (folder1.leaf) {
            if (folder2.leaf) {
                return compareImpl(folder1.name, folder2.name);
            } else {
                return 1;
            }
        } else {
            if (folder2.leaf) {
                return -1;
            } else {
                return compareImpl(folder1.name, folder2.name);
            }
        }
    }
    
    private int compareImpl(String value1, String value2) {
        if (value1.matches("^[0-9]+$")) {
            if (value2.matches("^[0-9]+$")) {
                if (value1.length() != value2.length()) {
                    return value1.length() - value2.length();
                }
            }
        }
        return value1.compareTo(value2);
    }
}
