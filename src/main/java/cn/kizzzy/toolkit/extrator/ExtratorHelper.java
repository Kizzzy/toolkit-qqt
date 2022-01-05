package cn.kizzzy.toolkit.extrator;

import cn.kizzzy.qqt.QqtFile;
import cn.kizzzy.vfs.tree.Node;
import javafx.scene.control.TreeItem;

import java.util.Comparator;
import java.util.function.Function;

public class ExtratorHelper {
    
    public static void addItem(final TreeItem<Node<QqtFile>> root) {
        root.getValue().children.forEach((path, child) -> {
            TreeItem<Node<QqtFile>> item = new TreeItem<>(child);
            root.getChildren().add(item);
            addItem(item);
        });
    }
    
    public static void sortTreeItem(TreeItem<Node<QqtFile>> root, Comparator<TreeItem<Node<QqtFile>>> comparator) {
        if (!root.isLeaf()) {
            root.getChildren().sort(comparator);
            for (TreeItem<Node<QqtFile>> child : root.getChildren()) {
                sortTreeItem(child, comparator);
            }
        }
    }
    
    public static void listTreeItem(Node<QqtFile> root, Function<Node<QqtFile>, Boolean> callback) {
        if (!callback.apply(root)) {
            for (Node<QqtFile> child : root.children.values()) {
                listTreeItem(child, callback);
            }
        }
    }
}
