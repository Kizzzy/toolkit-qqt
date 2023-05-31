package cn.kizzzy.toolkit.controller;

import cn.kizzzy.javafx.menu.MenuParameter;
import cn.kizzzy.javafx.plugin.PluginParameter;
import cn.kizzzy.javafx.viewer.ViewerExecutor;
import cn.kizzzy.javafx.viewer.executor.QqtViewerExecutor;

@MenuParameter(path = "文件浏览/QQ堂/解包器(本地)")
@PluginParameter(title = "文件浏览(QQT)")
public class QqtLocalController extends ExplorerView {
    
    @Override
    protected ViewerExecutor initialViewExecutor() {
        return new QqtViewerExecutor();
    }
}