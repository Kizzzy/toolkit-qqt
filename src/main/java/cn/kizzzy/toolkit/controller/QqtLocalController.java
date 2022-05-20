package cn.kizzzy.toolkit.controller;

import cn.kizzzy.javafx.viewer.ViewerExecutor;
import cn.kizzzy.javafx.viewer.executor.QqtViewerExecutor;

@MenuParameter(path = "文件浏览/QQ堂/解包器(本地)")
@PluginParameter(url = "/fxml/explorer_view.fxml", title = "文件浏览(QQT)")
public class QqtLocalController extends ExplorerView {
    
    @Override
    public String getName() {
        return "QQT Display";
    }
    
    @Override
    protected ViewerExecutor initialViewExecutor() {
        return new QqtViewerExecutor();
    }
}