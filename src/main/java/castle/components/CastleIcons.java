package castle.components;

import arc.struct.StringMap;
import arc.util.Http;
import mindustry.ctype.MappableContent;

public class CastleIcons {

    private static final StringMap icons = new StringMap();

    public static void load() {
        Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/master/core/assets/icons/icons.properties", response -> {
            for (var line : response.getResultAsString().split("\n")) {
                var values = line.split("\\|")[0].split("=");
                icons.put(values[1], String.valueOf((char) Integer.parseInt(values[0])));
            }
        });
    }

    public static String get(MappableContent content) {
        return icons.get(content.name, content.name);
    }
}