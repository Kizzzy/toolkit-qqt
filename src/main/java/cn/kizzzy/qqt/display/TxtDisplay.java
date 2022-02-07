package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.DisplayType;

@DisplayFlag(suffix = {
    "txt",
    "ini",
    "xml",
    "lua",
    "eff",
})
public class TxtDisplay extends Display {
    
    public TxtDisplay(DisplayContext context, String path) {
        super(context, path);
    }
    
    @Override
    public void init() {
        context.notifyListener(DisplayType.SHOW_TEXT, context.load(path, String.class));
    }
}
