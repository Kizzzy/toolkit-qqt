package cn.kizzzy.display;

import cn.kizzzy.clazz.ClassFilter;
import cn.kizzzy.clazz.ClassFinderHelper;
import cn.kizzzy.helper.FileHelper;
import cn.kizzzy.helper.LogHelper;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class DisplayHelper {
    private static final Map<String, Class<? extends Display>> displayKvs = new HashMap<>();
    
    public static boolean load() {
        try {
            List<Class<?>> list = ClassFinderHelper.find(new ClassFilter() {
                @Override
                public String packageRoot() {
                    return "cn.kizzzy.display";
                }
                
                @Override
                public boolean isRecursive() {
                    return false;
                }
                
                @Override
                public boolean accept(Class<?> clazz) {
                    return clazz.isAnnotationPresent(DisplayFlag.class);
                }
            });
            
            for (Class<?> clazz : list) {
                DisplayFlag flag = clazz.getAnnotation(DisplayFlag.class);
                for (String suffix : flag.suffix()) {
                    displayKvs.put(suffix, (Class<? extends Display>) clazz);
                }
            }
            
            return true;
        } catch (Exception e) {
            LogHelper.error(null, e);
            return false;
        }
    }
    
    public static Display newDisplay(DisplayContext context, String path) {
        String ext = FileHelper.getExtension(path);
        Class<? extends Display> clazz = displayKvs.get(ext);
        if (clazz != null) {
            try {
                return clazz.getDeclaredConstructor(DisplayContext.class, String.class)
                    .newInstance(context, path);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return new Display();
    }
}
