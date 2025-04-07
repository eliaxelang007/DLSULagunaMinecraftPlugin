package zoy.dLSULaguna.utils;

import java.io.File;
import java.io.IOException;

public class PlayerStatsUtils {
    public static File getOrCreateFile(File parent, String filename) throws IOException{
        if (!parent.exists()) {
            parent.mkdirs();
        }
        File file = new File(parent, filename);
        if(!file.exists()){
            file.createNewFile();
        }
        return file;
    }
}
